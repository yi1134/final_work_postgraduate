package com.keybird.edge_control_system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.keybird.edge_control_system.mapper.CheckpointRecordMapper;
import com.keybird.edge_control_system.mapper.EdgeNodeMapper;
import com.keybird.edge_control_system.mapper.ScheduleTaskMapper;
import com.keybird.edge_control_system.model.entity.CheckpointRecord;
import com.keybird.edge_control_system.model.entity.EdgeNode;
import com.keybird.edge_control_system.model.entity.ScheduleTask;
import com.keybird.edge_control_system.model.vo.*;
import com.keybird.edge_control_system.service.DashboardService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class DashboardServiceImpl implements DashboardService {

    @Resource
    private ScheduleTaskMapper scheduleTaskMapper;

    @Resource
    private CheckpointRecordMapper checkpointRecordMapper;

    @Resource
    private EdgeNodeMapper edgeNodeMapper;

    @Override
    public DashboardSummaryVO getDashboardSummary() {
        DashboardSummaryVO summaryVO = new DashboardSummaryVO();

        // 1. 当前任务：优先取正在运行的，否则取最近一条
        ScheduleTask currentTask = scheduleTaskMapper.selectOne(
                new QueryWrapper<ScheduleTask>()
                        .eq("task_status", "RUNNING")
                        .orderByDesc("id")
                        .last("limit 1")
        );

        if (currentTask == null) {
            currentTask = scheduleTaskMapper.selectOne(
                    new QueryWrapper<ScheduleTask>()
                            .orderByDesc("id")
                            .last("limit 1")
            );
        }

        Long currentTaskId = currentTask != null ? currentTask.getId() : null;

        // 2. 最近一条 checkpoint
        CheckpointRecord latestRecord = null;
        List<CheckpointRecord> latestRecords = new ArrayList<>();
        if (currentTaskId != null) {
            latestRecord = checkpointRecordMapper.selectOne(
                    new QueryWrapper<CheckpointRecord>()
                            .eq("task_id", currentTaskId)
                            .orderByDesc("checkpoint_no")
                            .last("limit 1")
            );

            latestRecords = checkpointRecordMapper.selectList(
                    new QueryWrapper<CheckpointRecord>()
                            .eq("task_id", currentTaskId)
                            .orderByDesc("checkpoint_no")
                            .last("limit 20")
            );

            Collections.reverse(latestRecords);
        }

        // 3. 节点数据
        List<EdgeNode> nodeList = edgeNodeMapper.selectList(
                new QueryWrapper<EdgeNode>()
                        .eq("is_deleted", 0)
                        .orderByAsc("id")
        );

        // 4. 顶部指标卡
        int nodeTotal = nodeList.size();
        int onlineNodeCount = (int) nodeList.stream()
                .filter(node -> Objects.equals(node.getStatus(), "ONLINE"))
                .count();

        int totalInstanceCount = nodeList.stream()
                .map(EdgeNode::getCurrentInstanceCount)
                .filter(v -> v != null)
                .reduce(0, Integer::sum);

        int currentRequestCount = latestRecord != null ? defaultInt(latestRecord.getTotalRequestCount()) : 0;
        int edgeRequestCount = latestRecord != null ? defaultInt(latestRecord.getEdgeRequestCount()) : 0;
        int cloudRequestCount = latestRecord != null ? defaultInt(latestRecord.getCloudRequestCount()) : 0;
        double avgLatency = latestRecord != null ? defaultDouble(latestRecord.getAvgLatency()) : 0D;
        double utilityValue = latestRecord != null ? defaultDouble(latestRecord.getUtilityValue()) : 0D;
        int coldStartCount = latestRecord != null ? defaultInt(latestRecord.getColdStartCount()) : 0;
        int reconfigurationCount = latestRecord != null ? defaultInt(latestRecord.getReconfigurationCount()) : 0;

        summaryVO.setSummaryCards(List.of(
                new DashboardCardVO("当前任务ID", currentTaskId == null ? "-" : currentTaskId),
                new DashboardCardVO("当前请求数", currentRequestCount),
                new DashboardCardVO("节点总数", nodeTotal),
                new DashboardCardVO("在线节点数", onlineNodeCount),
                new DashboardCardVO("容器实例总数", totalInstanceCount),
                new DashboardCardVO("边缘处理数", edgeRequestCount),
                new DashboardCardVO("云端回退数", cloudRequestCount),
                new DashboardCardVO("平均时延(ms)", String.format("%.2f", avgLatency))
        ));

        // 5. 当前 checkpoint 摘要
        CheckpointSummaryVO checkpointSummaryVO = new CheckpointSummaryVO();
        checkpointSummaryVO.setCurrentCheckpoint(currentTask != null ? defaultInt(currentTask.getCurrentCheckpoint()) : 0);
        checkpointSummaryVO.setNewRequestCount(latestRecord != null ? defaultInt(latestRecord.getNewRequestCount()) : 0);
        checkpointSummaryVO.setActiveRequestCount(latestRecord != null ? defaultInt(latestRecord.getContinuousRequestCount()) : 0);
        checkpointSummaryVO.setNewInstanceCount(coldStartCount);
        checkpointSummaryVO.setReuseInstanceCount(Math.max(0, edgeRequestCount - coldStartCount));
        checkpointSummaryVO.setCloudFallbackCount(cloudRequestCount);
        summaryVO.setCheckpointSummary(checkpointSummaryVO);

        // 6. 节点资源图
        NodeResourceChartVO nodeResourceChartVO = new NodeResourceChartVO();
        nodeResourceChartVO.setNodeNames(nodeList.stream().map(EdgeNode::getNodeName).collect(Collectors.toList()));
        nodeResourceChartVO.setCpuUsageList(nodeList.stream().map(this::calcCpuUsage).collect(Collectors.toList()));
        nodeResourceChartVO.setMemoryUsageList(nodeList.stream().map(this::calcMemoryUsage).collect(Collectors.toList()));
        nodeResourceChartVO.setStorageUsageList(nodeList.stream().map(this::calcStorageUsage).collect(Collectors.toList()));
        summaryVO.setNodeResourceChart(nodeResourceChartVO);

        // 7. 云边请求分配图
        RequestDistributionVO requestDistributionVO = new RequestDistributionVO();
        requestDistributionVO.setEdgeRequestCount(edgeRequestCount);
        requestDistributionVO.setCloudRequestCount(cloudRequestCount);
        summaryVO.setRequestDistribution(requestDistributionVO);

        // 8. 趋势图
        TrendChartVO trendChartVO = new TrendChartVO();
        trendChartVO.setCheckpointLabels(latestRecords.stream()
                .map(item -> "CP" + item.getCheckpointNo())
                .collect(Collectors.toList()));
        trendChartVO.setRequestCountList(latestRecords.stream()
                .map(item -> defaultInt(item.getTotalRequestCount()))
                .collect(Collectors.toList()));
        trendChartVO.setAvgLatencyList(latestRecords.stream()
                .map(item -> defaultDouble(item.getAvgLatency()))
                .collect(Collectors.toList()));
        trendChartVO.setColdStartCountList(latestRecords.stream()
                .map(item -> defaultInt(item.getColdStartCount()))
                .collect(Collectors.toList()));
        trendChartVO.setReconfigurationCountList(latestRecords.stream()
                .map(item -> defaultInt(item.getReconfigurationCount()))
                .collect(Collectors.toList()));
        summaryVO.setTrendChart(trendChartVO);

        return summaryVO;
    }

    private Integer calcCpuUsage(EdgeNode node) {
        if (node.getCpuTotal() == null || node.getCpuTotal() == 0 || node.getCpuUsed() == null) {
            return 0;
        }
        return Math.min(100, (int) Math.round(node.getCpuUsed() * 100.0 / node.getCpuTotal()));
    }

    private Integer calcMemoryUsage(EdgeNode node) {
        if (node.getMemoryTotal() == null || node.getMemoryTotal() == 0 || node.getMemoryUsed() == null) {
            return 0;
        }
        return Math.min(100, (int) Math.round(node.getMemoryUsed() * 100.0 / node.getMemoryTotal()));
    }

    private Integer calcStorageUsage(EdgeNode node) {
        if (node.getStorageTotal() == null || node.getStorageTotal() == 0 || node.getStorageUsed() == null) {
            return 0;
        }
        return Math.min(100, (int) Math.round(node.getStorageUsed() * 100.0 / node.getStorageTotal()));
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private double defaultDouble(Double value) {
        return value == null ? 0D : value;
    }
}