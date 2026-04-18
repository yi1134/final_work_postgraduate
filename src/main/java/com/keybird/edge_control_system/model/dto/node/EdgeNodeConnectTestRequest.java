package com.keybird.edge_control_system.model.dto.node;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class EdgeNodeConnectTestRequest {

    @NotBlank(message = "IP地址不能为空")
    private String ipAddress;

    @NotNull(message = "端口不能为空")
    private Integer port;
}