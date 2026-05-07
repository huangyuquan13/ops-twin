package com.ops.twin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ops.twin.common.Result;
import com.ops.twin.entity.PipelineLog;
import com.ops.twin.entity.PipelineTask;
import com.ops.twin.mapper.PipelineLogMapper;
import com.ops.twin.mapper.PipelineTaskMapper;
import com.ops.twin.service.PipelineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pipeline")
@CrossOrigin
public class PipelineController {

    @Autowired
    private PipelineTaskMapper taskMapper;
    
    @Autowired
    private PipelineLogMapper logMapper;

    @Autowired
    private PipelineService pipelineService;

    @PostMapping("/execute")
    public Result<Long> execute(@RequestBody PipelineTask task) {
        task.setStatus("RUNNING");
        taskMapper.insert(task);
        
        // 开启异步模拟执行
        pipelineService.executeTaskSimulate(task.getId());
        
        return Result.success(task.getId());
    }

    @GetMapping("/logs/{taskId}")
    public Result<List<PipelineLog>> getLogs(@PathVariable Long taskId) {
        LambdaQueryWrapper<PipelineLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PipelineLog::getTaskId, taskId).orderByAsc(PipelineLog::getCreateTime);
        return Result.success(logMapper.selectList(wrapper));
    }
}
