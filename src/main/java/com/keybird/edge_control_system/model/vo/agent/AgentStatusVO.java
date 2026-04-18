package com.keybird.edge_control_system.model.vo.agent;

import lombok.Data;

@Data
public class AgentStatusVO {

    private Long nodeId;
    private String nodeName;
    private String nodeType;
    private String ipAddress;
    private Integer port;

    private Integer cpuTotal;
    private Integer memoryTotal;
    private Integer storageTotal;
    private Integer bandwidth;

    private Integer coverageUserCount;

    private Boolean running;
}