# Spec: 演练执行 UX 完善 v2

Date: 2026-05-14 | Status: APPROVED

---

## Requirements

### 1. DRILL 执行后禁用预案（同 FAILOVER/SCALE）
- `TaskExecutionEngine.execute()` 中，去掉 `if (!isDrill)` 条件
- DRILL 执行完后 `status=0`，按钮变为"重置"
- 用户需手动点击"重置"调 `POST /api/task/plan/reset/{planId}` 恢复

### 2. 多机柜 L2 同框显示
- `flyToTargetCabinets` 收集所有受影响机柜，计算包围盒中心
- 自动算距离（2机柜≈8-10, 3机柜≈12-14），保证同框可见
- 隐藏无关机柜，不轮播

### 3. CSS2D 主机名标签
- 演练开始时给所有涉事主机刀片挂标签
- 演练结束/重置时清除

### 4. 演练结束不跳 L1
- `DRILL_REVERT` 只清标签 + 恢复机柜可见 + 隐藏执行标签
- 镜头保持在当前 L2

### 5. 终端日志持久化
- `terminal.vue` onUnmounted 写入 sessionStorage
- onMounted 恢复

### 6. 安全门：执行中拦截
- 后端 `POST /api/task/record/trigger/{planId}` 检查是否有 RUNNING/PENDING 的 task_record
- 有 → 返回 code=500, message="有任务正在执行中，请等待完成"
- 前端收到 → 弹窗提示"有任务正在执行，请等待完成"，当前任务不受影响

### 7. 安全门：执行完确认清屏
- 前端在调用 trigger 前，检查终端是否有残留日志（`logs.length > 0` 且任务已结束）
- 有 → 弹窗"终端尚有上次记录，确认将清屏并执行？"
- 取消 → 留在终端；确认 → 清屏 + 执行新任务

---

## Files Changed

| File | Change |
|------|--------|
| `TaskExecutionEngine.java` | 去掉 `if (!isDrill)` — DRILL 也禁用预案 |
| `TaskRecordController.java` | trigger 接口加 RUNNING/PENDING 检查 |
| `dashboard/index.vue` | 多机柜包围盒 L2 + CSS2D 标签 + 不跳 L1 |
| `tasks/terminal.vue` | sessionStorage 日志持久化 |
| `tasks/strategy.vue` | 执行前安全门弹窗逻辑 |

---

## Verification

1. DRILL 执行完预案状态变为禁用，按钮显示"重置"
2. 两个机柜同框 L2 显示，无关机柜隐藏
3. 主机名标签显示在刀片旁
4. 执行完镜头停在 L2
5. 切页再回终端，日志不丢
6. 执行中点另一个执行 → 弹窗拦截
7. 执行完点另一个执行 → 弹窗确认清屏
