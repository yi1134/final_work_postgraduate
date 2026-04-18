package com.keybird.edge_control_system.model.vo;

import lombok.Data;

@Data
public class RequestDistributionVO {

    private Integer edgeRequestCount;

    private Integer cloudRequestCount;
}