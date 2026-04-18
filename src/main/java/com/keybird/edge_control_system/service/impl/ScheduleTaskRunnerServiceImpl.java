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

        int totalRequestCount = 0;
        int totalNewRequestCount = 0;
        int totalContinuousRequestCount = 0;
        int edgeRequestCount = 0;
        int cloudRequestCount = 0;
        int coldStartCount = 0;
        int reconfigurationCount = 0;
        double totalLatency = 0D;
        int latencyCount = 0;
        int checkpointNo = task.getCurrentCheckpoint() + 1;

        Map<Long, Set<String>> currentNodeContainerMap = new HashMap<>();
        Map<Long, Integer> nodeRequestCountMap = new HashMap<>();

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

            Set<String> currentNodeContainers = new HashSet<>();
            int currentNodeRequestCount = 0;
            int cpuUsed = 0;
            int memoryUsed = 0;
            int storageUsed = 0;

            Set<String> previousContainers = previousNodeContainerMap.getOrDefault(node.getId(), new HashSet<>());

            if (cpResult.getRequestList() != null) {
                for (AgentCheckpointGenerateVO.RequestItemVO item : cpResult.getRequestList()) {
                    ContainerTemplate template = templateMap.get(item.getContainerCode());
                    if (template == null) {
                        cloudRequestCount++;
                        totalLatency += item.getUserToCloudDelayMs();
                        latencyCount++;
                        continue;
                    }

                    boolean feasibleOnNode = isTemplateFeasibleForNode(node, template);

                    if (feasibleOnNode) {
                        edgeRequestCount++;
                        currentNodeRequestCount++;
                        totalLatency += item.getUserToEdgeDelayMs();
                        latencyCount++;

                        currentNodeContainers.add(template.getTemplateCode());

                        if (!previousContainers.contains(template.getTemplateCode())) {
                            coldStartCount++;
                        }
                    } else {
                        cloudRequestCount++;
                        totalLatency += item.getUserToCloudDelayMs();
                        latencyCount++;
                    }
                }
            }

            // 资源占用按“当前节点活跃容器集合”估算
            for (String containerCode : currentNodeContainers) {
                ContainerTemplate template = templateMap.get(containerCode);
                if (template != null) {
                    cpuUsed += defaultInt(template.getRequiredCpu());
                    memoryUsed += defaultInt(template.getRequiredMemory());
                    storageUsed += defaultInt(template.getRequiredStorage());
                }
            }

            node.setCurrentRequestCount(currentNodeRequestCount);
            node.setCurrentInstanceCount(currentNodeContainers.size());
            node.setCpuUsed(cpuUsed);
            node.setMemoryUsed(memoryUsed);
            node.setStorageUsed(storageUsed);
            edgeNodeMapper.updateById(node);

            currentNodeContainerMap.put(node.getId(), currentNodeContainers);
            nodeRequestCountMap.put(node.getId(), currentNodeRequestCount);
        }

        // 简化版重配置次数：上一轮存在、本轮不存在，或者从某节点消失并在另一节点出现
        reconfigurationCount = calculateReconfigurationCount(previousNodeContainerMap, currentNodeContainerMap);

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
        record.setUtilityValue((double) edgeRequestCount - cloudRequestCount * 0.5);
        record.setDetailJson("checkpoint_" + checkpointNo);
        checkpointRecordMapper.insert(record);

        // 更新运行时状态
        previousNodeContainerMap.clear();
        previousNodeContainerMap.putAll(currentNodeContainerMap);

        task.setCurrentCheckpoint(checkpointNo);
        if (checkpointNo >= task.getCheckpointCount()) {
            task.setTaskStatus("FINISHED");
            task.setEndTime(LocalDateTime.now());
            scheduleTaskMapper.updateById(task);
            scheduleRuntimeStore.clearTaskState(task.getId());
        } else {
            scheduleTaskMapper.updateById(task);
        }

        log.info("单轮checkpoint执行完成，taskId={}, checkpointNo={}, totalRequestCount={}, edgeRequestCount={}, cloudRequestCount={}, coldStartCount={}, reconfigurationCount={}",
                task.getId(), checkpointNo, totalRequestCount, edgeRequestCount, cloudRequestCount, coldStartCount, reconfigurationCount);
    }

    private boolean isTemplateFeasibleForNode(EdgeNode node, ContainerTemplate template) {
        return defaultInt(template.getRequiredCpu()) <= defaultInt(node.getCpuTotal())
                && defaultInt(template.getRequiredMemory()) <= defaultInt(node.getMemoryTotal())
                && defaultInt(template.getRequiredStorage()) <= defaultInt(node.getStorageTotal());
    }

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

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private List<Long> parseNodeIds(String selectedNodeIds) {
        return List.of(selectedNodeIds.split(","))
                .stream()
                .filter(s -> s != null && !s.trim().isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }
}