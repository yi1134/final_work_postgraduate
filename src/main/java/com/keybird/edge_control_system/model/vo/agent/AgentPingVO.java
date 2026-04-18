package com.keybird.edge_control_system.model.vo.agent;

import lombok.Data;

@Data
public class AgentPingVO {

    private String status;
    private String service;
    private Long timestamp;
}