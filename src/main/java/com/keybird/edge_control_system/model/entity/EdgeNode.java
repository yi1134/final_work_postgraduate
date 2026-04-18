package com.keybird.edge_control_system.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("edge_node")
public class EdgeNode {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String nodeCode;

    private String nodeName;

    /**
     * CLOUD / EDGE
     */
    private String nodeType;

    private String ipAddress;

    private Integer port;

    private String region;

    /**
     * ONLINE / OFFLINE / MAINTAIN
     */
    private String status;

    private Integer cpuTotal;

    private Integer memoryTotal;

    private Integer storageTotal;

    private Integer bandwidth;

    private Integer cpuUsed;

    private Integer memoryUsed;

    private Integer storageUsed;

    private Integer currentInstanceCount;

    private Integer currentRequestCount;

    private LocalDateTime lastHeartbeatTime;

    private String description;

    @TableLogic
    private Integer isDeleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}