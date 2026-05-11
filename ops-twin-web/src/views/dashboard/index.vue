<template>
  <div class="dashboard-container">
    <!-- 3D 渲染容器 -->
    <div ref="threeContainer" class="three-canvas-wrapper"></div>

    <!-- 顶部状态看板 -->
    <div class="overlay-stats">
      <div class="stat-item">
        <div class="label">资产总数</div>
        <div class="value">{{ total }}</div>
        <!-- //计数器 用于顶部状态栏 -->
      </div>
      <div class="stat-item">
        <div class="label">渲染引擎</div>
        <div class="value">Three.js L3 穿透版</div>
      </div>
      <!-- 热力图模式切换 -->
      <div class="stat-item clickable" @click="toggleThermalMode">
        <div class="label">热力图模式</div>
        <div
          class="value"
          :style="{ color: isThermalMode ? '#ff4d4f' : '#409eff' }"
        >
          {{ isThermalMode ? "ON" : "OFF" }}
        </div>
      </div>
    </div>

    <!-- 沉浸式视角控制 -->
    <div class="view-controls">
      <el-button
        type="primary"
        size="large"
        @click="resetPerspective"
        round
        shadow
      >
        重置视角
      </el-button>
      <Transition name="fade">
        <el-button
          v-if="currentView !== 'L1'"
          type="warning"
          size="large"
          @click="goBack"
          round
          shadow
          style="margin-left: 15px"
        >
          {{ currentView === "L3" ? "返回 L2 机柜视图" : "返回 L1 机房全景" }}
        </el-button>
      </Transition>
    </div>

    <!-- L2 视图: 机柜详情卡片 -->
    <Transition name="fade">
      <div v-if="selectedCabinet && currentView === 'L2'" class="info-card">
        <div class="card-header">
          <h3>机柜编号: {{ selectedCabinet.cabinetId }}</h3>
          <el-tag type="info" size="small">
            {{ selectedCabinet.hostCount }} 台物理服务器
          </el-tag>
        </div>
        <p>
          <span>机柜位置:</span> [X: {{ selectedCabinet.posX }}, Z:
          {{ selectedCabinet.posZ }}]
        </p>
        <div class="divider"></div>
        <p>
          提示：现在您可以直接点击机柜内部发光的刀片服务器，进入 L3
          硬件剖解层级！
        </p>
      </div>
    </Transition>

    <!-- L3 视图: 专属全息数字标牌 (固定看板版，解决显示不全问题) -->
    <Transition name="fade">
      <div v-if="currentView === 'L3' && activeBladeData" class="hologram-card">
        <div class="holo-header">
          <i class="el-icon-cpu"></i> 硬件数字标牌 (L3)
        </div>
        <div class="holo-body">
          <div class="data-row">
            <span class="label">主机名</span>
            <span class="value" style="color: #fff">{{
              activeBladeData?.parentHost?.hostname
            }}</span>
          </div>
          <div class="data-row">
            <span class="label">物理插槽 (U位)</span>
            <span class="value val-blue"
              >{{
                activeBladeData?.slotIndex !== undefined
                  ? activeBladeData.slotIndex + 1
                  : ""
              }}U</span
            >
          </div>
          <div class="data-row">
            <span class="label">实时 CPU 负载</span>
            <span
              class="value"
              :class="
                (activeBladeData?.usage || 0) > 80 ? 'val-red' : 'val-green'
              "
            >
              {{ activeBladeData?.usage || 0 }}%
            </span>
          </div>
          <div class="data-row">
            <span class="label">风扇转速 (RPM)</span>
            <span class="value val-yellow"
              >{{ 3000 + (activeBladeData?.usage || 0) * 20 }} RPM</span
            >
          </div>
          <div class="data-row">
            <span class="label">磁盘 I/O (MB/s)</span>
            <span class="value val-blue"
              >{{ ((activeBladeData?.usage || 0) * 1.5).toFixed(1) }} MB/s</span
            >
          </div>
        </div>
        <!-- 模拟风扇旋转动画 -->
        <div
          class="fan-animation"
          :style="{
            animationDuration: `${100 / ((activeBladeData?.usage || 0) + 10)}s`,
          }"
        >
          <div class="fan-blade"></div>
          <div class="fan-blade" style="transform: rotate(120deg)"></div>
          <div class="fan-blade" style="transform: rotate(240deg)"></div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from "vue";
