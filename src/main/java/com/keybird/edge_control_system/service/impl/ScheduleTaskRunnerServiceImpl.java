package com.keybird.edge_control_system.service.impl;

import com.keybird.edge_control_system.mapper.CheckpointRecordMapper;
import com.keybird.edge_control_system.mapper.EdgeNodeMapper;
import com.keybird.edge_control_system.mapper.ScheduleTaskMapper;
import com.keybird.edge_control_system.model.entity.CheckpointRecord;
import com.keybird.edge_control_system.model.entity.EdgeNode;
import com.keybird.edge_control_system.model.entity.ScheduleTask;
import com.keybird.edge_control_system.model.vo.agent.AgentCheckpointGenerateVO;
import com.keybird.edge_control_system.service.AgentClientService;
import com.keybird.edge_control_system.service.ScheduleTaskRunnerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
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

        int totalRequestCount = 0;
        int totalNewRequestCount = 0;
        int totalContinuousRequestCount = 0;
        double totalLatency = 0D;
        int latencyCount = 0;
        int checkpointNo = task.getCurrentCheckpoint() + 1;

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
                    int latency = Math.min(item.getUserToEdgeDelayMs(), item.getUserToCloudDelayMs());
                    totalLatency += latency;
                    latencyCount++;
                }
            }
        }

        CheckpointRecord record = new CheckpointRecord();
        record.setTaskId(task.getId());
        record.setCheckpointNo(checkpointNo);
        record.setTotalRequestCount(totalRequestCount);
        record.setNewRequestCount(totalNewRequestCount);
        record.setContinuousRequestCount(totalContinuousRequestCount);
        record.setEdgeRequestCount(totalRequestCount);
        record.setCloudRequestCount(0);
        record.setAvgLatency(latencyCount == 0 ? 0D : totalLatency / latencyCount);
        record.setColdStartCount(0);
        record.setReconfigurationCount(0);
        record.setUtilityValue((double) totalRequestCount);
        record.setDetailJson("first_version_checkpoint_record");
        checkpointRecordMapper.insert(record);

        task.setCurrentCheckpoint(checkpointNo);
        if (checkpointNo >= task.getCheckpointCount()) {
            task.setTaskStatus("FINISHED");
            task.setEndTime(LocalDateTime.now());
        }
        scheduleTaskMapper.updateById(task);

        log.info("单轮checkpoint执行完成，taskId={}, checkpointNo={}", task.getId(), checkpointNo);
    }

    private List<Long> parseNodeIds(String selectedNodeIds) {
        return List.of(selectedNodeIds.split(","))
                .stream()
                .filter(s -> s != null && !s.trim().isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }
}