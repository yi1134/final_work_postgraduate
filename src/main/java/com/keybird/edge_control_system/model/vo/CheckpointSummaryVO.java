package com.keybird.edge_control_system.model.vo;

import lombok.Data;

@Data
public class CheckpointSummaryVO {

    private Integer currentCheckpoint;

    private Integer newRequestCount;

    private Integer activeRequestCount;

    private Integer newInstanceCount;

    private Integer reuseInstanceCount;

    private Integer cloudFallbackCount;
}