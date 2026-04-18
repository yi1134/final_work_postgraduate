package com.keybird.edge_control_system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.keybird.edge_control_system.common.BaseResponse;
import com.keybird.edge_control_system.common.ResultUtils;
import com.keybird.edge_control_system.model.dto.log.CheckpointRecordQueryRequest;
import com.keybird.edge_control_system.model.vo.log.CheckpointRecordVO;
import com.keybird.edge_control_system.service.CheckpointRecordService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/log")
public class LogController {

    @Resource
    private CheckpointRecordService checkpointRecordService;

    @PostMapping("/checkpoint/list/page")
    public BaseResponse<IPage<CheckpointRecordVO>> listCheckpointRecordByPage(
            @RequestBody CheckpointRecordQueryRequest request) {
        return ResultUtils.success(checkpointRecordService.listCheckpointRecordByPage(request));
    }
}