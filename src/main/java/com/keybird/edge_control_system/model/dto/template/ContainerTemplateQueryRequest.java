package com.keybird.edge_control_system.model.dto.template;

import lombok.Data;

@Data
public class ContainerTemplateQueryRequest {

    private long current = 1;

    private long pageSize = 10;

    private String containerName;

    private String serviceType;

    private String status;
}