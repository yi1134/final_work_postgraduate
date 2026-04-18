package com.keybird.edge_control_system.controller;

import com.keybird.edge_control_system.common.BaseResponse;
import com.keybird.edge_control_system.common.ResultUtils;
import com.keybird.edge_control_system.model.dto.AdminLoginRequest;
import com.keybird.edge_control_system.model.dto.AdminRegisterRequest;
import com.keybird.edge_control_system.model.vo.LoginAdminVO;
import com.keybird.edge_control_system.service.AdminUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/admin")
@Api(tags = "登陆注册接口组")
public class AdminUserController {

    @Resource
    private AdminUserService adminUserService;

    @PostMapping("/register")
    @ApiOperation(value = "用户注册")
    public BaseResponse<Long> register(@Validated @RequestBody AdminRegisterRequest request) {
        return ResultUtils.success(adminUserService.register(request));
    }

    @PostMapping("/login")
    @ApiOperation(value = "用户登录")
    public BaseResponse<LoginAdminVO> login(@Validated @RequestBody AdminLoginRequest request,
                                            HttpServletRequest httpRequest) {
        return ResultUtils.success(adminUserService.login(request, httpRequest));
    }

    @PostMapping("/logout")
    @ApiOperation(value = "用户退出登录")
    public BaseResponse<Boolean> logout(HttpServletRequest httpRequest) {
        return ResultUtils.success(adminUserService.logout(httpRequest));
    }

    @GetMapping("/get/login")
    @ApiOperation(value = "获取用户信息")
    public BaseResponse<LoginAdminVO> getLoginAdmin(HttpServletRequest httpRequest) {
        return ResultUtils.success(adminUserService.getLoginAdmin(httpRequest));
    }
}