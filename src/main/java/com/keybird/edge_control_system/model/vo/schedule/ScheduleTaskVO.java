package com.keybird.edge_control_system.model.vo.schedule;

import lombok.Data;

@Data
public class ScheduleTaskVO {

    private Long id;
    private String taskCode;
    private String taskName;
    private String taskStatus;
    private Integer checkpointCount;
    private Integer currentCheckpoint;
    private Integer enableSpecialCase;
    private String selectedNodeIds;
}