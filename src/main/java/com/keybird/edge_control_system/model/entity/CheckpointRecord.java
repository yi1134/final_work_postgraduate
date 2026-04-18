package com.keybird.edge_control_system.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("checkpoint_record")
public class CheckpointRecord {

    @TableId(type = IdType.AUTO)
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

    private LocalDateTime createTime;
}