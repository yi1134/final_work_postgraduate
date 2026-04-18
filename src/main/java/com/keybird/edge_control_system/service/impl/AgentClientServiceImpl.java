package com.keybird.edge_control_system.service.impl;

import com.keybird.edge_control_system.model.vo.agent.*;
import com.keybird.edge_control_system.service.AgentClientService;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;

@Service
public class AgentClientServiceImpl implements AgentClientService {

    @Resource
    private RestTemplate restTemplate;

    @Override
    public boolean ping(String ipAddress, Integer port) {
        String url = buildBaseUrl(ipAddress, port) + "/agent/ping";
        try {
            ResponseEntity<AgentBaseResponse<AgentPingVO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<AgentBaseResponse<AgentPingVO>>() {}
            );
            AgentBaseResponse<AgentPingVO> body = response.getBody();
            return body != null && body.getCode() == 0 && body.getData() != null
                    && "UP".equalsIgnoreCase(body.getData().getStatus());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public AgentStatusVO getAgentStatus(String ipAddress, Integer port) {
        String url = buildBaseUrl(ipAddress, port) + "/agent/status";
        try {
            ResponseEntity<AgentBaseResponse<AgentStatusVO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<AgentBaseResponse<AgentStatusVO>>() {}
            );
            AgentBaseResponse<AgentStatusVO> body = response.getBody();
            if (body == null || body.getCode() != 0 || body.getData() == null) {
                throw new RuntimeException("获取边缘节点状态失败");
            }
            return body.getData();
        } catch (Exception e) {
            throw new RuntimeException("边缘节点不可访问或状态获取失败");
        }
    }

    private String buildBaseUrl(String ipAddress, Integer port) {
        return "http://" + ipAddress + ":" + port;
    }


    @Override
    public boolean prepareTask(String ipAddress, Integer port, AgentTaskPrepareRequest request) {
        String url = buildBaseUrl(ipAddress, port) + "/agent/task/prepare";
        try {
            ResponseEntity<AgentBaseResponse<Boolean>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new org.springframework.http.HttpEntity<>(request),
                    new ParameterizedTypeReference<AgentBaseResponse<Boolean>>() {}
            );
            AgentBaseResponse<Boolean> body = response.getBody();
            return body != null && body.getCode() == 0 && Boolean.TRUE.equals(body.getData());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean startTask(String ipAddress, Integer port, String taskId) {
        String url = buildBaseUrl(ipAddress, port) + "/agent/task/start?taskId=" + taskId;
        try {
            ResponseEntity<AgentBaseResponse<Boolean>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    null,
                    new ParameterizedTypeReference<AgentBaseResponse<Boolean>>() {}
            );
            AgentBaseResponse<Boolean> body = response.getBody();
            return body != null && body.getCode() == 0 && Boolean.TRUE.equals(body.getData());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public AgentTaskRuntimeVO getTaskRuntime(String ipAddress, Integer port) {
        String url = buildBaseUrl(ipAddress, port) + "/agent/task/runtime";
        try {
            ResponseEntity<AgentBaseResponse<AgentTaskRuntimeVO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<AgentBaseResponse<AgentTaskRuntimeVO>>() {}
            );
            AgentBaseResponse<AgentTaskRuntimeVO> body = response.getBody();
            if (body == null || body.getCode() != 0 || body.getData() == null) {
                throw new RuntimeException("获取边缘任务状态失败");
            }
            return body.getData();
        } catch (Exception e) {
            throw new RuntimeException("边缘任务状态获取失败");
        }
    }

    @Override
    public AgentCheckpointGenerateVO generateNextCheckpoint(String ipAddress, Integer port, String taskId) {
        String url = buildBaseUrl(ipAddress, port) + "/agent/cp/next?taskId=" + taskId;
        try {
            ResponseEntity<AgentBaseResponse<AgentCheckpointGenerateVO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    null,
                    new ParameterizedTypeReference<AgentBaseResponse<AgentCheckpointGenerateVO>>() {}
            );
            AgentBaseResponse<AgentCheckpointGenerateVO> body = response.getBody();
            if (body == null || body.getCode() != 0 || body.getData() == null) {
                throw new RuntimeException("生成checkpoint失败");
            }
            return body.getData();
        } catch (Exception e) {
            throw new RuntimeException("边缘checkpoint生成失败");
        }
    }
}