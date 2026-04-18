package com.keybird.edge_control_system.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class DashboardSummaryVO {

    private List<DashboardCardVO> summaryCards;

    private CheckpointSummaryVO checkpointSummary;

    private NodeResourceChartVO nodeResourceChart;

    private RequestDistributionVO requestDistribution;

    private TrendChartVO trendChart;
}