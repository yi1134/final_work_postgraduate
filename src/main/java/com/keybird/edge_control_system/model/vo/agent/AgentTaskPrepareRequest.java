package com.keybird.edge_control_system.model.vo.agent;

import lombok.Data;

import java.util.List;

@Data
public class AgentTaskPrepareRequest {

    private String taskId;
    private Integer checkpointCount;
    private List<ContainerMeta> containerList;

    @Data
    public static class ContainerMeta {
        private String containerCode;
        private String containerName;
        private Integer requiredCpu;
        private Integer requiredMemory;
        private Integer requiredStorage;
    }
}