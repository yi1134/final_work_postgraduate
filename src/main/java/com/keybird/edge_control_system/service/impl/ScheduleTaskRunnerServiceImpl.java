package com.keybird.edge_control_system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.keybird.edge_control_system.mapper.CheckpointRecordMapper;
import com.keybird.edge_control_system.mapper.ContainerTemplateMapper;
import com.keybird.edge_control_system.mapper.EdgeNodeMapper;
import com.keybird.edge_control_system.mapper.ScheduleTaskMapper;
import com.keybird.edge_control_system.model.entity.CheckpointRecord;
import com.keybird.edge_control_system.model.entity.ContainerTemplate;
import com.keybird.edge_control_system.model.entity.EdgeNode;
import com.keybird.edge_control_system.model.entity.ScheduleTask;
import com.keybird.edge_control_system.model.vo.agent.AgentCheckpointGenerateVO;
import com.keybird.edge_control_system.service.AgentClientService;
import com.keybird.edge_control_system.service.ScheduleTaskRunnerService;
import com.keybird.edge_control_system.store.ScheduleRuntimeStore;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ScheduleTaskRunnerServiceImpl implements ScheduleTaskRunnerService {

    @Resource
    private ScheduleTaskMapper scheduleTaskMapper;

    @Resource
    private EdgeNodeMapper edgeNodeMapper;

    @Resource
    private CheckpointRecordMapper checkpointRecordMapper;

    @Resource
    private AgentClientService agentClientService;

    @Resource
    private ContainerTemplateMapper containerTemplateMapper;

    @Resource
    private ScheduleRuntimeStore scheduleRuntimeStore;

    @Override
    public void runTaskAsync(Long taskId) {
        try {
            ScheduleTask task = scheduleTaskMapper.selectById(taskId);
            if (task == null) {
                log.error("异步执行任务失败，任务不存在，taskId={}", taskId);
                return;
            }

            log.info("开始异步执行调度任务，taskId={}, taskCode={}", taskId, task.getTaskCode());

            while (true) {
                ScheduleTask latestTask = scheduleTaskMapper.selectById(taskId);
                if (latestTask == null) {
                    log.error("任务执行中任务丢失，taskId={}", taskId);
                    return;
                }

                if (!Objects.equals(latestTask.getTaskStatus(), "RUNNING")) {
                    log.info("任务已非运行状态，停止异步执行，taskId={}, status={}",
                            taskId, latestTask.getTaskStatus());
                    return;
                }

                if (latestTask.getCurrentCheckpoint() >= latestTask.getCheckpointCount()) {
                    latestTask.setTaskStatus("FINISHED");
                    latestTask.setEndTime(LocalDateTime.now());
                    scheduleTaskMapper.updateById(latestTask);

                    log.info("任务执行完成，taskId={}, totalCheckpoint={}",
                            taskId, latestTask.getCheckpointCount());
                    return;
                }

                executeOneCheckpoint(latestTask);

                Thread.sleep(1000);
            }
        } catch (Exception e) {
            log.error("异步执行调度任务异常，taskId={}", taskId, e);

            ScheduleTask failedTask = scheduleTaskMapper.selectById(taskId);
            if (failedTask != null) {
                failedTask.setTaskStatus("FAILED");
                failedTask.setEndTime(LocalDateTime.now());
                scheduleTaskMapper.updateById(failedTask);
            }
        }
    }

    private void executeOneCheckpoint(ScheduleTask task) {
        List<EdgeNode> nodeList = edgeNodeMapper.selectBatchIds(parseNodeIds(task.getSelectedNodeIds()));

        List<ContainerTemplate> enabledTemplateList = containerTemplateMapper.selectList(
                new QueryWrapper<ContainerTemplate>().eq("status", "ENABLED")
        );

        Map<String, ContainerTemplate> templateMap = enabledTemplateList.stream()
                .collect(Collectors.toMap(ContainerTemplate::getTemplateCode, item -> item, (a, b) -> a));

        ScheduleRuntimeStore.TaskRuntimeState runtimeState = scheduleRuntimeStore.getOrCreateTaskState(task.getId());
        Map<Long, Set<String>> previousNodeContainerMap = runtimeState.getNodeContainerMap();
        Map<String, String> previousUserRequestContainerMap = runtimeState.getUserLastRequestContainerMap();

        int checkpointNo = task.getCurrentCheckpoint() + 1;

        // ===== 1. 先拉取所有节点本轮请求 =====
        List<NodeRequestWrapper> allRequests = new ArrayList<>();

        int totalRequestCount = 0;
        int totalNewRequestCount = 0;
        int totalContinuousRequestCount = 0;

        for (EdgeNode node : nodeList) {
            log.info("开始拉取节点checkpoint，nodeName={}, ip={}, port={}, taskCode={}",
                    node.getNodeName(), node.getIpAddress(), node.getPort(), task.getTaskCode());

            AgentCheckpointGenerateVO cpResult = agentClientService.generateNextCheckpoint(
                    node.getIpAddress(),
                    node.getPort(),
                    task.getTaskCode()
            );

            int requestCount = cpResult.getRequestList() == null ? 0 : cpResult.getRequestList().size();
            log.info("节点checkpoint返回成功，nodeName={}, checkpointNo={}, requestCount={}",
                    node.getNodeName(), cpResult.getCheckpointNo(), requestCount);

            totalRequestCount += requestCount;
            totalNewRequestCount += cpResult.getNewRequestCount() == null ? 0 : cpResult.getNewRequestCount();
            totalContinuousRequestCount += cpResult.getContinuousRequestCount() == null ? 0 : cpResult.getContinuousRequestCount();

            if (cpResult.getRequestList() != null) {
                for (AgentCheckpointGenerateVO.RequestItemVO item : cpResult.getRequestList()) {
                    NodeRequestWrapper wrapper = new NodeRequestWrapper();
                    wrapper.setSourceNodeId(node.getId());
                    wrapper.setSourceNodeName(node.getNodeName());
                    wrapper.setRequestItem(item);
                    allRequests.add(wrapper);
                }
            }
        }

        // ===== 2. 统计每个容器在各节点的请求热度 =====
        Map<String, Map<Long, Integer>> containerNodeDemandMap = new HashMap<>();
        for (NodeRequestWrapper wrapper : allRequests) {
            String containerCode = wrapper.getRequestItem().getContainerCode();
            Long nodeId = wrapper.getSourceNodeId();

            containerNodeDemandMap
                    .computeIfAbsent(containerCode, k -> new HashMap<>())
                    .merge(nodeId, 1, Integer::sum);
        }

        // ===== 3. 按简易算法3做容器驻留决策 =====
        // newContainerPlacement: 容器 -> 目标驻留节点
        Map<String, Long> newContainerPlacement = new HashMap<>();

        int coldStartCount = 0;
        int reconfigurationCount = 0;

        // beta 的简化门槛：新热点节点比旧节点至少多 1 个请求才迁移
        final int betaThreshold = 1;

        for (Map.Entry<String, Map<Long, Integer>> entry : containerNodeDemandMap.entrySet()) {
            String containerCode = entry.getKey();
            Map<Long, Integer> demandMap = entry.getValue();

            ContainerTemplate template = templateMap.get(containerCode);
            if (template == null) {
                continue;
            }

            // 找本轮需求最高节点
            Long bestNodeId = null;
            int bestDemand = -1;
            for (Map.Entry<Long, Integer> demandEntry : demandMap.entrySet()) {
                if (demandEntry.getValue() > bestDemand) {
                    bestDemand = demandEntry.getValue();
                    bestNodeId = demandEntry.getKey();
                }
            }

            // 找上一轮该容器驻留在哪个节点
            Long oldNodeId = findContainerOwner(previousNodeContainerMap, containerCode);

            if (oldNodeId == null) {
                // 上一轮没有驻留，尝试在最热点节点新建
                EdgeNode bestNode = findNodeById(nodeList, bestNodeId);
                if (bestNode != null && isTemplateFeasibleForNode(bestNode, template)) {
                    newContainerPlacement.put(containerCode, bestNodeId);
                    coldStartCount++;
                }
                continue;
            }

            // 上一轮有驻留，判断是否迁移
            if (Objects.equals(oldNodeId, bestNodeId)) {
                newContainerPlacement.put(containerCode, oldNodeId);
                continue;
            }

            int oldDemand = demandMap.getOrDefault(oldNodeId, 0);
            EdgeNode bestNode = findNodeById(nodeList, bestNodeId);

            if (bestNode != null
                    && isTemplateFeasibleForNode(bestNode, template)
                    && (bestDemand - oldDemand) >= betaThreshold) {
                // 发生迁移（简易版重配置）
                newContainerPlacement.put(containerCode, bestNodeId);
                reconfigurationCount++;
            } else {
                // 继续保留原节点
                newContainerPlacement.put(containerCode, oldNodeId);
            }
        }

        // ===== 4. 按新驻留结果分配请求 =====
        int edgeRequestCount = 0;
        int cloudRequestCount = 0;
        int reuseCount = 0;
        double totalLatency = 0D;
        int latencyCount = 0;

        Map<Long, Set<String>> currentNodeContainerMap = new HashMap<>();
        Map<Long, Integer> nodeRequestCountMap = new HashMap<>();
        Map<String, String> currentUserRequestContainerMap = new HashMap<>();

        for (NodeRequestWrapper wrapper : allRequests) {
            AgentCheckpointGenerateVO.RequestItemVO item = wrapper.getRequestItem();
            String containerCode = item.getContainerCode();

            Long targetNodeId = newContainerPlacement.get(containerCode);
            ContainerTemplate template = templateMap.get(containerCode);

            // 记录用户这一轮请求容器，用于下一轮 ΔU_p
            currentUserRequestContainerMap.put(item.getUserId(), containerCode);

            if (targetNodeId == null || template == null) {
                cloudRequestCount++;
                totalLatency += item.getUserToCloudDelayMs();
                latencyCount++;
                continue;
            }

            EdgeNode targetNode = findNodeById(nodeList, targetNodeId);
            if (targetNode == null || !isTemplateFeasibleForNode(targetNode, template)) {
                cloudRequestCount++;
                totalLatency += item.getUserToCloudDelayMs();
                latencyCount++;
                continue;
            }

            // 边缘处理
            edgeRequestCount++;
            nodeRequestCountMap.merge(targetNodeId, 1, Integer::sum);
            currentNodeContainerMap.computeIfAbsent(targetNodeId, k -> new HashSet<>()).add(containerCode);

            // 复用统计：持续活跃且请求容器与上一轮相同，视为复用
            String previousContainer = previousUserRequestContainerMap.get(item.getUserId());
            if (Boolean.TRUE.equals(item.getContinuousActive()) && Objects.equals(previousContainer, containerCode)) {
                reuseCount++;
            }

            totalLatency += item.getUserToEdgeDelayMs();
            latencyCount++;
        }

        // ===== 5. 更新节点资源占用 =====
        for (EdgeNode node : nodeList) {
            Set<String> nodeContainers = currentNodeContainerMap.getOrDefault(node.getId(), new HashSet<>());
            int cpuUsed = 0;
            int memoryUsed = 0;
            int storageUsed = 0;

            for (String containerCode : nodeContainers) {
                ContainerTemplate template = templateMap.get(containerCode);
                if (template != null) {
                    cpuUsed += defaultInt(template.getRequiredCpu());
                    memoryUsed += defaultInt(template.getRequiredMemory());
                    storageUsed += defaultInt(template.getRequiredStorage());
                }
            }

            node.setCurrentRequestCount(nodeRequestCountMap.getOrDefault(node.getId(), 0));
            node.setCurrentInstanceCount(nodeContainers.size());
            node.setCpuUsed(cpuUsed);
            node.setMemoryUsed(memoryUsed);
            node.setStorageUsed(storageUsed);
            edgeNodeMapper.updateById(node);
        }

        // ===== 6. 记录 checkpoint 结果 =====
        CheckpointRecord record = new CheckpointRecord();
        record.setTaskId(task.getId());
        record.setCheckpointNo(checkpointNo);
        record.setTotalRequestCount(totalRequestCount);
        record.setNewRequestCount(totalNewRequestCount);
        record.setContinuousRequestCount(totalContinuousRequestCount);
        record.setEdgeRequestCount(edgeRequestCount);
        record.setCloudRequestCount(cloudRequestCount);
        record.setAvgLatency(latencyCount == 0 ? 0D : totalLatency / latencyCount);
        record.setColdStartCount(coldStartCount);
        record.setReconfigurationCount(reconfigurationCount);
        record.setUtilityValue((double) edgeRequestCount + reuseCount * 0.5 - cloudRequestCount * 0.5);
        record.setDetailJson("checkpoint_" + checkpointNo);
        checkpointRecordMapper.insert(record);

        // ===== 7. 更新运行时状态 =====
        previousNodeContainerMap.clear();
        previousNodeContainerMap.putAll(currentNodeContainerMap);

        previousUserRequestContainerMap.clear();
        previousUserRequestContainerMap.putAll(currentUserRequestContainerMap);

        task.setCurrentCheckpoint(checkpointNo);
        if (checkpointNo >= task.getCheckpointCount()) {
            task.setTaskStatus("FINISHED");
            task.setEndTime(LocalDateTime.now());
            scheduleTaskMapper.updateById(task);
            scheduleRuntimeStore.clearTaskState(task.getId());
        } else {
            scheduleTaskMapper.updateById(task);
        }

        log.info("简易算法3执行完成，taskId={}, checkpointNo={}, totalRequestCount={}, edgeRequestCount={}, cloudRequestCount={}, coldStartCount={}, reconfigurationCount={}, reuseCount={}",
                task.getId(), checkpointNo, totalRequestCount, edgeRequestCount, cloudRequestCount, coldStartCount, reconfigurationCount, reuseCount);
    }

    private Long findContainerOwner(Map<Long, Set<String>> nodeContainerMap, String containerCode) {
        for (Map.Entry<Long, Set<String>> entry : nodeContainerMap.entrySet()) {
            if (entry.getValue() != null && entry.getValue().contains(containerCode)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private EdgeNode findNodeById(List<EdgeNode> nodeList, Long nodeId) {
        if (nodeId == null) {
            return null;
        }
        for (EdgeNode node : nodeList) {
            if (Objects.equals(node.getId(), nodeId)) {
                return node;
            }
        }
        return null;
    }

    private boolean isTemplateFeasibleForNode(EdgeNode node, ContainerTemplate template) {
        return defaultInt(template.getRequiredCpu()) <= defaultInt(node.getCpuTotal())
                && defaultInt(template.getRequiredMemory()) <= defaultInt(node.getMemoryTotal())
                && defaultInt(template.getRequiredStorage()) <= defaultInt(node.getStorageTotal());
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    @Data
    private static class NodeRequestWrapper {
        private Long sourceNodeId;
        private String sourceNodeName;
        private AgentCheckpointGenerateVO.RequestItemVO requestItem;
    }

//    private boolean isTemplateFeasibleForNode(EdgeNode node, ContainerTemplate template) {
//        return defaultInt(template.getRequiredCpu()) <= defaultInt(node.getCpuTotal())
//                && defaultInt(template.getRequiredMemory()) <= defaultInt(node.getMemoryTotal())
//                && defaultInt(template.getRequiredStorage()) <= defaultInt(node.getStorageTotal());
//    }

    private int calculateReconfigurationCount(Map<Long, Set<String>> previousMap, Map<Long, Set<String>> currentMap) {
        Map<String, Long> previousOwner = new HashMap<>();
        for (Map.Entry<Long, Set<String>> entry : previousMap.entrySet()) {
            for (String containerCode : entry.getValue()) {
                previousOwner.put(containerCode, entry.getKey());
            }
        }

        Map<String, Long> currentOwner = new HashMap<>();
        for (Map.Entry<Long, Set<String>> entry : currentMap.entrySet()) {
            for (String containerCode : entry.getValue()) {
                currentOwner.put(containerCode, entry.getKey());
            }
        }

        int count = 0;
        for (Map.Entry<String, Long> entry : currentOwner.entrySet()) {
            String containerCode = entry.getKey();
            Long currentNodeId = entry.getValue();
            Long previousNodeId = previousOwner.get(containerCode);
            if (previousNodeId != null && !Objects.equals(previousNodeId, currentNodeId)) {
                count++;
            }
        }
        return count;
    }

//    private int defaultInt(Integer value) {
//        return value == null ? 0 : value;
//    }

    private List<Long> parseNodeIds(String selectedNodeIds) {
        return List.of(selectedNodeIds.split(","))
                .stream()
                .filter(s -> s != null && !s.trim().isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }
}