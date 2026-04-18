package com.keybird.edge_control_system.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.keybird.edge_control_system.mapper.CheckpointRecordMapper;
import com.keybird.edge_control_system.mapper.ContainerTemplateMapper;
import com.keybird.edge_control_system.mapper.EdgeNodeMapper;
import com.keybird.edge_control_system.mapper.ScheduleTaskMapper;
import com.keybird.edge_control_system.model.dto.schedule.ScheduleTaskCreateRequest;
import com.keybird.edge_control_system.model.entity.CheckpointRecord;
import com.keybird.edge_control_system.model.entity.ContainerTemplate;
import com.keybird.edge_control_system.model.entity.EdgeNode;
import com.keybird.edge_control_system.model.entity.ScheduleTask;
import com.keybird.edge_control_system.model.vo.agent.AgentCheckpointGenerateVO;
import com.keybird.edge_control_system.model.vo.agent.AgentTaskPrepareRequest;
import com.keybird.edge_control_system.model.vo.schedule.ScheduleTaskVO;
import com.keybird.edge_control_system.service.AgentClientService;
import com.keybird.edge_control_system.service.ScheduleTaskRunnerService;
import com.keybird.edge_control_system.service.ScheduleTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ScheduleTaskServiceImpl implements ScheduleTaskService {

    @Resource
    private ScheduleTaskMapper scheduleTaskMapper;

    @Resource
    private EdgeNodeMapper edgeNodeMapper;

    @Resource
    private ContainerTemplateMapper containerTemplateMapper;

    @Resource
    private CheckpointRecordMapper checkpointRecordMapper;

    @Resource
    private AgentClientService agentClientService;

    @Resource
    private ScheduleTaskRunnerService scheduleTaskRunnerService;

    @Override
    public ScheduleTaskVO createTask(ScheduleTaskCreateRequest request) {
        ScheduleTask task = new ScheduleTask();
        task.setTaskCode("TASK_" + IdUtil.getSnowflakeNextIdStr());
        task.setTaskName(request.getTaskName());
        task.setTaskStatus("PENDING");
        task.setCheckpointCount(request.getCheckpointCount());
        task.setCurrentCheckpoint(0);
        task.setEnableSpecialCase(request.getEnableSpecialCase());
        task.setSelectedNodeIds(request.getSelectedNodeIds().stream().map(String::valueOf).collect(Collectors.joining(",")));
        task.setTaskConfigJson(JSONUtil.toJsonStr(request));

        scheduleTaskMapper.insert(task);

        ScheduleTaskVO vo = new ScheduleTaskVO();
        BeanUtils.copyProperties(task, vo);
        return vo;
    }

    @Override
    public boolean prepareTask(Long taskId) {
        ScheduleTask task = scheduleTaskMapper.selectById(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }

        List<Long> nodeIds = parseNodeIds(task.getSelectedNodeIds());
        List<EdgeNode> nodeList = edgeNodeMapper.selectBatchIds(nodeIds);
        if (nodeList.isEmpty()) {
            throw new RuntimeException("未找到参与节点");
        }

        List<ContainerTemplate> containerList = containerTemplateMapper.selectList(
                new QueryWrapper<ContainerTemplate>().eq("status", "ENABLED")
        );
        if (containerList.isEmpty()) {
            throw new RuntimeException("当前没有启用的容器");
        }

        AgentTaskPrepareRequest prepareRequest = new AgentTaskPrepareRequest();
        prepareRequest.setTaskId(task.getTaskCode());
        prepareRequest.setCheckpointCount(task.getCheckpointCount());
        prepareRequest.setContainerList(containerList.stream().map(item -> {
            AgentTaskPrepareRequest.ContainerMeta meta = new AgentTaskPrepareRequest.ContainerMeta();
            meta.setContainerCode(item.getTemplateCode());
            meta.setContainerName(item.getContainerName());
            meta.setRequiredCpu(item.getRequiredCpu());
            meta.setRequiredMemory(item.getRequiredMemory());
            meta.setRequiredStorage(item.getRequiredStorage());
            return meta;
        }).collect(Collectors.toList()));

        for (EdgeNode node : nodeList) {
            boolean connected = agentClientService.ping(node.getIpAddress(), node.getPort());
            if (!connected) {
                throw new RuntimeException("节点【" + node.getNodeName() + "】不可连接");
            }
            boolean prepared = agentClientService.prepareTask(node.getIpAddress(), node.getPort(), prepareRequest);
            if (!prepared) {
                throw new RuntimeException("节点【" + node.getNodeName() + "】任务准备失败");
            }
        }

        task.setTaskStatus("PENDING");
        scheduleTaskMapper.updateById(task);
        return true;
    }

    @Override
    public boolean startTask(Long taskId) {
        ScheduleTask task = scheduleTaskMapper.selectById(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }

        if (!Objects.equals(task.getTaskStatus(), "PREPARED") && !Objects.equals(task.getTaskStatus(), "PENDING")) {
            throw new RuntimeException("当前任务状态不允许启动");
        }

        log.info("开始启动任务，taskId={}, taskCode={}", taskId, task.getTaskCode());

        List<EdgeNode> nodeList = edgeNodeMapper.selectBatchIds(parseNodeIds(task.getSelectedNodeIds()));
        for (EdgeNode node : nodeList) {
            log.info("准备启动节点任务，nodeName={}, ip={}, port={}, taskCode={}",
                    node.getNodeName(), node.getIpAddress(), node.getPort(), task.getTaskCode());

            boolean started = agentClientService.startTask(node.getIpAddress(), node.getPort(), task.getTaskCode());

            log.info("节点启动结果，nodeName={}, started={}", node.getNodeName(), started);

            if (!started) {
                throw new RuntimeException("节点【" + node.getNodeName() + "】任务启动失败");
            }
        }

        task.setTaskStatus("RUNNING");
        task.setStartTime(LocalDateTime.now());
        scheduleTaskMapper.updateById(task);

        // 异步自动执行全部 checkpoint
        scheduleTaskRunnerService.runTaskAsync(taskId);

        log.info("任务启动成功并已进入异步执行，taskId={}, taskCode={}", taskId, task.getTaskCode());
        return true;
    }

//    @Override
//    public boolean runNextCheckpoint(Long taskId) {
//        ScheduleTask task = scheduleTaskMapper.selectById(taskId);
//        if (task == null) {
//            throw new RuntimeException("任务不存在");
//        }
//        if (!Objects.equals(task.getTaskStatus(), "RUNNING")) {
//            throw new RuntimeException("任务未运行");
//        }
//
//        List<EdgeNode> nodeList = edgeNodeMapper.selectBatchIds(parseNodeIds(task.getSelectedNodeIds()));
//
//        int totalRequestCount = 0;
//        int totalNewRequestCount = 0;
//        int totalContinuousRequestCount = 0;
//        double totalLatency = 0D;
//        int latencyCount = 0;
//        int checkpointNo = task.getCurrentCheckpoint() + 1;
//
//        for (EdgeNode node : nodeList) {
//            AgentCheckpointGenerateVO cpResult = agentClientService.generateNextCheckpoint(
//                    node.getIpAddress(),
//                    node.getPort(),
//                    task.getTaskCode()
//            );
//
//            totalRequestCount += cpResult.getRequestList() == null ? 0 : cpResult.getRequestList().size();
//            totalNewRequestCount += cpResult.getNewRequestCount() == null ? 0 : cpResult.getNewRequestCount();
//            totalContinuousRequestCount += cpResult.getContinuousRequestCount() == null ? 0 : cpResult.getContinuousRequestCount();
//
//            if (cpResult.getRequestList() != null) {
//                for (AgentCheckpointGenerateVO.RequestItemVO item : cpResult.getRequestList()) {
//                    int latency = Math.min(item.getUserToEdgeDelayMs(), item.getUserToCloudDelayMs());
//                    totalLatency += latency;
//                    latencyCount++;
//                }
//            }
//        }
//
//        CheckpointRecord record = new CheckpointRecord();
//        record.setTaskId(taskId);
//        record.setCheckpointNo(checkpointNo);
//        record.setTotalRequestCount(totalRequestCount);
//        record.setNewRequestCount(totalNewRequestCount);
//        record.setContinuousRequestCount(totalContinuousRequestCount);
//        record.setEdgeRequestCount(totalRequestCount);
//        record.setCloudRequestCount(0);
//        record.setAvgLatency(latencyCount == 0 ? 0D : totalLatency / latencyCount);
//        record.setColdStartCount(0);
//        record.setReconfigurationCount(0);
//        record.setUtilityValue((double) totalRequestCount);
//        record.setDetailJson("first_version_checkpoint_record");
//        checkpointRecordMapper.insert(record);
//
//        task.setCurrentCheckpoint(checkpointNo);
//        if (checkpointNo >= task.getCheckpointCount()) {
//            task.setTaskStatus("FINISHED");
//            task.setEndTime(LocalDateTime.now());
//        }
//        scheduleTaskMapper.updateById(task);
//
//        return true;
//    }

    @Override
    public ScheduleTaskVO getTaskDetail(Long taskId) {
        ScheduleTask task = scheduleTaskMapper.selectById(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }
        ScheduleTaskVO vo = new ScheduleTaskVO();
        BeanUtils.copyProperties(task, vo);
        return vo;
    }

    private List<Long> parseNodeIds(String selectedNodeIds) {
        return List.of(selectedNodeIds.split(","))
                .stream()
                .filter(s -> s != null && !s.trim().isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }
}