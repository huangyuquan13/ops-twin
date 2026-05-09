package com.ops.twin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("asset_service")
public class AssetService {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String serviceName;
    private String owner;
    private String description;

    // Vue Flow的拓扑JSON，保存节点的布局和连线关系
    private String topologyJson;

    /** 非持久化字段：绑定的物理主机数量 */
    @TableField(exist = false)
    private Integer hostCount;

    private Date createTime;
    private Date updateTime;
}
