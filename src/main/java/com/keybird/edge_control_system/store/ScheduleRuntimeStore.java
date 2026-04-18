package com.keybird.edge_control_system.store;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ScheduleRuntimeStore {

    private final Map<Long, TaskRuntimeState> taskStateMap = new ConcurrentHashMap<>();

    private final Map<Long, Set<String>> nodeContainerMap = new HashMap<>();
    private final Map<String, String> userLastRequestContainerMap = new HashMap<>();

    public TaskRuntimeState getOrCreateTaskState(Long taskId) {
        return taskStateMap.computeIfAbsent(taskId, k -> new TaskRuntimeState());
    }


    public Map<String, String> getUserLastRequestContainerMap() {
        return userLastRequestContainerMap;
    }

    public void clearTaskState(Long taskId) {
        taskStateMap.remove(taskId);
    }

    public static class TaskRuntimeState {
        /**
         * 上一轮每个节点部署过的容器集合
         */
        private final Map<Long, Set<String>> nodeContainerMap = new HashMap<>();

        public Map<Long, Set<String>> getNodeContainerMap() {
            return nodeContainerMap;
        }
    }
}