package com.ops.twin.controller;

import com.ops.twin.audit.AuditLog;
import com.ops.twin.common.Result;
import com.ops.twin.entity.AssetHost;
import com.ops.twin.mapper.AssetHostMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/asset")
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

    @Autowired
    private com.ops.twin.mapper.AssetCabinetMapper assetCabinetMapper;

    // 保存或更新主机
    @AuditLog(operation = "CREATE_HOST", description = "新增主机")
    @PostMapping("/host/save")
    public Result<String> save(@RequestBody AssetHost host) {
        // 1. 校验主机名唯一性
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AssetHost> nameWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        nameWrapper.eq(AssetHost::getHostname, host.getHostname());
        if (host.getId() != null) nameWrapper.ne(AssetHost::getId, host.getId());
        if (assetHostMapper.selectCount(nameWrapper) > 0) {
            return Result.error("主机名称 [" + host.getHostname() + "] 已被占用");
        }

        // 2. 校验 IP 地址唯一性
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AssetHost> ipWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        ipWrapper.eq(AssetHost::getIpAddr, host.getIpAddr());
        if (host.getId() != null) ipWrapper.ne(AssetHost::getId, host.getId());
        if (assetHostMapper.selectCount(ipWrapper) > 0) {
            return Result.error("IP 地址 [" + host.getIpAddr() + "] 已存在");
        }

        // 3. 校验机柜插槽冲突及高度溢出
        if (host.getCabinetId() != null && host.getRackPos() != null) {
            // 首先获取机柜信息，查出其 maxU
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.ops.twin.entity.AssetCabinet> cabWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            cabWrapper.eq(com.ops.twin.entity.AssetCabinet::getCabinetId, host.getCabinetId());
            com.ops.twin.entity.AssetCabinet cabinet = assetCabinetMapper.selectOne(cabWrapper);
            
            if (cabinet != null && host.getRackPos() > cabinet.getMaxU()) {
                return Result.error("保存失败：机柜 " + host.getCabinetId() + " 最高只有 " + cabinet.getMaxU() + "U，无法放置在 " + host.getRackPos() + "U 位");
            }

            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AssetHost> slotWrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            slotWrapper.eq(AssetHost::getCabinetId, host.getCabinetId())
                       .eq(AssetHost::getRackPos, host.getRackPos());
            
            if (host.getId() != null) {
                slotWrapper.ne(AssetHost::getId, host.getId());
            }
            
            if (assetHostMapper.selectCount(slotWrapper) > 0) {
                return Result.error("保存失败：机柜 " + host.getCabinetId() + " 的 " + host.getRackPos() + "U 插槽已被占用！");
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
    @AuditLog(operation = "DELETE_HOST", description = "删除主机")
    @DeleteMapping("/host/delete/{id}")
    public Result<String> delete(@PathVariable Long id) {
        assetHostMapper.deleteById(id);
        return Result.success("删除成功");
    }
}
