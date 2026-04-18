package com.keybird.edge_control_system.model.vo.agent;

import lombok.Data;

@Data
public class AgentBaseResponse<T> {

    private Integer code;
    private T data;
    private String message;
}