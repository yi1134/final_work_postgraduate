package com.keybird.edge_control_system.controller;

import com.keybird.edge_control_system.common.BaseResponse;
import com.keybird.edge_control_system.common.ResultUtils;
import com.keybird.edge_control_system.model.dto.schedule.ScheduleTaskCreateRequest;
import com.keybird.edge_control_system.model.vo.schedule.ScheduleTaskVO;
import com.keybird.edge_control_system.service.ScheduleTaskService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/schedule")
public class ScheduleTaskController {

    @Resource
    private ScheduleTaskService scheduleTaskService;

    @PostMapping("/create")
    public BaseResponse<ScheduleTaskVO> createTask(@Validated @RequestBody ScheduleTaskCreateRequest request) {
        return ResultUtils.success(scheduleTaskService.createTask(request));
    }

    @PostMapping("/prepare")
    public BaseResponse<Boolean> prepareTask(@RequestParam Long taskId) {
        return ResultUtils.success(scheduleTaskService.prepareTask(taskId));
    }

    @PostMapping("/start")
    public BaseResponse<Boolean> startTask(@RequestParam Long taskId) {
        return ResultUtils.success(scheduleTaskService.startTask(taskId));
    }

//    @PostMapping("/next")
//    public BaseResponse<Boolean> runNextCheckpoint(@RequestParam Long taskId) {
//        return ResultUtils.success(scheduleTaskService.runNextCheckpoint(taskId));
//    }

    @GetMapping("/detail")
    public BaseResponse<ScheduleTaskVO> getTaskDetail(@RequestParam Long taskId) {
        return ResultUtils.success(scheduleTaskService.getTaskDetail(taskId));
    }
}