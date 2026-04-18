package com.keybird.edge_control_system.service;

import com.keybird.edge_control_system.model.dto.schedule.ScheduleTaskCreateRequest;
import com.keybird.edge_control_system.model.vo.schedule.ScheduleTaskVO;

public interface ScheduleTaskService {

    ScheduleTaskVO createTask(ScheduleTaskCreateRequest request);

    boolean prepareTask(Long taskId);

    boolean startTask(Long taskId);

//    boolean runNextCheckpoint(Long taskId);

    ScheduleTaskVO getTaskDetail(Long taskId);
}