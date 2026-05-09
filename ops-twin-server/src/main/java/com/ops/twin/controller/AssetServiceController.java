package com.ops.twin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ops.twin.common.Result;
import com.ops.twin.entity.AssetHost;
import com.ops.twin.entity.AssetService;
import com.ops.twin.entity.ServiceHostMap;
import com.ops.twin.mapper.AssetHostMapper;
import com.ops.twin.service.AssetServiceService;
import com.ops.twin.service.ServiceHostMapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/asset/service")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AssetServiceController {

    @Autowired
    private AssetServiceService assetServiceService;

    @Autowired
    private ServiceHostMapService serviceHostMapService;

    @Autowired
    private AssetHostMapper assetHostMapper;

    // 分页获取服务列表（含每项绑定的物理主机数量）
    @GetMapping("/list")
    public Result<Page<AssetService>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String serviceName) {

        Page<AssetService> page = new Page<>(current, size);
        LambdaQueryWrapper<AssetService> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(serviceName)) {
            wrapper.like(AssetService::getServiceName, serviceName);
        }

        wrapper.orderByDesc(AssetService::getCreateTime);
        Page<AssetService> result = assetServiceService.page(page, wrapper);

        // 填充每个服务的物理主机绑定数量
        for (AssetService svc : result.getRecords()) {
            long count = serviceHostMapService.count(
                new LambdaQueryWrapper<ServiceHostMap>().eq(ServiceHostMap::getServiceId, svc.getId())
            );
            svc.setHostCount((int) count);
        }

        return Result.success(result);
    }

    // 保存或更新服务
    @PostMapping("/save")
    public Result<AssetService> save(@RequestBody AssetService service) {
        // 校验名称唯一性
        LambdaQueryWrapper<AssetService> nameWrapper = new LambdaQueryWrapper<>();
        nameWrapper.eq(AssetService::getServiceName, service.getServiceName());
        if (service.getId() != null) {
            nameWrapper.ne(AssetService::getId, service.getId());
        }
        if (assetServiceService.count(nameWrapper) > 0) {
            return Result.error("逻辑服务名称 [" + service.getServiceName() + "] 已存在");
        }

        assetServiceService.saveOrUpdate(service);
        return Result.success(service);
    }

    // 获取单个服务的拓扑图数据
    @GetMapping("/topology/{id}")
    public Result<AssetService> getTopology(@PathVariable Long id) {
        AssetService service = assetServiceService.getById(id);
        if (service == null) {
            return Result.error("未找到对应的逻辑服务");
        }
        return Result.success(service);
    }

    // 接收带拓扑关系的保存请求
    @PostMapping("/topology/save")
    public Result<Boolean> saveTopology(@RequestBody Map<String, Object> payload) {
        Long serviceId = Long.valueOf(payload.get("serviceId").toString());
        String topologyJson = payload.get("topologyJson").toString();
        List<Integer> hostIds = (List<Integer>) payload.get("hostIds");

        // 1. 更新 AssetService 的拓扑数据
        AssetService service = assetServiceService.getById(serviceId);
        if (service != null) {
            service.setTopologyJson(topologyJson);
            assetServiceService.updateById(service);
        } else {
            return Result.error("未找到对应的逻辑服务");
        }

        // 2. 更新 ServiceHostMap 映射关系
        // 先删除原有的映射
        LambdaQueryWrapper<ServiceHostMap> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(ServiceHostMap::getServiceId, serviceId);
        serviceHostMapService.remove(deleteWrapper);

        // 再插入新的映射
        if (hostIds != null && !hostIds.isEmpty()) {
            for (Integer hostId : hostIds) {
                ServiceHostMap map = new ServiceHostMap();
                map.setServiceId(serviceId);
                map.setHostId(hostId.longValue());
                serviceHostMapService.save(map);
            }
        }

        return Result.success(true);
    }

    // 查询某个逻辑服务绑定的所有物理主机
    @GetMapping("/{id}/hosts")
    public Result<List<AssetHost>> getServiceHosts(@PathVariable Long id) {
        // 查映射关系
        List<ServiceHostMap> maps = serviceHostMapService.list(
            new LambdaQueryWrapper<ServiceHostMap>().eq(ServiceHostMap::getServiceId, id)
        );
        if (maps.isEmpty()) {
            return Result.success(List.of());
        }
        // 查物理主机
        List<Long> hostIds = maps.stream().map(ServiceHostMap::getHostId).collect(Collectors.toList());
        List<AssetHost> hosts = assetHostMapper.selectBatchIds(hostIds);
        return Result.success(hosts);
    }

    // 删除服务
    @DeleteMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        // 删除服务本身
        boolean success = assetServiceService.removeById(id);
        if (success) {
            // 删除关联的主机映射
            LambdaQueryWrapper<ServiceHostMap> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(ServiceHostMap::getServiceId, id);
            serviceHostMapService.remove(deleteWrapper);
        }
        return success ? Result.success(true) : Result.error("删除失败");
    }
}
