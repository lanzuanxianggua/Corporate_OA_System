import * as echarts from "echarts";

export const chartPalette = [
  "#2563eb",
  "#059669",
  "#d97706",
  "#7c3aed",
  "#dc2626",
  "#0891b2",
  "#64748b"
];

export function chartTextStyle() {
  return {
    color: "#374151",
    fontFamily: "Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif"
  };
}

export function axisStyle() {
  return {
    axisLine: { lineStyle: { color: "#d1d5db" } },
    axisTick: { show: false },
    axisLabel: { color: "#6b7280", fontWeight: 600 },
    splitLine: { lineStyle: { color: "#e5e7eb", type: "dashed" } }
  };
}

export function chartGrid(left = 42) {
  return { top: 28, right: 22, bottom: 34, left, containLabel: true };
}

export function axisTooltip() {
  return {
    trigger: "axis",
    axisPointer: { type: "shadow" },
    backgroundColor: "rgba(17, 24, 39, 0.92)",
    borderWidth: 0,
    textStyle: { color: "#ffffff", fontSize: 12 }
  };
}

export function itemTooltip(formatter?: string) {
  return {
    trigger: "item",
    formatter,
    backgroundColor: "rgba(17, 24, 39, 0.92)",
    borderWidth: 0,
    textStyle: { color: "#ffffff", fontSize: 12 }
  };
}

export function createGradient(start: string, end: string, horizontal = false) {
  return new echarts.graphic.LinearGradient(0, 0, horizontal ? 1 : 0, horizontal ? 0 : 1, [
    { offset: 0, color: start },
    { offset: 1, color: end }
  ]);
}

export function emptyChartOption(text: string): echarts.EChartsOption {
  return {
    textStyle: chartTextStyle(),
    title: {
      text,
      left: "center",
      top: "center",
      textStyle: { color: "#9ca3af", fontSize: 14, fontWeight: 600 }
    }
  };
}
