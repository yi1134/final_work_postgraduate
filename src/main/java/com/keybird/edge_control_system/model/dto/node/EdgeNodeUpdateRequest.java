package com.keybird.edge_control_system.model.dto.node;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class EdgeNodeUpdateRequest {

    @NotNull(message = "节点ID不能为空")
    private Long id;

    @NotBlank(message = "节点名称不能为空")
    private String nodeName;

    @NotBlank(message = "节点类型不能为空")
    private String nodeType;

    @NotBlank(message = "IP地址不能为空")
    private String ipAddress;

    @NotNull(message = "端口不能为空")
    private Integer port;

    private String region;

    @NotBlank(message = "节点状态不能为空")
    private String status;

    @Min(value = 0, message = "CPU总量不能小于0")
    private Integer cpuTotal;

    @Min(value = 0, message = "内存总量不能小于0")
    private Integer memoryTotal;

    @Min(value = 0, message = "存储总量不能小于0")
    private Integer storageTotal;

    @Min(value = 0, message = "带宽不能小于0")
    private Integer bandwidth;

    private String description;
}