package com.keybird.edge_control_system.model.dto.template;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

@Data
public class ContainerTemplateAddRequest {

    @NotBlank(message = "容器名称不能为空")
    private String containerName;

    @NotBlank(message = "服务类型不能为空")
    private String serviceType;

    @NotBlank(message = "镜像名称不能为空")
    private String imageName;

    @NotBlank(message = "镜像版本不能为空")
    private String imageVersion;

    @Min(value = 0, message = "所需CPU不能小于0")
    private Integer requiredCpu;

    @Min(value = 0, message = "所需内存不能小于0")
    private Integer requiredMemory;

    @Min(value = 0, message = "所需存储不能小于0")
    private Integer requiredStorage;

    @Min(value = 0, message = "冷启动时延不能小于0")
    private Integer coldStartDelay;

    @NotBlank(message = "状态不能为空")
    private String status;

    private String description;
}