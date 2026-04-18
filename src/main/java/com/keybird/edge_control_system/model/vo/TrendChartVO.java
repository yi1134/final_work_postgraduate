package com.keybird.edge_control_system.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class TrendChartVO {

    private List<String> checkpointLabels;

    private List<Integer> requestCountList;

    private List<Double> avgLatencyList;

    private List<Integer> coldStartCountList;

    private List<Integer> reconfigurationCountList;
}