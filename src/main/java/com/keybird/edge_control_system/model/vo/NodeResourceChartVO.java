package com.keybird.edge_control_system.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class NodeResourceChartVO {

    private List<String> nodeNames;

    private List<Integer> cpuUsageList;

    private List<Integer> memoryUsageList;

    private List<Integer> storageUsageList;
}