package com.keybird.edge_control_system.service;

import com.keybird.edge_control_system.model.vo.agent.*;

public interface AgentClientService {

    boolean ping(String ipAddress, Integer port);

    AgentStatusVO getAgentStatus(String ipAddress, Integer port);

    boolean prepareTask(String ipAddress, Integer port, AgentTaskPrepareRequest request);

    boolean startTask(String ipAddress, Integer port, String taskId);

    AgentTaskRuntimeVO getTaskRuntime(String ipAddress, Integer port);

    AgentCheckpointGenerateVO generateNextCheckpoint(String ipAddress, Integer port, String taskId);
}