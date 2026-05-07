package com.ops.twin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ops.twin.entity.AssetCabinet;
import com.ops.twin.mapper.AssetCabinetMapper;
import com.ops.twin.service.AssetCabinetService;
import org.springframework.stereotype.Service;

@Service
public class AssetCabinetServiceImpl extends ServiceImpl<AssetCabinetMapper, AssetCabinet> implements AssetCabinetService {
}
