package com.keybird.edge_control_system.model.dto.schedule;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class ScheduleTaskCreateRequest {

    @NotBlank(message = "任务名称不能为空")
    private String taskName;

    @NotNull(message = "checkpoint数量不能为空")
    private Integer checkpointCount;

    /**
     * 0-不启用 1-启用
     */
    private Integer enableSpecialCase = 0;

    @NotEmpty(message = "请选择参与节点")
    private List<Long> selectedNodeIds;
}