package com.ops.twin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ops.twin.entity.TaskPlan;
import com.ops.twin.mapper.TaskPlanMapper;
import com.ops.twin.service.TaskPlanService;
import org.springframework.stereotype.Service;

/**
 * 演练预案 Service 实现类
 */
@Service
public class TaskPlanServiceImpl extends ServiceImpl<TaskPlanMapper, TaskPlan> implements TaskPlanService {
}
