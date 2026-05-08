package com.ops.twin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ops.twin.entity.ServiceHostMap;
import com.ops.twin.mapper.ServiceHostMapMapper;
import com.ops.twin.service.ServiceHostMapService;
import org.springframework.stereotype.Service;

@Service
public class ServiceHostMapServiceImpl extends ServiceImpl<ServiceHostMapMapper, ServiceHostMap> implements ServiceHostMapService {
}
