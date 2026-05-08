package com.ops.twin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ops.twin.entity.TaskRecord;
import com.ops.twin.mapper.TaskRecordMapper;
import com.ops.twin.service.TaskRecordService;
import org.springframework.stereotype.Service;

/**
 * 任务执行流水 Service 实现类
 */
@Service
public class TaskRecordServiceImpl extends ServiceImpl<TaskRecordMapper, TaskRecord> implements TaskRecordService {
}
