package com.keybird.edge_control_system.model.dto.log;

import lombok.Data;

@Data
public class CheckpointRecordQueryRequest {

    private long current = 1;

    private long pageSize = 10;

    private Long taskId;

    private Integer checkpointNo;
}