import * as THREE from "three";
import { OrbitControls } from "three/examples/jsm/controls/OrbitControls.js";
import {
  CSS2DRenderer,
  CSS2DObject,
} from "three/examples/jsm/renderers/CSS2DRenderer.js";
import * as TWEEN from "@tweenjs/tween.js";
import request from "@/api/request";

// ==========================================
// 响应式变量
// ==========================================
const threeContainer = ref<HTMLElement | null>(null); //钩子 拿到html元素进行threejs挂载
const hostList = ref<any[]>([]); //机子数据
const cabinetList = ref<any[]>([]); //机柜数据
const total = ref(0); //计数器
const selectedCabinet = ref<any>(null); // 被选中的聚合机柜数据
const isThermalMode = ref(false);
const currentView = ref("L1"); // 状态机: L1(全景) -> L2(机柜) -> L3(硬件节点)
const activeBladeData = ref<any>(null); // L3状态下的刀片节点数据

// Three.js 核心对象
let scene: THREE.Scene;
let camera: THREE.PerspectiveCamera;
let renderer: THREE.WebGLRenderer;
let labelRenderer: CSS2DRenderer;
let controls: OrbitControls;
let frameId: number;
let activeCabinet: THREE.Group | null = null;

// 视角记忆：记录用户从 L1 飞往 L2 之前，停留在 L1 的相机位置和焦点
const l1CameraState = {
  position: new THREE.Vector3(),
  target: new THREE.Vector3(),
};

// ==========================================
// 1. 初始化 3D 环境
// ==========================================
const initThree = () => {
  if (!threeContainer.value) return;

  scene = new THREE.Scene();
  scene.background = new THREE.Color("#020508");
  scene.fog = new THREE.Fog("#020508", 20, 150);

  camera = new THREE.PerspectiveCamera(
    75,
    threeContainer.value.clientWidth / threeContainer.value.clientHeight,
    0.1,
    1000,
  );
  camera.position.set(15, 12, 20);

  renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
  renderer.setSize(
    threeContainer.value.clientWidth,
    threeContainer.value.clientHeight,
  );
  renderer.setPixelRatio(window.devicePixelRatio);
  threeContainer.value.appendChild(renderer.domElement); //THREEjs挂载到HTML页面上

  labelRenderer = new CSS2DRenderer();
  labelRenderer.setSize(
    threeContainer.value.clientWidth,
    threeContainer.value.clientHeight,
  );
  labelRenderer.domElement.style.position = "absolute";
  labelRenderer.domElement.style.top = "0px";
  labelRenderer.domElement.style.pointerEvents = "none";
  threeContainer.value.appendChild(labelRenderer.domElement);

  controls = new OrbitControls(camera, renderer.domElement);
  controls.enableDamping = true; //随时更新控制器状态

  const ambientLight = new THREE.AmbientLight(0xffffff, 0.4);
  scene.add(ambientLight);
  const pointLight = new THREE.PointLight(0x409eff, 1, 100);
  pointLight.position.set(10, 20, 10);
  scene.add(pointLight);

  const gridHelper = new THREE.GridHelper(100, 40, 0x00e5ff, 0x05101a);
  gridHelper.position.y = -0.1;
  scene.add(gridHelper);

  animate();
};

const animate = () => {
  frameId = requestAnimationFrame(animate);
  controls.update();
  TWEEN.update();

  renderer.render(scene, camera);
  labelRenderer.render(scene, camera);
};

// 【动态视角算法】根据机柜高度计算相机的最佳距离
const getL2Distance = (maxU: number = 8) => {
  // 保持 8U 基准距离为 3.2 米左右
  // 对于高出的 U 位，采用更激进的拉远策略 (每 U 位额外后退 0.4 米)
  if (maxU <= 8) return 3.2;
  // 每多 1U，相机额外后退 0.3 米 (针对 42U 约后退 10+ 米)
  return 3.2 + (maxU - 8) * 0.3;
};

