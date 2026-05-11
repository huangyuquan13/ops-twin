package com.ops.twin.controller;

import com.ops.twin.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ops.twin.entity.AssetHost;
import com.ops.twin.mapper.AssetHostMapper;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    @Autowired
    private AssetHostMapper assetHostMapper;

    // 模拟获取服务器 CPU 7 天的历史趋势
    @GetMapping("/cpu-trend")
    public Result<Map<String, Object>> getCpuTrend() {
        Map<String, Object> data = new HashMap<>();
        List<String> days = Arrays.asList("周一", "周二", "周三", "周四", "周五", "周六", "周日");
        List<Integer> cpuUsage = Arrays.asList(45, 52, 38, 85, 60, 22, 30);
        data.put("xAxis", days);
        data.put("seriesData", cpuUsage);
        return Result.success(data);
    }

    // 从数据库获取真实的各机柜资产分布
    @GetMapping("/asset-distribution")
    public Result<List<Map<String, Object>>> getAssetDistribution() {
        // 从数据库查出所有资产
        List<AssetHost> allHosts = assetHostMapper.selectList(null);
        
        // 按机柜ID (cabinetId) 进行分组统计
        Map<String, Long> countByCabinet = allHosts.stream()
                .collect(Collectors.groupingBy(
                        h -> h.getCabinetId() != null ? h.getCabinetId() : "未知机柜",
                        Collectors.counting()
                ));

        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<String, Long> entry : countByCabinet.entrySet()) {
            list.add(createPieItem(entry.getKey(), entry.getValue().intValue()));
        }
        
        return Result.success(list);
    }

    // 从数据库获取核心 KPI 数据
    @GetMapping("/kpi-stats")
    public Result<Map<String, Object>> getKpiStats() {
        Map<String, Object> map = new HashMap<>();
        
        // 【真实数据】总资产数和告警数
        List<AssetHost> allHosts = assetHostMapper.selectList(null);
        long totalAssets = allHosts.size();
        long alertCount = allHosts.stream().filter(h -> h.getStatus() != null && h.getStatus() == 2).count();

        // 【真实数据】汇总物理服务器的总 CPU 核数和总内存
        long totalCores = allHosts.stream().mapToLong(h -> h.getCpuCores() != null ? h.getCpuCores() : 0).sum();
        long totalMemory = allHosts.stream().mapToLong(h -> h.getMemoryGb() != null ? h.getMemoryGb() : 0).sum();

        map.put("totalAssets", totalAssets); 
        map.put("alertCount", alertCount);   
        map.put("cpuTotal", totalCores + " 核");  
        map.put("memTotal", totalMemory + " GB");  
        
        return Result.success(map);
    }

    // 模拟获取网络流量趋势 (柱状图数据)
    @GetMapping("/network-traffic")
    public Result<Map<String, Object>> getNetworkTraffic() {
        Map<String, Object> data = new HashMap<>();
        List<String> days = Arrays.asList("周一", "周二", "周三", "周四", "周五", "周六", "周日");
        List<Integer> inTraffic = Arrays.asList(120, 132, 101, 134, 90, 230, 210); 
        List<Integer> outTraffic = Arrays.asList(220, 182, 191, 234, 290, 330, 310); 

        data.put("xAxis", days);
        data.put("inData", inTraffic);
        data.put("outData", outTraffic);
        
        return Result.success(data);
    }

    private Map<String, Object> createPieItem(String name, int value) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("value", value);
        return map;
    }
}
