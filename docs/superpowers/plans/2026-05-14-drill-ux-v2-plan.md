# 演练执行 UX 完善 v2 — 实施计划

> **For agentic workers:** Use superpowers:executing-plans to implement task-by-task. Steps use checkbox (`- [ ]`) syntax.

**Goal:** DRILL 同 FAILOVER 需手动重置 + 多机柜同框 L2 + CSS2D 标签 + 终端持久化 + 安全门拦截

**Architecture:** 后端去掉 DRILL 自动恢复并统一禁用逻辑，trigger 加并发检查；前端 3D 镜头改为包围盒计算多机柜距离，CSS2DRenderer 挂标签，terminal sessionStorage 读写，strategy 加安全门弹窗。

**Tech Stack:** Java 17 / Vue 3 + Three.js + CSS2DRenderer / Element Plus

---

### Task 1: 后端 — DRILL 统一禁用 + 并发检查

**Files:**
- Modify: `ops-twin-server/src/main/java/com/ops/twin/service/TaskExecutionEngine.java:185-195`
- Modify: `ops-twin-server/src/main/java/com/ops/twin/controller/TaskRecordController.java:43-55`

- [ ] **Step 1: DRILL 也走禁用逻辑**

将这段 (TaskExecutionEngine.java:185-195):
```java
            // DRILL 模式：执行完成后恢复所有主机原状态
            revertDrillChanges(hostRevertMap, rid);

            // FAILOVER / SCALE 执行后禁用预案
            if (!isDrill) {
                TaskPlan disablePlan = new TaskPlan();
                disablePlan.setId(plan.getId());
                disablePlan.setStatus(0);
                planMapper.updateById(disablePlan);
                pushLog(rid, "[INFO] 预案已自动禁用，请通过【重置】恢复");
            }
```

替换为:
```java
            // 所有类型的预案执行完成后统一禁用，需手动点击"重置"恢复
            TaskPlan disablePlan = new TaskPlan();
            disablePlan.setId(plan.getId());
            disablePlan.setStatus(0);
            planMapper.updateById(disablePlan);
            pushLog(rid, "");
            pushLog(rid, "[INFO] ═══════════════════════════════════════════");
            pushLog(rid, "[INFO]  演练完成，预案已禁用。请手动点击【重置】恢复");
            pushLog(rid, "[INFO] ═══════════════════════════════════════════");
            pushLog(rid, "");

            if (isDrill) {
                // DRILL: 保留主机状态（不自动恢复），重置时由 reset 接口恢复
                pushLog(rid, "[INFO] DRILL 模式：主机状态已保留（红/黄不恢复），重置后自动还原");
            }
```

- [ ] **Step 2: 未使用的 DRILL cancel 路径也保持一致**

找到 line 177 附近的 cancel 处理中的 `revertDrillChanges(hostRevertMap, rid)`:
```java
                    revertDrillChanges(hostRevertMap, rid);
                    updateStatus(recordId, "CANCELLED", System.currentTimeMillis() - startMs, "用户手动终止");
```
改为:
```java
                    updateStatus(recordId, "CANCELLED", System.currentTimeMillis() - startMs, "用户手动终止");
                    // 取消时也禁用预案（已部分执行，状态不可靠）
                    TaskPlan disablePlan2 = new TaskPlan();
                    disablePlan2.setId(plan.getId());
                    disablePlan2.setStatus(0);
                    planMapper.updateById(disablePlan2);
```

- [ ] **Step 3: 编译验证**

```bash
cd ops-twin-server && mvn compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 4: trigger 接口加并发检查**

在 TaskRecordController.java 的 `trigger` 方法中，校验预案状态之后加:
```java
        // 1.5 检查是否有正在执行或等待中的任务
        long runningCount = taskRecordService.count(
            new LambdaQueryWrapper<TaskRecord>()
                .in(TaskRecord::getRunStatus, "PENDING", "RUNNING")
        );
        if (runningCount > 0) {
            return Result.error("有任务正在执行中，请等待完成后再触发新任务");
        }
