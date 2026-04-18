package com.keybird.edge_control_system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.keybird.edge_control_system.mapper.CheckpointRecordMapper;
import com.keybird.edge_control_system.model.dto.log.CheckpointRecordQueryRequest;
import com.keybird.edge_control_system.model.entity.CheckpointRecord;
import com.keybird.edge_control_system.model.vo.log.CheckpointRecordVO;
import com.keybird.edge_control_system.service.CheckpointRecordService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Service
public class CheckpointRecordServiceImpl implements CheckpointRecordService {

    @Resource
    private CheckpointRecordMapper checkpointRecordMapper;

    @Override
    public IPage<CheckpointRecordVO> listCheckpointRecordByPage(CheckpointRecordQueryRequest request) {
        QueryWrapper<CheckpointRecord> queryWrapper = new QueryWrapper<>();

        if (request.getTaskId() != null) {
            queryWrapper.eq("task_id", request.getTaskId());
        }
        if (request.getCheckpointNo() != null) {
            queryWrapper.eq("checkpoint_no", request.getCheckpointNo());
        }

        queryWrapper.orderByDesc("id");

        Page<CheckpointRecord> page = new Page<>(request.getCurrent(), request.getPageSize());
        IPage<CheckpointRecord> recordPage = checkpointRecordMapper.selectPage(page, queryWrapper);

        Page<CheckpointRecordVO> voPage = new Page<>(recordPage.getCurrent(), recordPage.getSize(), recordPage.getTotal());
        voPage.setRecords(recordPage.getRecords().stream().map(item -> {
            CheckpointRecordVO vo = new CheckpointRecordVO();
            BeanUtils.copyProperties(item, vo);
            if (item.getCreateTime() != null) {
                vo.setCreateTime(item.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }
            return vo;
        }).collect(Collectors.toList()));

        return voPage;
    }
}