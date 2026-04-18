package com.keybird.edge_control_system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.keybird.edge_control_system.mapper.AdminUserMapper;
import com.keybird.edge_control_system.model.dto.AdminLoginRequest;
import com.keybird.edge_control_system.model.dto.AdminRegisterRequest;
import com.keybird.edge_control_system.model.entity.AdminUser;
import com.keybird.edge_control_system.model.vo.LoginAdminVO;
import com.keybird.edge_control_system.service.AdminUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@Service
@Slf4j
public class AdminUserServiceImpl extends ServiceImpl<AdminUserMapper, AdminUser> implements AdminUserService {

    public static final String LOGIN_ADMIN_STATE = "loginAdmin";

    private static final String SALT = "keybird-edge-system";

    @Value("${system.init-admin.username:admin}")
    private String initUsername;

    @Value("${system.init-admin.password:123456}")
    private String initPassword;

    @Value("${system.init-admin.nickname:超级管理员}")
    private String initNickname;

    @Override
    public Long register(AdminRegisterRequest request) {
        QueryWrapper<AdminUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", request.getUsername());
        Long count = this.baseMapper.selectCount(queryWrapper);
        if (count != null && count > 0) {
            throw new RuntimeException("用户名已存在");
        }

        AdminUser adminUser = new AdminUser();
        adminUser.setUsername(request.getUsername());
        adminUser.setPassword(encryptPassword(request.getPassword()));
        adminUser.setNickname(request.getNickname());
        adminUser.setStatus(1);

        int inserted = this.baseMapper.insert(adminUser);
        if (inserted <= 0) {
            throw new RuntimeException("注册失败");
        }
        return adminUser.getId();
    }

    @Override
    public LoginAdminVO login(AdminLoginRequest request, HttpServletRequest httpRequest) {
        String encryptPassword = encryptPassword(request.getPassword());

        QueryWrapper<AdminUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", request.getUsername());
        queryWrapper.eq("password", encryptPassword);
        queryWrapper.eq("status", 1);

        AdminUser adminUser = this.baseMapper.selectOne(queryWrapper);
        if (adminUser == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        HttpSession session = httpRequest.getSession();
        session.setAttribute(LOGIN_ADMIN_STATE, adminUser.getId());

        LoginAdminVO loginAdminVO = new LoginAdminVO();
        BeanUtils.copyProperties(adminUser, loginAdminVO);
        return loginAdminVO;
    }

    @Override
    public boolean logout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.removeAttribute(LOGIN_ADMIN_STATE);
        }
        return true;
    }

    @Override
    public LoginAdminVO getLoginAdmin(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session == null) {
            throw new RuntimeException("未登录");
        }
        Object adminIdObj = session.getAttribute(LOGIN_ADMIN_STATE);
        if (adminIdObj == null) {
            throw new RuntimeException("未登录");
        }

        Long adminId = Long.valueOf(String.valueOf(adminIdObj));
        AdminUser adminUser = this.getById(adminId);
        if (adminUser == null || adminUser.getStatus() == 0) {
            throw new RuntimeException("账号不存在或已禁用");
        }

        LoginAdminVO loginAdminVO = new LoginAdminVO();
        BeanUtils.copyProperties(adminUser, loginAdminVO);
        return loginAdminVO;
    }

    @Override
    @PostConstruct
    public void initDefaultAdmin() {
        QueryWrapper<AdminUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", initUsername);
        Long count = this.baseMapper.selectCount(queryWrapper);
        if (count != null && count > 0) {
            return;
        }

        AdminUser adminUser = new AdminUser();
        adminUser.setUsername(initUsername);
        adminUser.setPassword(encryptPassword(initPassword));
        adminUser.setNickname(initNickname);
        adminUser.setStatus(1);
        this.baseMapper.insert(adminUser);

        log.info("初始化管理员账号成功，username={}", initUsername);
    }

    private String encryptPassword(String password) {
        return DigestUtils.md5DigestAsHex((SALT + password).getBytes());
    }
}