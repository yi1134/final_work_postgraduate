package com.keybird.edge_control_system.model.vo.agent;

import lombok.Data;

@Data
public class AgentTaskRuntimeVO {

    private String currentTaskId;
    private Boolean running;
    private Integer totalCheckpointCount;
    private Integer currentCheckpointNo;
    private Integer supportedContainerCount;
    private Integer feasibleContainerCount;
    private Integer userCount;
}