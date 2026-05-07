package com.ops.twin.controller;

import com.ops.twin.common.Result;
import com.ops.twin.entity.AssetHost;
import com.ops.twin.mapper.AssetHostMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/asset")
@CrossOrigin // 支持跨域
public class AssetController {

    @Autowired
    private AssetHostMapper assetHostMapper;

    // 获取主机列表（支持模糊搜索 + 分页）
    @GetMapping("/host/list")
    public Result<com.baomidou.mybatisplus.core.metadata.IPage<AssetHost>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            String hostname, String ipAddr, String cabinetId) {
        
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<AssetHost> page = 
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(current, size);
            
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AssetHost> wrapper = 
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        
        if (hostname != null && !hostname.isEmpty()) {
            wrapper.like(AssetHost::getHostname, hostname);
        }
        if (ipAddr != null && !ipAddr.isEmpty()) {
            wrapper.like(AssetHost::getIpAddr, ipAddr);
        }
        if (cabinetId != null && !cabinetId.isEmpty()) {
            wrapper.eq(AssetHost::getCabinetId, cabinetId);
        }
        
        wrapper.orderByDesc(AssetHost::getCreateTime);
        
        com.baomidou.mybatisplus.core.metadata.IPage<AssetHost> result = assetHostMapper.selectPage(page, wrapper);
        return Result.success(result);
    }

    // 保存或更新主机
    @PostMapping("/host/save")
    public Result<String> save(@RequestBody AssetHost host) {
        // 【核心业务逻辑修复】检查机柜插槽冲突：同一个机柜的同一个 U 位只能有一台物理机
        if (host.getCabinetId() != null && host.getRackPos() != null) {
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AssetHost> checkWrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            checkWrapper.eq(AssetHost::getCabinetId, host.getCabinetId())
                        .eq(AssetHost::getRackPos, host.getRackPos());
            
            if (host.getId() != null) {
                checkWrapper.ne(AssetHost::getId, host.getId()); // 修改时排除自己
            }
            
            if (assetHostMapper.selectCount(checkWrapper) > 0) {
                return Result.error("保存失败：机柜 " + host.getCabinetId() + " 的 " + host.getRackPos() + "U 插槽已被其他资产占用！");
            }
        }

        if (host.getId() != null) {
            assetHostMapper.updateById(host);
        } else {
            assetHostMapper.insert(host);
        }
        return Result.success("操作成功");
    }

    // 删除主机
    @DeleteMapping("/host/delete/{id}")
    public Result<String> delete(@PathVariable Long id) {
        assetHostMapper.deleteById(id);
        return Result.success("删除成功");
    }
}
