package com.ops.twin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ops.twin.common.Result;
import com.ops.twin.entity.AssetCabinet;
import com.ops.twin.entity.AssetHost;
import com.ops.twin.service.AssetCabinetService;
import com.ops.twin.mapper.AssetHostMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/asset/cabinet")
@CrossOrigin
public class AssetCabinetController {

    @Autowired
    private AssetCabinetService cabinetService;
    
    @Autowired
    private AssetHostMapper hostMapper;

    // 获取所有机柜 (无分页，供3D大屏和下拉框使用)
    @GetMapping("/list/all")
    public Result<List<AssetCabinet>> listAll() {
        return Result.success(cabinetService.list());
    }

    // 分页查询机柜列表
    @GetMapping("/list")
    public Result<IPage<AssetCabinet>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            String cabinetId, String cabinetName) {
            
        Page<AssetCabinet> page = new Page<>(current, size);
        LambdaQueryWrapper<AssetCabinet> wrapper = new LambdaQueryWrapper<>();
        
        if (cabinetId != null && !cabinetId.isEmpty()) {
            wrapper.like(AssetCabinet::getCabinetId, cabinetId);
        }
        if (cabinetName != null && !cabinetName.isEmpty()) {
            wrapper.like(AssetCabinet::getCabinetName, cabinetName);
        }
        wrapper.orderByDesc(AssetCabinet::getCreateTime);
        
        return Result.success(cabinetService.page(page, wrapper));
    }

    // 保存或更新机柜
    @PostMapping("/save")
    public Result<AssetCabinet> save(@RequestBody AssetCabinet cabinet) {
        // 1. 校验编号唯一性
        LambdaQueryWrapper<AssetCabinet> idWrapper = new LambdaQueryWrapper<>();
        idWrapper.eq(AssetCabinet::getCabinetId, cabinet.getCabinetId());
        if (cabinet.getId() != null) {
            idWrapper.ne(AssetCabinet::getId, cabinet.getId());
        }
        if (cabinetService.count(idWrapper) > 0) {
            return Result.error("机柜编号 [" + cabinet.getCabinetId() + "] 已存在");
        }

        // 2. 校验空间坐标唯一性 (防止机柜重叠)
        LambdaQueryWrapper<AssetCabinet> posWrapper = new LambdaQueryWrapper<>();
        posWrapper.eq(AssetCabinet::getPosX, cabinet.getPosX())
                  .eq(AssetCabinet::getPosZ, cabinet.getPosZ());
        if (cabinet.getId() != null) {
            posWrapper.ne(AssetCabinet::getId, cabinet.getId());
        }
        if (cabinetService.count(posWrapper) > 0) {
            return Result.error("坐标点 (X:" + cabinet.getPosX() + ", Z:" + cabinet.getPosZ() + ") 已有其他机柜占用，请调整位置");
        }

        // 3. 校验最大 U 位有效性 (防止机柜高度缩减导致已有设备越界)
        if (cabinet.getId() != null) {
            LambdaQueryWrapper<AssetHost> hostWrapper = new LambdaQueryWrapper<>();
            hostWrapper.eq(AssetHost::getCabinetId, cabinet.getCabinetId())
                      .gt(AssetHost::getRackPos, cabinet.getMaxU());
            if (hostMapper.selectCount(hostWrapper) > 0) {
                return Result.error("修改失败：当前机柜内已安装有超过 " + cabinet.getMaxU() + "U 的服务器，请先迁移资产");
            }
        }
        
        cabinetService.saveOrUpdate(cabinet);
        return Result.success(cabinet);
    }

    // 删除机柜
    @DeleteMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        AssetCabinet cabinet = cabinetService.getById(id);
        if (cabinet != null) {
            // 校验是否有关联的主机
            LambdaQueryWrapper<AssetHost> hostWrapper = new LambdaQueryWrapper<>();
            hostWrapper.eq(AssetHost::getCabinetId, cabinet.getCabinetId());
            long hostCount = hostMapper.selectCount(hostWrapper);
            if (hostCount > 0) {
                return Result.error("该机柜下存在物理资产，无法删除");
            }
        }
        boolean success = cabinetService.removeById(id);
        return success ? Result.success(true) : Result.error("删除失败");
    }
}
