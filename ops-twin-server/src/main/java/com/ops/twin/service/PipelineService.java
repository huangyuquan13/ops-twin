package com.ops.twin.service;

import com.ops.twin.entity.PipelineLog;
import com.ops.twin.entity.PipelineTask;
import com.ops.twin.mapper.PipelineLogMapper;
import com.ops.twin.mapper.PipelineTaskMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PipelineService {

    @Autowired
    private PipelineTaskMapper taskMapper;

    @Autowired
    private PipelineLogMapper logMapper;

    // 模拟异步执行任务并生成流式日志
    @Async
    public void executeTaskSimulate(Long taskId) {
        String[] logs = {
            "[INFO] 正在初始化调度引擎...",
            "[INFO] 正在剥离故障节点流量...",
            "[WARN] 检测到 CPU 持续高位, 尝试强制重启服务...",
            "[INFO] 冷备机启动中... 20%",
            "[INFO] 镜像拉取成功, 正在部署新实例...",
            "[SUCCESS] 服务已完成自动迁移, 节点恢复健康。"
        };

        try {
            for (String content : logs) {
                Thread.sleep(1500); // 模拟每隔1.5秒产生一条日志
                PipelineLog log = new PipelineLog();
                log.setTaskId(taskId);
                log.setLogContent(content);
                log.setCreateTime(LocalDateTime.now());
                logMapper.insert(log);
            }
            
            // 更新任务状态为成功
            PipelineTask task = taskMapper.selectById(taskId);
            task.setStatus("SUCCESS");
            taskMapper.updateById(task);
            
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
