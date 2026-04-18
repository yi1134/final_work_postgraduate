package com.keybird.edge_control_system.model.vo;

import lombok.Data;

@Data
public class DashboardCardVO {

    private String label;

    private Object value;

    public DashboardCardVO() {
    }

    public DashboardCardVO(String label, Object value) {
        this.label = label;
        this.value = value;
    }
}