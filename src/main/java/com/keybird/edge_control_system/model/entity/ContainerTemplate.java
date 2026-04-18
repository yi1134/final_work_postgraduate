package com.keybird.edge_control_system.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("container_template")
public class ContainerTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String templateCode;

    private String containerName;

    private String serviceType;

    private String imageName;

    private String imageVersion;

    private Integer requiredCpu;

    private Integer requiredMemory;

    private Integer requiredStorage;

    private Integer coldStartDelay;

    /**
     * ENABLED / DISABLED
     */
    private String status;

    private String description;

    @TableLogic
    private Integer isDeleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}