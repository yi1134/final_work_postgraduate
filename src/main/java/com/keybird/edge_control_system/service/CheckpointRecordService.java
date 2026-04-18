package com.keybird.edge_control_system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.keybird.edge_control_system.model.dto.log.CheckpointRecordQueryRequest;
import com.keybird.edge_control_system.model.vo.log.CheckpointRecordVO;

public interface CheckpointRecordService {

    IPage<CheckpointRecordVO> listCheckpointRecordByPage(CheckpointRecordQueryRequest request);
}