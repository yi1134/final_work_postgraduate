package com.keybird.edge_control_system.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.keybird.edge_control_system.mapper.EdgeNodeMapper;
import com.keybird.edge_control_system.model.dto.node.EdgeNodeAddRequest;
import com.keybird.edge_control_system.model.dto.node.EdgeNodeQueryRequest;
import com.keybird.edge_control_system.model.dto.node.EdgeNodeStatusUpdateRequest;
import com.keybird.edge_control_system.model.dto.node.EdgeNodeUpdateRequest;
import com.keybird.edge_control_system.model.entity.EdgeNode;
import com.keybird.edge_control_system.service.EdgeNodeService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EdgeNodeServiceImpl extends ServiceImpl<EdgeNodeMapper, EdgeNode> implements EdgeNodeService {

    @Override
    public Long addNode(EdgeNodeAddRequest request) {
        EdgeNode edgeNode = new EdgeNode();
        BeanUtils.copyProperties(request, edgeNode);

        edgeNode.setNodeCode("NODE_" + IdUtil.getSnowflakeNextIdStr());
        edgeNode.setCpuUsed(0);
        edgeNode.setMemoryUsed(0);
        edgeNode.setStorageUsed(0);
        edgeNode.setCurrentInstanceCount(0);
        edgeNode.setCurrentRequestCount(0);

        boolean result = this.save(edgeNode);
        if (!result) {
            throw new RuntimeException("新增节点失败");
        }
        return edgeNode.getId();
    }

    @Override
    public boolean updateNode(EdgeNodeUpdateRequest request) {
        EdgeNode oldNode = this.getById(request.getId());
        if (oldNode == null) {
            throw new RuntimeException("节点不存在");
        }

        EdgeNode edgeNode = new EdgeNode();
        BeanUtils.copyProperties(request, edgeNode);

        boolean result = this.updateById(edgeNode);
        if (!result) {
            throw new RuntimeException("更新节点失败");
        }
        return true;
    }

    @Override
    public boolean deleteNode(Long id) {
        EdgeNode oldNode = this.getById(id);
        if (oldNode == null) {
            throw new RuntimeException("节点不存在");
        }
        return this.removeById(id);
    }

    @Override
    public boolean updateNodeStatus(EdgeNodeStatusUpdateRequest request) {
        EdgeNode oldNode = this.getById(request.getId());
        if (oldNode == null) {
            throw new RuntimeException("节点不存在");
        }
        oldNode.setStatus(request.getStatus());
        return this.updateById(oldNode);
    }

    @Override
    public IPage<EdgeNode> listNodeByPage(EdgeNodeQueryRequest request) {
        QueryWrapper<EdgeNode> queryWrapper = new QueryWrapper<>();

        if (StringUtils.hasText(request.getNodeName())) {
            queryWrapper.like("node_name", request.getNodeName());
        }
        if (StringUtils.hasText(request.getNodeType())) {
            queryWrapper.eq("node_type", request.getNodeType());
        }
        if (StringUtils.hasText(request.getStatus())) {
            queryWrapper.eq("status", request.getStatus());
        }

        queryWrapper.orderByDesc("id");

        Page<EdgeNode> page = new Page<>(request.getCurrent(), request.getPageSize());
        return this.page(page, queryWrapper);
    }
}