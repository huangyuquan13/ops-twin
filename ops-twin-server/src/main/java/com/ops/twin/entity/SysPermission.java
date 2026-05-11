package com.ops.twin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_permission")
public class SysPermission {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long parentId;
    private String title;
    private String path;
    private String component;
    private String icon;
    private String permissionCode;
    private Integer type;
    private Integer sort;
}
