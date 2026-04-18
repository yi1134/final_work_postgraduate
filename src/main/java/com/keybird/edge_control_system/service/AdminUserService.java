package com.keybird.edge_control_system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.keybird.edge_control_system.model.dto.AdminLoginRequest;
import com.keybird.edge_control_system.model.dto.AdminRegisterRequest;
import com.keybird.edge_control_system.model.entity.AdminUser;
import com.keybird.edge_control_system.model.vo.LoginAdminVO;

import javax.servlet.http.HttpServletRequest;

public interface AdminUserService extends IService<AdminUser> {

    Long register(AdminRegisterRequest request);

    LoginAdminVO login(AdminLoginRequest request, HttpServletRequest httpRequest);

    boolean logout(HttpServletRequest httpRequest);

    LoginAdminVO getLoginAdmin(HttpServletRequest httpRequest);

    void initDefaultAdmin();
}