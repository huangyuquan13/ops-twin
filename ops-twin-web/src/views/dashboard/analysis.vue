<template>
  <div class="analysis-container">
    <div class="header">
      <h2>效能大盘分析</h2>
      <p class="subtitle">实时监控核心集群的算力分布与性能衰减趋势</p>
    </div>

    <!-- 1. 顶部 KPI 数据看板 -->
    <div class="kpi-cards">
      <div class="kpi-card" v-for="(item, index) in kpiList" :key="index">
        <div class="kpi-title">{{ item.title }}</div>
        <div class="kpi-value" :style="{ color: item.color }">{{ item.value }}</div>
      </div>
    </div>

    <!-- 2. 中间图表区：两列布局 -->
    <div class="charts-wrapper">
      <!-- 左侧：CPU 折线图 -->
      <div class="chart-card">
        <h3 class="chart-title">集群 CPU 7天趋势</h3>
        <!-- ref 绑定，用于给 ECharts 提供真实的 DOM 容器 -->
        <div ref="cpuChartRef" class="chart-box"></div>
      </div>

      <!-- 右侧：资产分布饼图 -->
      <div class="chart-card">
        <h3 class="chart-title">物理机柜资产分布</h3>
        <div ref="pieChartRef" class="chart-box"></div>
      </div>

      <!-- 下方全宽：网络流量柱状图 -->
      <div class="chart-card full-width">
        <h3 class="chart-title">出入网流量波动监测 (GB)</h3>
        <div ref="netChartRef" class="chart-box-large"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import request from '@/api/request'

// ========================
// DOM 引用定义
// ========================
// 在 Vue3 中，获取 DOM 必须用 ref。在 onMounted 之前它们都是 null
const cpuChartRef = ref<HTMLElement | null>(null)
const pieChartRef = ref<HTMLElement | null>(null)
const netChartRef = ref<HTMLElement | null>(null)

// ========================
// 图表实例与数据定义
// ========================
// 独立保存 ECharts 的实例，用于后续在窗口缩放时调用 resize()，或者在销毁时调用 dispose()
let cpuChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null
let netChart: echarts.ECharts | null = null

const kpiList = reactive([
  { title: '总管辖资产 (台)', value: '-', color: '#409eff' },
  { title: '物理总算力 (CPU)', value: '-', color: '#67c23a' },
  { title: '物理总内存 (GB)', value: '-', color: '#e6a23c' },
  { title: '待处理告警 (条)', value: '-', color: '#f56c6c' }
])

// ========================
// 核心逻辑：获取顶部 KPI 数据
// ========================
const fetchKpiData = async () => {
  const res: any = await request.get('/api/analysis/kpi-stats')
  if (res.code === 200) {
    kpiList[0].value = res.data.totalAssets
    kpiList[1].value = res.data.cpuTotal
    kpiList[2].value = res.data.memTotal
    kpiList[3].value = res.data.alertCount
  }
}

// ========================
// 核心逻辑：渲染 CPU 折线图
// ========================
const initCpuChart = async () => {
  // 防御性编程：确保 DOM 已经存在
  if (!cpuChartRef.value) return
  
  // 初始化图表实例，第二个参数 'dark' 开启内置深色主题
  cpuChart = echarts.init(cpuChartRef.value, 'dark')
  cpuChart.showLoading()

  try {
    const res: any = await request.get('/api/analysis/cpu-trend')
    if (res.code === 200) {
      // Option 是 ECharts 的灵魂，所有的图表长什么样，完全由 Option 里的 JSON 决定
      const option = {
        backgroundColor: 'transparent',
        tooltip: { trigger: 'axis' },
        xAxis: {
          type: 'category',
          data: res.data.xAxis,
          axisLine: { lineStyle: { color: '#888' } }
        },
        yAxis: {
          type: 'value',
          axisLabel: { formatter: '{value} %' },
          splitLine: { lineStyle: { type: 'dashed', color: '#333' } }
        },
        series: [{
          name: 'CPU 使用率',
          data: res.data.seriesData,
          type: 'line',
          smooth: true, // 开启平滑曲线
          lineStyle: { color: '#00e5ff', width: 3 }, // 折线颜色：科技蓝
          areaStyle: {
            // 酷炫的背景渐变色填充
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(0, 229, 255, 0.5)' },
              { offset: 1, color: 'rgba(0, 229, 255, 0.1)' }
            ])
          }
        }]
      }
      // 将配置填入实例，图表就会自动渲染
      cpuChart.setOption(option)
    }
  } finally {
    cpuChart.hideLoading()
  }
}

