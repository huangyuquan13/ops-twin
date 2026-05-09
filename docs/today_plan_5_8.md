# 智维方舟 (Ops-Twin) 日常开发计划 (5-8)

## 当前进度概览
✅ **Stage 3 启动成功**：我们已经建立了演练预案的基础设施，包括 `task_plan` 和 `task_record` 数据库表，完善的后端 CRUD 接口，以及前端“预案方案库”管理页面（`tasks/strategy.vue`）。现在，我们已经拥有了“剧本”，明天我们将编写“放映机”。

---

## 今日核心任务：Stage 3 攻坚 - 自动化演练引擎与实时终端

**目标**: 实现从点击“执行”到后端解析步骤、驱动模拟逻辑、并通过 WebSocket 实时将日志推送到前端 Xterm 终端的完整闭环。

**具体工作**:

### 任务一：后端演练调度引擎 (Execution Engine)
- **需求**: 实现预案的异步触发与模拟执行逻辑。
- **细节**:
  - 开发 `POST /api/task/record/trigger/{planId}` 接口。
  - **异步处理**: 使用 `@Async` 或线程池触发任务，立即返回 `recordId` 给前端。
  - **逻辑模拟**: 解析 `steps_json`，遍历步骤。针对每个动作（如 `STOP_NODE`），模拟执行耗时（`Thread.sleep`），并实时生成执行日志。
  - **状态更新**: 任务开始、每步完成及最终结束时，实时更新 `task_record` 的状态和耗时。

### 任务二：WebSocket 实时日志推送 (Real-time Pipeline)
- **需求**: 建立前后端双向通信，实现“大厂风”的日志滚动效果。
- **细节**:
  - 在 Spring Boot 中集成 `spring-boot-starter-websocket`。
  - 配置 `WebSocketHandler`，监听 `/ws/task/log/{recordId}`。
  - 在引擎执行每一步时，将日志字符串通过 WebSocket 发送到对应的客户端。

### 任务三：前端实时终端与执行联动 (Frontend Terminal)
- **需求**: 激活 `tasks/terminal.vue`，实现酷炫的日志展示。
- **细节**:
  - **页面激活**: 改造 `tasks/terminal.vue`，使用类似 Xterm.js 的黑底绿字风格。
  - **执行联动**: 在 `tasks/strategy.vue` 中点击“执行”后，调用触发接口，获取 `recordId` 并自动跳转至终端页面。
  - **实时订阅**: 进入终端页后自动建立 WebSocket 连接，接收并滚动显示日志流。
  - **可视化反馈**: 增加进度条或步骤指示器，实时反馈当前正在执行哪一步。

### 任务四：演练编排工作站 (Workflow Designer) ✅ 已完成
- **定位**: `workflow.vue` 是 `steps_json` 的**可视化编辑器**，替代手写 JSON。
- **完整流程**:
  1. 在“预案方案库”点击“编排”按鈗 → 跳转至此页
  2. 从左侧动作库拖拽节点（停止节点/健康检查等）到中间画布
  3. 点击节点 → 右侧抄屉配置 `target`（主机名）和 `waitMs`（模拟耗时）
  4. 画布节点实时显示 `动作名 → 目标节点`，一目了然
  5. 点击“保存编排” → 按 X 坐标排序 → 序列化为 JSON → 写回 `task_plan.steps_json`
  6. 回到方案库点“执行” → 后端引擎按保存的步骤顺序真实执行
- **技术亮点**:
  - 数据双向流：JSON → 可视化节点（回显），可视化节点 → JSON（保存）
  - `@node-click` 事件驱动配置抄屉
  - 节点 label 随参数输入实时同步更新（`syncNodeLabel`）

---

## 成功标准 (DoD)
1. 在“预案方案库”点击执行“数据库主从切换测试”。
2. 页面自动跳转至“实时监控终端”。
3. 终端内实时滚动出类似：`[INFO] 正在停止节点 Pay-DB-Master...` 的日志。
4. 任务结束后，终端显示 `[SUCCESS] 演练任务执行成功，耗时 5.2s`，且数据库 `task_record` 状态同步更新。

---

## 执行准则
- **追溯性**: 修改代码后，同步核对 `01_PRD`、`02_DB`、`03_API` 确保描述一致。
- **容错性**: 考虑 WebSocket 连接断开后的自动重连或错误提示。
- **极客风**: 终端界面必须够酷，保持数字孪生的高级感。

---

## 5/9 追加优化 (Claude Code 完成)

### 终止命令功能
- **后端**: `TaskExecutionEngine` 新增 `ConcurrentHashMap` 取消标志，`POST /api/task/record/{id}/terminate` 接口
- **前端**: `terminal.vue` 新增红色"终止命令"按钮，ElMessageBox 确认弹窗，CANCELLED 状态徽章
- **同时修复**: 错误检测条件放宽（`[ERROR]` 不再要求同时包含"失败"），空 recordId 不显示终止按钮

### 引擎校验收紧
- `stepsJson` 为空/无真实步骤 → FAILED（删除了硬编码 demo 模式）
- 关联服务不存在 → FAILED
- 关联服务 0 台主机 → FAILED
- 步骤 target 为空 → FAILED（WAIT/NOTIFY 除外）

### 服务-预案-工作流联动
- `AssetService` 新增 `hostCount` 字段，`/api/asset/service/list` 返回主机数
- `GET /api/asset/service/{id}/hosts` 查询服务绑定的物理主机列表
- `strategy.vue` 服务下拉框显示 "订单处理引擎 (0台)" 格式
- `workflow.vue` Target 从文本输入改为 `el-select` 下拉选择（来源：关联服务的主机列表）
- `strategy.vue` → `workflow.vue` 路由跳转新增 `serviceId` 参数传递

### Workflow 主机类型标签 + 任务总览重构
- `workflow.vue` Target 下拉框新增 el-tag 显示主机类型（颜色对齐 service.vue）
- `tasks/index.vue` 从占位页改为任务总览仪表盘：
  - 顶部 KPI 统计卡片（预案总数/今日执行/成功率，调 task_record 接口计算）
  - 预案列表 + 快捷「编排」「执行」按钮（附带 loading 防重复点击）
  - 内嵌新建/编辑预案弹窗
- **路由结构调整**:
  - 新增 `tasks/index` 路由
  - 删除 `tasks/workflow` 和 `tasks/terminal` 路由（不可从侧边栏直接访问）
  - 侧边栏任务中心只保留「任务总览」「预案方案库」两个入口
- **参数守卫**: workflow.vue 无 planId → `router.replace('/tasks/index')`；terminal.vue 无 recordId → 同样跳回
- **返回按钮**: workflow 保存后 → index；terminal「返回任务总览」→ index
- **文档同步**: implementation_plan.md 新增 Stage 3.5；03_API 新增 `/api/asset/service/{id}/hosts`
