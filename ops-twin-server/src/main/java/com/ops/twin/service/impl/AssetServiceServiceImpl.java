package com.ops.twin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ops.twin.entity.AssetService;
import com.ops.twin.mapper.AssetServiceMapper;
import com.ops.twin.service.AssetServiceService;
import org.springframework.stereotype.Service;

@Service
public class AssetServiceServiceImpl extends ServiceImpl<AssetServiceMapper, AssetService> implements AssetServiceService {
}
