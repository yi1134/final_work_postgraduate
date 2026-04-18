package com.keybird.edge_control_system.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.keybird.edge_control_system.mapper.ContainerTemplateMapper;
import com.keybird.edge_control_system.model.dto.template.ContainerTemplateAddRequest;
import com.keybird.edge_control_system.model.dto.template.ContainerTemplateQueryRequest;
import com.keybird.edge_control_system.model.dto.template.ContainerTemplateUpdateRequest;
import com.keybird.edge_control_system.model.entity.ContainerTemplate;
import com.keybird.edge_control_system.service.ContainerTemplateService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ContainerTemplateServiceImpl extends ServiceImpl<ContainerTemplateMapper, ContainerTemplate>
        implements ContainerTemplateService {

    @Override
    public Long addTemplate(ContainerTemplateAddRequest request) {
        ContainerTemplate template = new ContainerTemplate();
        BeanUtils.copyProperties(request, template);
        template.setTemplateCode("TPL_" + IdUtil.getSnowflakeNextIdStr());

        boolean result = this.save(template);
        if (!result) {
            throw new RuntimeException("新增容器模板失败");
        }
        return template.getId();
    }

    @Override
    public boolean updateTemplate(ContainerTemplateUpdateRequest request) {
        ContainerTemplate oldTemplate = this.getById(request.getId());
        if (oldTemplate == null) {
            throw new RuntimeException("容器模板不存在");
        }

        ContainerTemplate template = new ContainerTemplate();
        BeanUtils.copyProperties(request, template);

        boolean result = this.updateById(template);
        if (!result) {
            throw new RuntimeException("更新容器模板失败");
        }
        return true;
    }

    @Override
    public boolean deleteTemplate(Long id) {
        ContainerTemplate oldTemplate = this.getById(id);
        if (oldTemplate == null) {
            throw new RuntimeException("容器模板不存在");
        }
        return this.removeById(id);
    }

    @Override
    public IPage<ContainerTemplate> listTemplateByPage(ContainerTemplateQueryRequest request) {
        QueryWrapper<ContainerTemplate> queryWrapper = new QueryWrapper<>();

        if (StringUtils.hasText(request.getContainerName())) {
            queryWrapper.like("container_name", request.getContainerName());
        }
        if (StringUtils.hasText(request.getServiceType())) {
            queryWrapper.eq("service_type", request.getServiceType());
        }
        if (StringUtils.hasText(request.getStatus())) {
            queryWrapper.eq("status", request.getStatus());
        }

        queryWrapper.orderByDesc("id");

        Page<ContainerTemplate> page = new Page<>(request.getCurrent(), request.getPageSize());
        return this.page(page, queryWrapper);
    }
}