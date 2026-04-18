package com.keybird.edge_control_system.model.dto.node;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class EdgeNodeStatusUpdateRequest {

    @NotNull(message = "节点ID不能为空")
    private Long id;

    @NotBlank(message = "状态不能为空")
    private String status;
}