```

需要 import:
```java
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ops.twin.entity.TaskRecord;
```

- [ ] **Step 5: 再次编译验证**

```bash
cd ops-twin-server && mvn compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add ops-twin-server/src/main/java/com/ops/twin/service/TaskExecutionEngine.java ops-twin-server/src/main/java/com/ops/twin/controller/TaskRecordController.java
git commit -m "fix(engine): DRILL 统一禁用+trigger并发检查 — 执行后需手动重置，执行中拦截新任务"
```

---

### Task 2: 前端 3D — 多机柜 L2 + CSS2D 标签 + 不跳 L1

**Files:**
- Modify: `ops-twin-web/src/views/dashboard/index.vue`

- [ ] **Step 1: 替换 flyToTargetCabinets — 多机柜包围盒**

找到 `flyToTargetCabinets` 函数（约 line 331），整体替换为:

```typescript
/** 飞到受影响机柜 — 多机柜自动算包围盒距离，单机柜进L2 */
const flyToTargetCabinets = (cabIds: string[]) => {
  const targets: any[] = [];
  scene.children.forEach((child: any) => {
    if (child.name === 'hostModel') {
      const match = cabIds.includes(child.userData?.cabinetId);
      child.visible = match;
      if (match) targets.push(child);
    }
  });
  if (targets.length === 0) return;

  lastExecCabinetId = cabIds[0];
  l1CameraState.position.copy(camera.position);
  l1CameraState.target.copy(controls.target);

  // 计算包围盒中心
  let cx = 0, cz = 0;
  for (const t of targets) { cx += t.position.x; cz += t.position.z; }
  cx /= targets.length; cz /= targets.length;

  // 自动算距离：单机柜 L2，多机柜框住全部
  let distance: number;
  if (targets.length === 1) {
    distance = getL2Distance(targets[0].userData.maxU || 8);
  } else {
    // 计算包围盒半径
    let maxDist = 0;
    for (const t of targets) {
      const dx = t.position.x - cx;
      const dz = t.position.z - cz;
      maxDist = Math.max(maxDist, Math.sqrt(dx * dx + dz * dz));
    }
    distance = maxDist * 2.0 + 6; // 2倍半径 + 6 基础距离
  }
  currentView.value = 'L2';
  activeCabinet = targets[0];
  selectedCabinet.value = targets[0].userData;

  const from = { x: camera.position.x, y: camera.position.y, z: camera.position.z };
  new TWEEN.Tween(from)
    .to({ x: cx, y: targets[0].position.y, z: cz + distance }, 1200)
    .easing(TWEEN.Easing.Quadratic.InOut)
    .onUpdate(() => {
      camera.position.set(from.x, from.y, from.z);
      controls.target.set(cx, targets[0].position.y, cz);
    })
    .start();
};
```

- [ ] **Step 2: 添加 CSS2D 标签管理**

在 `let lastExecCabinetId` 后加:
```typescript
const drillHostLabels: any[] = [];
```

在 `flyToTargetCabinets` 函数后加:
```typescript
const showDrillHostLabels = (hostnames: string[]) => {
  clearDrillHostLabels();
  scene.children.forEach((child: any) => {
    if (child.name === 'hostModel' && hostnames.includes(child.userData?.hostname)) {
      const div = document.createElement('div');
      div.textContent = child.userData.hostname || '';
      div.style.cssText =
        'color:#00e5ff;font-size:11px;font-family:JetBrains Mono,monospace;' +
        'background:rgba(0,0,0,0.8);padding:2px 6px;border-radius:3px;' +
        'white-space:nowrap;pointer-events:none;';
      const label = new CSS2DObject(div);
      label.position.copy(child.position);
      label.position.y += 1.2;
      label.name = 'drillHostLabel';
      scene.add(label);
      drillHostLabels.push(label);
    }
  });
};

const clearDrillHostLabels = () => {
  drillHostLabels.forEach(l => scene.remove(l));
  drillHostLabels.length = 0;
};
```

- [ ] **Step 3: 演练启动时调标签**

在 `watch(() => routeObj.query, ...)` 中 `flyToTargetCabinets` 后加:
```typescript
showDrillHostLabels(targets);
```

- [ ] **Step 4: HOST_STATUS 事件驱动镜头**

找到 `dashboardWs.onmessage`，在 `updateHostColor` 之前加镜头聚焦:
```typescript
      if (data.type === "HOST_STATUS") {
        const host = hostList.value.find((h: any) => h.id === data.hostId);
        if (host) {
          host.status = data.status;
          if (host.cabinetId && !data.action?.includes('DRILL_REVERT')) {
            flyToTargetCabinets([host.cabinetId]);
          }
        }
        updateHostColor(data.hostId, data.status);
      }
