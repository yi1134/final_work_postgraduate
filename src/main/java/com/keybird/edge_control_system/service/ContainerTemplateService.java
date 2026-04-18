package com.keybird.edge_control_system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.keybird.edge_control_system.model.dto.template.ContainerTemplateAddRequest;
import com.keybird.edge_control_system.model.dto.template.ContainerTemplateQueryRequest;
import com.keybird.edge_control_system.model.dto.template.ContainerTemplateUpdateRequest;
import com.keybird.edge_control_system.model.entity.ContainerTemplate;

public interface ContainerTemplateService extends IService<ContainerTemplate> {

    Long addTemplate(ContainerTemplateAddRequest request);

    boolean updateTemplate(ContainerTemplateUpdateRequest request);

    boolean deleteTemplate(Long id);

    IPage<ContainerTemplate> listTemplateByPage(ContainerTemplateQueryRequest request);
}