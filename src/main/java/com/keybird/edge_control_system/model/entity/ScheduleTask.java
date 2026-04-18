package com.keybird.edge_control_system.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("schedule_task")
public class ScheduleTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskCode;

    private String taskName;

    /**
     * PENDING / RUNNING / FINISHED / STOPPED / FAILED
     */
    private String taskStatus;

    private Integer checkpointCount;

    private Integer currentCheckpoint;

    private Integer enableSpecialCase;

    private String selectedNodeIds;

    private String taskConfigJson;

    private String summaryJson;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @TableLogic
    private Integer isDeleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}