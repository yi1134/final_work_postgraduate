package com.keybird.edge_control_system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.keybird.edge_control_system.model.dto.node.EdgeNodeAddRequest;
import com.keybird.edge_control_system.model.dto.node.EdgeNodeQueryRequest;
import com.keybird.edge_control_system.model.dto.node.EdgeNodeStatusUpdateRequest;
import com.keybird.edge_control_system.model.dto.node.EdgeNodeUpdateRequest;
import com.keybird.edge_control_system.model.entity.EdgeNode;

public interface EdgeNodeService extends IService<EdgeNode> {

    Long addNode(EdgeNodeAddRequest request);

    boolean updateNode(EdgeNodeUpdateRequest request);

    boolean deleteNode(Long id);

    boolean updateNodeStatus(EdgeNodeStatusUpdateRequest request);

    IPage<EdgeNode> listNodeByPage(EdgeNodeQueryRequest request);
}