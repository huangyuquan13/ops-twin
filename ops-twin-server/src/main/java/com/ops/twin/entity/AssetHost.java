package com.ops.twin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("asset_host")
public class AssetHost {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String hostname;
    private String ipAddr;
    private Integer status; // 1健康/2报警/3宕机
    private Integer cpuCores;
    private Integer memoryGb;
    private String hostType; // 主机类型: WEB, APP, DB, CACHE, LB, MQ
    private String description; // 用途描述
    private String cabinetId;
    private Integer rackPos;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
