package com.keybird.edge_control_system.model.vo.log;

import lombok.Data;

@Data
public class CheckpointRecordVO {

    private Long id;

    private Long taskId;

    private Integer checkpointNo;

    private Integer totalRequestCount;

    private Integer newRequestCount;

    private Integer continuousRequestCount;

    private Integer edgeRequestCount;

    private Integer cloudRequestCount;

    private Double avgLatency;

    private Integer coldStartCount;

    private Integer reconfigurationCount;

    private Double utilityValue;

    private String detailJson;

    private String createTime;
}