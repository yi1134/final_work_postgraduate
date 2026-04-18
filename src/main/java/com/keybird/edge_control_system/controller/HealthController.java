package com.keybird.edge_control_system.controller;

import com.keybird.edge_control_system.common.BaseResponse;
import com.keybird.edge_control_system.common.ResultUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@Api(tags = "健康检测")
public class HealthController {

    @GetMapping("/health")
    @ApiOperation(value = "健康检查")
    public BaseResponse<Map<String, Object>> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("service", "edge-control-system");
        result.put("timestamp", System.currentTimeMillis());
        return ResultUtils.success(result);
    }
}