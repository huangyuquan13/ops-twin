# 智维方舟 (Ops-Twin) 项目实施计划

## 当前阶段：Stage 3 - 效能分析与 3D 沉浸式联动

### 1. 进度看板
- [x] Stage 1: 基础脚手架与 UI 骨架 (Vue3 + SpringBoot)
- [x] Stage 2: 用户中心与权限基础 (MD5加密、Pinia持久化)
- [x] Stage 3: 效能分析大盘 (ECharts + 真实数据库接入)
- [x] Stage 4: 3D 沉浸式大屏强化 (L2 机架层穿透、L3 标牌渲染)
- [▶] Stage 5: 逻辑服务拓扑 (Vue Flow 拖拽绑定)
- [ ] Stage 6: 自动化演练编排 (任务流引擎)

### 2. 核心技术攻坚 (针对 PRD 2.1 与 2.2)
#### 真实数据源映射 (已完成)
- [x] **物理与 3D 拓扑解耦**：修正 `asset_host` 为真实的刀片服务器，并按 `cabinet_id` 在前端进行机柜级聚合渲染。
- [x] **L3 真实化**：L3 硬件标牌读取正确的插槽 U 位（rack_pos）和 IP 数据。
- [x] **大盘数据打通**：`AnalysisController` 和 `analysis.vue` 完全对接 `asset_host` 真实状态汇总数据。

## 3. 今日目标
1. 编写 SQL 脚本修正 `asset_host` 的假坐标，划分真实的机柜归属（`cabinet_id`）和插槽号（`rack_pos`）。
2. 重构前端 L1/L2 渲染工厂，将同一机柜的多台主机聚合渲染到一个 3D 模型中。
3. 对接大盘图表数据，让一切所见皆为真实的数据库数据。
