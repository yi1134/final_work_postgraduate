package com.keybird.edge_control_system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.keybird.edge_control_system.common.BaseResponse;
import com.keybird.edge_control_system.common.ResultUtils;
import com.keybird.edge_control_system.model.dto.template.ContainerTemplateAddRequest;
import com.keybird.edge_control_system.model.dto.template.ContainerTemplateQueryRequest;
import com.keybird.edge_control_system.model.dto.template.ContainerTemplateUpdateRequest;
import com.keybird.edge_control_system.model.entity.ContainerTemplate;
import com.keybird.edge_control_system.service.ContainerTemplateService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/template")
public class ContainerTemplateController {

    @Resource
    private ContainerTemplateService containerTemplateService;

    @PostMapping("/add")
    public BaseResponse<Long> addTemplate(@Validated @RequestBody ContainerTemplateAddRequest request) {
        return ResultUtils.success(containerTemplateService.addTemplate(request));
    }

    @PostMapping("/update")
    public BaseResponse<Boolean> updateTemplate(@Validated @RequestBody ContainerTemplateUpdateRequest request) {
        return ResultUtils.success(containerTemplateService.updateTemplate(request));
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTemplate(@RequestParam Long id) {
        return ResultUtils.success(containerTemplateService.deleteTemplate(id));
    }

    @PostMapping("/list/page")
    public BaseResponse<IPage<ContainerTemplate>> listTemplateByPage(@RequestBody ContainerTemplateQueryRequest request) {
        return ResultUtils.success(containerTemplateService.listTemplateByPage(request));
    }
}