// ========================
// 核心逻辑：渲染资产饼图
// ========================
const initPieChart = async () => {
  if (!pieChartRef.value) return
  pieChart = echarts.init(pieChartRef.value, 'dark')
  pieChart.showLoading()

  try {
    const res: any = await request.get('/api/analysis/asset-distribution')
    if (res.code === 200) {
      const option = {
        backgroundColor: 'transparent',
        tooltip: { trigger: 'item' },
        legend: { top: '5%', left: 'center' },
        series: [{
          name: '资产分类',
          type: 'pie',
          radius: ['40%', '70%'], // 数组代表内半径和外半径，设置成数组就会变成空心圆环图
          itemStyle: {
            borderRadius: 10,
            borderColor: '#1e1e1e',
            borderWidth: 2
          },
          label: { show: false }, // 默认不显示文字
          emphasis: {
            label: { show: true, fontSize: 20, fontWeight: 'bold' } // 鼠标悬浮时中心显示放大文字
          },
          data: res.data
        }]
      }
      pieChart.setOption(option)
    }
  } finally {
    pieChart.hideLoading()
  }
}

// ========================
// 核心逻辑：渲染网络流量柱状图
// ========================
const initNetChart = async () => {
  if (!netChartRef.value) return
  netChart = echarts.init(netChartRef.value, 'dark')
  netChart.showLoading()

  try {
    const res: any = await request.get('/api/analysis/network-traffic')
    if (res.code === 200) {
      const option = {
        backgroundColor: 'transparent',
        tooltip: { trigger: 'axis' },
        legend: { data: ['流入流量', '流出流量'] },
        xAxis: { type: 'category', data: res.data.xAxis },
        yAxis: { type: 'value' },
        series: [
          {
            name: '流入流量',
            type: 'bar',
            data: res.data.inData,
            itemStyle: { color: '#67c23a' } // 绿色
          },
          {
            name: '流出流量',
            type: 'bar',
            data: res.data.outData,
            itemStyle: { color: '#e6a23c' } // 橙色
          }
        ]
      }
      netChart.setOption(option)
    }
  } finally {
    netChart.hideLoading()
  }
}

// ========================
// 窗口自适应引擎
// ========================
// ECharts 默认不会随着浏览器缩放而改变大小，必须手动监听浏览器 resize 事件，并调用图表的 resize() 方法
const handleResize = () => {
  cpuChart?.resize()
  pieChart?.resize()
  netChart?.resize()
}

// ========================
// 生命周期：挂载与销毁
// ========================
onMounted(() => {
  // DOM 加载完毕后，触发接口请求与图表渲染
  fetchKpiData()
  initCpuChart()
  initPieChart()
  initNetChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  // 组件销毁时（比如切换到了别的菜单），必须销毁图表实例并移除监听，否则会导致内存泄漏！
  cpuChart?.dispose()
  pieChart?.dispose()
  netChart?.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.analysis-container {
  padding: 20px;
  color: #fff;
  height: 100%;
  overflow-y: auto;
}

.header {
  margin-bottom: 20px;
}

.header h2 {
  margin: 0 0 10px 0;
  font-size: 28px;
  letter-spacing: 2px;
}

.subtitle {
  color: #888;
  margin: 0;
  font-size: 14px;
}

/* 顶部 KPI 卡片样式 */
.kpi-cards {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}

.kpi-card {
  flex: 1;
  background: #1e1e1e;
  border-radius: 12px;
  padding: 24px;
  border: 1px solid #333;
  box-shadow: 0 4px 12px rgba(0,0,0,0.2);
  text-align: center;
  transition: transform 0.3s ease;
}

.kpi-card:hover {
  transform: translateY(-5px);
}

.kpi-title {
  color: #999;
  font-size: 14px;
  margin-bottom: 10px;
}

.kpi-value {
  font-size: 32px;
  font-weight: bold;
}

/* 图表区样式 */
.charts-wrapper {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}

.chart-card {
  flex: 1;
  min-width: 400px;
  background: #1e1e1e;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.2);
  border: 1px solid #333;
}

.full-width {
  flex: 100%;
}

.chart-title {
  margin: 0 0 20px 0;
  font-size: 16px;
  font-weight: 500;
  color: #ccc;
  border-left: 4px solid #00e5ff;
  padding-left: 10px;
}

.chart-box {
  width: 100%;
  height: 300px;
}

.chart-box-large {
  width: 100%;
  height: 350px;
}
</style>