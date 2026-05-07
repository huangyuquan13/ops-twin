package com.ops.twin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("asset_cabinet")
public class AssetCabinet {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String cabinetId; // 机柜编号
    private String cabinetName; // 机柜名称
    private Float posX;
    private Float posZ;
    private Integer maxU;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