const fetchAndRenderAssets = async () => {
  try {
    const [cabRes, hostRes] = await Promise.all([
      request.get("/api/asset/cabinet/list/all"),
      request.get("/api/asset/host/list", {
        params: { current: 1, size: 500 },
      }),
    ]);

    if ((cabRes as any).code === 200) {
      cabinetList.value = (cabRes as any).data;
    }
    if ((hostRes as any).code === 200) {
      hostList.value = (hostRes as any).data.records;
      total.value = (hostRes as any).data.total;
    }
    renderHostModels();
  } catch (error) {
    console.error("获取资产失败:", error);
  }
};

// ==========================================
// 4. 高精机柜建模 (全息 X 光视界版)
// ==========================================
const createCabinetModel = (
  cabinetId: string,
  posX: number,
  posZ: number,
  hosts: any[],
  maxUParam: number = 42,
) => {
  const group = new THREE.Group();

  // 【参数化建模】基于机柜的真实 maxU
  const maxU = maxUParam;
  const slotHeight = 0.26; // 每个 U 位的高度间隔
  const cabinetHeight = maxU * slotHeight + 0.4; // 总高度

  // 1. 全息科技线框外壳 (高度动态化)
  const bodyGeom = new THREE.BoxGeometry(1.2, cabinetHeight, 1.2);
  const glassMat = new THREE.MeshBasicMaterial({
    color: "#00e5ff",
    transparent: true,
    opacity: 0.03,
    depthWrite: false,
  });
  const glassBody = new THREE.Mesh(bodyGeom, glassMat);
  group.add(glassBody);

  const edges = new THREE.EdgesGeometry(bodyGeom);
  const lineMat = new THREE.LineBasicMaterial({
    color: 0x00e5ff,
    transparent: true,
    opacity: 0.5,
  });
  const line = new THREE.LineSegments(edges, lineMat);
  group.add(line);

  // 2. 内部刀片服务器 (循环上限动态化)
  const servers = new THREE.Group();
  const startY = -(cabinetHeight / 2) + 0.3; // 从底部向上排列的起始点

  for (let i = 0; i < maxU; i++) {
    const host = hosts.find((h) => h.rackPos === i + 1 || h.rack_pos === i + 1);

    if (host) {
      const serverGeom = new THREE.BoxGeometry(1.0, 0.15, 1.0);
      const usage = host.cpuCores
        ? (host.cpuCores * 13) % 100
        : ((host.id || 1) * (i + 1) * 7) % 100;

      let baseColor = "#52c41a";
      if (host.status === 2) baseColor = "#faad14";
      else if (host.status === 0 || host.status === 3) baseColor = "#ff4d4f";

      const serverMat = new THREE.MeshPhongMaterial({ color: "#1e293b" });
      const server = new THREE.Mesh(serverGeom, serverMat);
      server.position.set(0, startY + i * slotHeight, 0);

      const ledGeom = new THREE.BoxGeometry(0.1, 0.02, 0.01);
      const ledMat = new THREE.MeshBasicMaterial({ color: baseColor });
      const led = new THREE.Mesh(ledGeom, ledMat);
      led.position.set(0.4, 0, 0.51);
      server.add(led);

      server.name = "bladeServer";
      server.userData = {
        usage,
        ledMat,
        baseColor,
        slotIndex: i,
        parentHost: host,
      };
      servers.add(server);
    } else {
      const emptyGeom = new THREE.BoxGeometry(1.0, 0.15, 1.0);
      const emptyMat = new THREE.MeshPhongMaterial({
        color: "#0a1118",
        transparent: true,
        opacity: 0.5,
      });
      const emptySlot = new THREE.Mesh(emptyGeom, emptyMat);
      emptySlot.position.set(0, startY + i * slotHeight, 0);
      servers.add(emptySlot);
    }
  }
  group.add(servers);

  // CSS2D 标签 (位置也随高度动态调整)
  const labelDiv = document.createElement("div");
  labelDiv.className = "host-label";
  labelDiv.textContent = cabinetId;
  const label = new CSS2DObject(labelDiv);
  label.position.set(0, cabinetHeight / 2 + 0.3, 0);
  group.add(label);

  group.name = "hostModel";
  group.userData = {
    cabinetId,
    hostCount: hosts.length,
    posX,
    posZ,
    maxU, // 【关键修复】存入 U 位高度，供点击时计算视距
    servers,
    glassBody,
    line,
  };
  // 机柜底座贴地，中心点 Y 值为高度的一半
  group.position.set(posX, cabinetHeight / 2, posZ);

  return group;
};

