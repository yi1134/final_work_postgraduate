package com.keybird.edge_control_system.model.vo.agent;

import lombok.Data;

import java.util.List;

@Data
public class AgentCheckpointGenerateVO {

    private String taskId;
    private Integer checkpointNo;
    private Integer activeUserCount;
    private Integer newRequestCount;
    private Integer continuousRequestCount;
    private List<RequestItemVO> requestList;

    @Data
    public static class RequestItemVO {
        private String userId;
        private String containerCode;
        private Boolean continuousActive;
        private Integer userToEdgeDelayMs;
        private Integer userToCloudDelayMs;
    }
}