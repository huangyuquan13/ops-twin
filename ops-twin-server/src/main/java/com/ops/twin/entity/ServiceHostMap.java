package com.ops.twin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("service_host_map")
public class ServiceHostMap {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long serviceId;
    private Long hostId;
}