const renderHostModels = () => {
  const oldModels = scene.children.filter(
    (child) => child.name === "hostModel",
  );
  oldModels.forEach((model) => scene.remove(model));

  // 遍历真实的机柜列表进行 3D 渲染
  cabinetList.value.forEach((cab) => {
    // 过滤出属于当前机柜的主机
    const hostsInCabinet = hostList.value.filter(
      (h) => (h.cabinetId || h.cabinet_id) === cab.cabinetId,
    );
    const cabinetModel = createCabinetModel(
      cab.cabinetId,
      cab.posX || 0,
      cab.posZ || 0,
      hostsInCabinet,
      cab.maxU || 42,
    );
    scene.add(cabinetModel);
  });
};

// ==========================================
// 5. 交互核心：L1 -> L2 -> L3 层层穿透
// ==========================================
let pointerDownPos = { x: 0, y: 0 };
const onPointerDown = (event: MouseEvent) => {
  pointerDownPos.x = event.clientX;
  pointerDownPos.y = event.clientY;
};

const onCanvasClick = (event: MouseEvent) => {
  if (!threeContainer.value) return;

  // 【极其关键】区分“拖拽旋转”与“精确点击”
  // 如果鼠标按下和松开的位移超过 3 个像素，说明用户在拖拽视角，直接忽略点击！
  if (
    Math.abs(event.clientX - pointerDownPos.x) > 3 ||
    Math.abs(event.clientY - pointerDownPos.y) > 3
  ) {
    return;
  }

  const rect = renderer.domElement.getBoundingClientRect();
  const x = ((event.clientX - rect.left) / rect.width) * 2 - 1; // //计算鼠标位置归一化坐标
  const y = -((event.clientY - rect.top) / rect.height) * 2 + 1;
  const raycaster = new THREE.Raycaster();
  // 【极其关键】因为机柜是线框 (LineSegments)，Three.js 默认线框点击阈值非常大 (1)
  // 导致鼠标在两个机柜中间时，极其容易误判点到后面的机柜！降低阈值提高精准度。
  raycaster.params.Line.threshold = 0.1;
  raycaster.setFromCamera(new THREE.Vector2(x, y), camera);

  // 获取所有相交的物体
  const intersects = raycaster.intersectObjects(scene.children, true);
  if (intersects.length === 0) return;

  // =====================================
  // L2 -> L3 的点击检测 (极强穿透版)
  // =====================================
  if (currentView.value === "L2") {
    // 遍历所有被射线贯穿的物体（无视遮挡物、透明罩或网格）
    for (let i = 0; i < intersects.length; i++) {
      let current: any = intersects[i].object;

      // 向上追溯，确认这个物体属于刀片服务器
      while (current && current.name !== "bladeServer") {
        current = current.parent;
      }

      if (current) {
        currentView.value = "L3";
        activeBladeData.value = current.userData;

        return;
      }
    }
  }

  // =====================================
  // L1 -> L2 的点击检测
  // =====================================
  if (currentView.value === "L1") {
    for (let i = 0; i < intersects.length; i++) {
      let current: any = intersects[i].object;
      while (current && current.name !== "hostModel") {
        current = current.parent;
      }

      if (current) {
        currentView.value = "L2";
        activeCabinet = current;
        selectedCabinet.value = current.userData;

        // 【核心】：记住当前 L1 的视角状态，以便“返回 L1”时精准还原
        l1CameraState.position.copy(camera.position);
        l1CameraState.target.copy(controls.target);

        // 彻底隐藏其他机柜，防止堆叠遮挡
        scene.children.forEach((child) => {
          if (child.name === "hostModel" && child !== current) {
            child.visible = false;
          }
        });

        // 动态计算相机拉远距离，42层机柜需要比8层机柜退后得更远
        const distance = getL2Distance(current.userData.maxU || 8);

        // 瞬间将相机传送到机柜前方，去掉多余的 y 轴偏移
        camera.position.set(
          current.position.x,
          current.position.y,
          current.position.z + distance,
        );
        controls.target.set(
          current.position.x,
          current.position.y,
          current.position.z,
        );
        controls.update();

        return;
      }
    }
  }
};

