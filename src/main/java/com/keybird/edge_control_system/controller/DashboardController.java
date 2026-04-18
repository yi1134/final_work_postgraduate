package com.keybird.edge_control_system.controller;

import com.keybird.edge_control_system.common.BaseResponse;
import com.keybird.edge_control_system.common.ResultUtils;
import com.keybird.edge_control_system.model.vo.DashboardSummaryVO;
import com.keybird.edge_control_system.service.DashboardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/dashboard")
@Api(tags = "仪表盘接口组")
public class DashboardController {

    @Resource
    private DashboardService dashboardService;

    @GetMapping("/summary")
    @ApiOperation(value = "获取仪表盘信息")
    public BaseResponse<DashboardSummaryVO> getDashboardSummary() {
        return ResultUtils.success(dashboardService.getDashboardSummary());
    }
}