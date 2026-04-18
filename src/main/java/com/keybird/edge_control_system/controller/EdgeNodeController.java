package com.keybird.edge_control_system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.keybird.edge_control_system.common.BaseResponse;
import com.keybird.edge_control_system.common.ResultUtils;
import com.keybird.edge_control_system.model.dto.node.*;
import com.keybird.edge_control_system.model.entity.EdgeNode;
import com.keybird.edge_control_system.model.vo.node.EdgeNodeConnectTestVO;
import com.keybird.edge_control_system.service.AgentClientService;
import com.keybird.edge_control_system.service.EdgeNodeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/node")
@Api(tags = "节点管理接口组")
public class EdgeNodeController {

    @Resource
    private EdgeNodeService edgeNodeService;

    @Resource
    private AgentClientService agentClientService;

    @PostMapping("/add")
    @ApiOperation(value = "增加节点")
    public BaseResponse<Long> addNode(@Validated @RequestBody EdgeNodeAddRequest request) {
        return ResultUtils.success(edgeNodeService.addNode(request));
    }

    @PostMapping("/update")
    @ApiOperation(value = "更新节点")
    public BaseResponse<Boolean> updateNode(@Validated @RequestBody EdgeNodeUpdateRequest request) {
        return ResultUtils.success(edgeNodeService.updateNode(request));
    }

    @PostMapping("/delete")
    @ApiOperation(value = "删除节点")
    public BaseResponse<Boolean> deleteNode(@RequestParam Long id) {
        return ResultUtils.success(edgeNodeService.deleteNode(id));
    }

    @PostMapping("/status/update")
    @ApiOperation(value = "修正节点状态")
    public BaseResponse<Boolean> updateNodeStatus(@Validated @RequestBody EdgeNodeStatusUpdateRequest request) {
        return ResultUtils.success(edgeNodeService.updateNodeStatus(request));
    }

    @PostMapping("/list/page")
    @ApiOperation(value = "节点列表")
    public BaseResponse<IPage<EdgeNode>> listNodeByPage(@RequestBody EdgeNodeQueryRequest request) {
        return ResultUtils.success(edgeNodeService.listNodeByPage(request));
    }

    @PostMapping("/test/connect")
    public BaseResponse<EdgeNodeConnectTestVO> testConnect(@Validated @RequestBody EdgeNodeConnectTestRequest request) {
        boolean success = agentClientService.ping(request.getIpAddress(), request.getPort());

        EdgeNodeConnectTestVO vo = new EdgeNodeConnectTestVO();
        vo.setSuccess(success);
        vo.setMessage(success ? "连接成功" : "连接失败");
        return ResultUtils.success(vo);
    }
}