// ==========================================
// 6. 交互核心：视角逐级返回 (L3 -> L2 -> L1)
// ==========================================
const goBack = () => {
  if (currentView.value === "L3") {
    // 从 L3 退回到 L2
    currentView.value = "L2";
    activeBladeData.value = null;
    return;
  }

  if (currentView.value === "L2") {
    // 从 L2 退回到 L1
    currentView.value = "L1";
    selectedCabinet.value = null;

    // 恢复所有机柜的显示
    scene.children.forEach((child) => {
      if (child.name === "hostModel") {
        child.visible = true;
      }
    });

    // 精准还原进入 L2 前的相机视角
    camera.position.copy(l1CameraState.position);
    controls.target.copy(l1CameraState.target);
    controls.update();

    if (activeCabinet) {
      activeCabinet = null;
    }
  }
};

// ==========================================
// 7. 工具功能：重置当前层级的视角
// ==========================================
const resetPerspective = () => {
  if (currentView.value === "L1") {
    // 重置 L1 全局视角
    camera.position.set(15, 12, 20);
    controls.target.set(0, 0, 0);
    controls.update();
  } else if (
    (currentView.value === "L2" || currentView.value === "L3") &&
    activeCabinet
  ) {
    // 重置 L2/L3 怼脸视角 (动态计算距离)
    const distance = getL2Distance(activeCabinet.userData.maxU || 8);
    camera.position.set(
      activeCabinet.position.x,
      activeCabinet.position.y,
      activeCabinet.position.z + distance,
    );
    controls.target.set(
      activeCabinet.position.x,
      activeCabinet.position.y,
      activeCabinet.position.z,
    );
    controls.update();
  }
};

const toggleThermalMode = () => {
  isThermalMode.value = !isThermalMode.value;
  scene.traverse((obj: any) => {
    if (obj.name === "hostModel") {
      const servers = obj.userData.servers.children;
      servers.forEach((server: any) => {
        // 【关键修复】只渲染真实插槽机器，跳过盲板
        if (server.name !== "bladeServer") return;

        const { usage, ledMat, baseColor } = server.userData;
        if (isThermalMode.value) {
          const heatColor = new THREE.Color().setHSL(
            (100 - usage) / 360,
            1,
            0.5,
          );
          ledMat.color.copy(heatColor);
          server.material.color.set(heatColor);
          server.material.emissive = heatColor;
        } else {
          ledMat.color.set(baseColor);
          server.material.color.set("#1e293b"); // 恢复刀片原始深灰色
          server.material.emissive.set("#000");
        }
      });
    }
  });
};

const handleResize = () => {
  //监听窗口变化 确保画布不拉伸
  if (!threeContainer.value) return;
  camera.aspect =
    threeContainer.value.clientWidth / threeContainer.value.clientHeight;
  camera.updateProjectionMatrix();
  renderer.setSize(
    threeContainer.value.clientWidth,
    threeContainer.value.clientHeight,
  );
  labelRenderer.setSize(
    threeContainer.value.clientWidth,
    threeContainer.value.clientHeight,
  );
};

onMounted(() => {
  initThree();
  fetchAndRenderAssets();
  // 绑定 pointerdown 和 click，防止拖拽视角的误触
  renderer.domElement.addEventListener("pointerdown", onPointerDown);
  renderer.domElement.addEventListener("click", onCanvasClick);
  window.addEventListener("resize", handleResize);
});

onUnmounted(() => {
  cancelAnimationFrame(frameId);
  renderer.domElement.removeEventListener("pointerdown", onPointerDown);
  renderer.domElement.removeEventListener("click", onCanvasClick);
  window.removeEventListener("resize", handleResize);
  TWEEN.removeAll();
});
</script>

