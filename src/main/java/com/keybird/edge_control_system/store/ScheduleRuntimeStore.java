package com.keybird.edge_control_system.store;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ScheduleRuntimeStore {

    private final Map<Long, TaskRuntimeState> taskStateMap = new ConcurrentHashMap<>();

    public TaskRuntimeState getOrCreateTaskState(Long taskId) {
        return taskStateMap.computeIfAbsent(taskId, k -> new TaskRuntimeState());
    }

    public void clearTaskState(Long taskId) {
        taskStateMap.remove(taskId);
    }

    public static class TaskRuntimeState {

        /**
         * 上一轮每个节点驻留的容器集合
         */
        private final Map<Long, Set<String>> nodeContainerMap = new HashMap<>();

        /**
         * 上一轮每个用户请求的容器
         */
        private final Map<String, String> userLastRequestContainerMap = new HashMap<>();

        public Map<Long, Set<String>> getNodeContainerMap() {
            return nodeContainerMap;
        }

        public Map<String, String> getUserLastRequestContainerMap() {
            return userLastRequestContainerMap;
        }
    }
}