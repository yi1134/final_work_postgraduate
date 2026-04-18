package com.keybird.edge_control_system.model.dto.node;

import lombok.Data;

@Data
public class EdgeNodeQueryRequest {

    private long current = 1;

    private long pageSize = 10;

    private String nodeName;

    private String nodeType;

    private String status;
}