<style scoped>
.dashboard-container {
  position: relative;
  width: 100%;
  height: calc(100vh - 80px);
  background: #020508;
  overflow: hidden;
}
.three-canvas-wrapper {
  width: 100%;
  height: 100%;
}

.overlay-stats {
  position: absolute;
  top: 20px;
  left: 20px;
  display: flex;
  gap: 15px;
  z-index: 10;
}
.stat-item {
  background: rgba(5, 12, 20, 0.85);
  border: 1px solid rgba(0, 229, 255, 0.2);
  padding: 12px 20px;
  border-radius: 4px;
  backdrop-filter: blur(8px);
}
.stat-item.clickable {
  cursor: pointer;
  transition: all 0.3s;
}
.stat-item.clickable:hover {
  border-color: #00e5ff;
  background: rgba(0, 229, 255, 0.1);
}
.stat-item .label {
  color: #666;
  font-size: 11px;
  text-transform: uppercase;
  margin-bottom: 4px;
}
.stat-item .value {
  color: #00e5ff;
  font-size: 20px;
  font-weight: bold;
  font-family: "JetBrains Mono";
}

.view-controls {
  position: absolute;
  bottom: 40px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 20;
}

.info-card {
  position: absolute;
  top: 100px;
  right: 20px;
  width: 320px;
  background: rgba(5, 12, 20, 0.9);
  border: 1px solid #00e5ff;
  padding: 24px;
  border-radius: 2px;
  color: #fff;
  z-index: 100;
  box-shadow: 0 0 40px rgba(0, 229, 255, 0.15);
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.card-header h3 {
  margin: 0;
  font-size: 20px;
  color: #00e5ff;
}
.info-card p {
  margin: 12px 0;
  font-size: 13px;
  line-height: 1.6;
}
.info-card p span {
  color: #666;
  width: 80px;
  display: inline-block;
}
.divider {
  height: 1px;
  background: rgba(0, 229, 255, 0.1);
  margin: 20px 0;
}

/* ======== L3 全息数字标牌样式 ======== */
.hologram-card {
  position: absolute;
  top: 100px;
  right: 20px;
  width: 340px;
  background: rgba(5, 12, 20, 0.95);
  border: 1px solid #00e5ff;
  border-radius: 2px;
  padding: 24px;
  color: #fff;
  z-index: 100;
  box-shadow: 0 0 40px rgba(0, 229, 255, 0.2);
  backdrop-filter: blur(10px);
}
.holo-header {
  font-size: 24px;
  font-weight: bold;
  color: #00e5ff;
  border-bottom: 1px solid rgba(0, 229, 255, 0.3);
  padding-bottom: 15px;
  margin-bottom: 20px;
  text-align: center;
  letter-spacing: 2px;
}
.data-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 15px;
  font-size: 16px;
  font-family: "JetBrains Mono";
}
.data-row .label {
  color: #888;
}
.val-green {
  color: #52c41a;
  font-weight: bold;
  text-shadow: 0 0 10px #52c41a;
}
.val-red {
  color: #ff4d4f;
  font-weight: bold;
  text-shadow: 0 0 10px #ff4d4f;
}
.val-yellow {
  color: #faad14;
}
.val-blue {
  color: #409eff;
}

/* CSS 风扇动画特效 */
.fan-animation {
  position: absolute;
  top: 30px;
  right: 30px;
  width: 40px;
  height: 40px;
  animation: spin linear infinite;
  opacity: 0.5;
}
.fan-blade {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 20px;
  height: 4px;
  background: #00e5ff;
  transform-origin: 0 50%;
  border-radius: 2px;
}
@keyframes spin {
  100% {
    transform: rotate(360deg);
  }
}

:deep(.host-label) {
  color: #00e5ff;
  font-size: 12px;
  padding: 4px 10px;
  background: rgba(0, 229, 255, 0.1);
  border: 1px solid rgba(0, 229, 255, 0.5);
  border-radius: 2px;
  white-space: nowrap;
  pointer-events: none;
}
.fade-enter-active,
.fade-leave-active {
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-20px);
}
.zoom-enter-active,
.zoom-leave-active {
  transition: all 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.zoom-enter-from,
.zoom-leave-to {
  opacity: 0;
  transform: scale(0.5);
}
</style>