```

- [ ] **Step 5: DRILL_REVERT 不跳 L1**

将 DRILL_REVERT 块改为:
```typescript
      if (data.type === 'HOST_STATUS' && data.action === 'DRILL_REVERT') {
        clearDrillHostLabels();
        scene.children.forEach((child: any) => {
          if (child.name === 'hostModel') child.visible = true;
        });
        hideExecLabel('SUCCESS');
      }
```

- [ ] **Step 6: onUnmounted 清理**

在 `onUnmounted` 开头加:
```typescript
clearDrillHostLabels();
```

- [ ] **Step 7: Commit**

```bash
git add ops-twin-web/src/views/dashboard/index.vue
git commit -m "fix(3d): 多机柜包围盒L2+CSS2D标签+事件驱动镜头+不跳L1"
```

---

### Task 3: 前端 — 终端日志持久化

**Files:**
- Modify: `ops-twin-web/src/views/tasks/terminal.vue`

- [ ] **Step 1: onUnmounted 保存日志**

找到 `onUnmounted`，在关闭 WebSocket 前加:
```typescript
onUnmounted(() => {
  if (logs.value.length > 0) {
    const saved = sessionStorage.getItem('dashboardState');
    const state = saved ? JSON.parse(saved) : {};
    state.miniTerm = { ...(state.miniTerm || {}), logs: logs.value.slice(-200) };
    sessionStorage.setItem('dashboardState', JSON.stringify(state));
  }
  if (ws) ws.close();
  stopProgressSimulation(0);
  stopElapsedTimer();
});
```

- [ ] **Step 2: 确认 onMounted 已有恢复逻辑**

检查 onMounted 中已有:
```typescript
if (hasDashboardState.value) {
  const saved = sessionStorage.getItem('dashboardState');
  // ... parse and restore logs.value = state.miniTerm.logs
}
```
无需修改。

- [ ] **Step 3: Commit**

```bash
git add ops-twin-web/src/views/tasks/terminal.vue
git commit -m "fix(terminal): onUnmounted 持久化日志到 sessionStorage"
```

---

### Task 4: 前端 — 安全门弹窗

**Files:**
- Modify: `ops-twin-web/src/views/tasks/strategy.vue`

- [ ] **Step 1: 在 handleExecute 中加安全门逻辑**

找到 strategy.vue 中的执行触发函数，在调用 `POST /api/task/record/trigger/{planId}` 之前加判断:

```typescript
const handleExecute = async (plan: any) => {
  // 安全门1: 检查是否有残留的终端状态（上次执行完未清理）
  const dashState = sessionStorage.getItem('dashboardState');
  if (dashState) {
    try {
      const state = JSON.parse(dashState);
      if (state.miniTerm?.logs?.length > 0 || state.active) {
        await ElMessageBox.confirm(
          '终端尚有上次执行记录，确认将清屏并执行新任务？',
          '确认执行',
          { confirmButtonText: '确认清屏执行', cancelButtonText: '取消', type: 'warning' }
        );
        sessionStorage.removeItem('dashboardState');
      }
    } catch (_) {}
  }

  // 调用后端 trigger
  try {
    const res: any = await request.post(`/api/task/record/trigger/${plan.id}?operator=admin`);
    if (res.code === 200) {
      router.push(`/tasks/terminal?recordId=${res.data.recordId}&planName=${plan.planName}`);
    } else {
      // 后端返回错误（如：有任务执行中）
      ElMessage.warning(res.message || '触发失败');
    }
  } catch (_) {
    ElMessage.error('触发失败，请检查后端服务');
  }
};
```

注意：需要找到 strategy.vue 中实际的执行函数名和逻辑，以上为模板，具体变量名和导入需匹配现有代码。

- [ ] **Step 2: 确保 ElMessageBox 已导入**

检查 `<script setup>` 中是否有:
```typescript
import { ElMessage, ElMessageBox } from 'element-plus';
```
没有则添加。

- [ ] **Step 3: Commit**

```bash
git add ops-twin-web/src/views/tasks/strategy.vue
git commit -m "feat(ux): 执行安全门 — 执行中拦截+残留日志确认清屏"
```

---

### Task 5: 验证编译

- [ ] **Step 1: 后端编译**

```bash
cd ops-twin-server && mvn compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 2: 前端类型检查**

```bash
cd ops-twin-web && npx vue-tsc --noEmit 2>&1 | head -20
```
Expected: no errors (或仅有已存在的 warning)
