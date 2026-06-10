import * as echarts from "@/utils/echarts";

function cssVar(name: string, fallback: string) {
  if (typeof window === "undefined") return fallback;
  const value = getComputedStyle(document.documentElement).getPropertyValue(name).trim();
  return value || fallback;
}

export const chartPalette = [
  "#2563eb",
  "#14b8a6",
  "#f97316",
  "#7c3aed",
  "#ef4444",
  "#16a34a",
  "#0891b2",
  "#eab308",
  "#db2777",
  "#64748b"
];

export function chartTextColor() {
  return cssVar("--oa-text-soft", "#374151");
}

export function chartMutedColor() {
  return cssVar("--oa-muted", "#6b7280");
}

export function chartSubtleColor() {
  return cssVar("--oa-subtle", "#9ca3af");
}

export function chartBorderColor() {
  return cssVar("--oa-border-soft", "#eef2f7");
}

export function chartTextStyle() {
  return {
    color: chartTextColor(),
    fontFamily: "Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif"
  };
}

export function axisStyle() {
  return {
    axisLine: { lineStyle: { color: cssVar("--oa-border", "#d1d5db") } },
    axisTick: { show: false },
    axisLabel: { color: chartMutedColor(), fontWeight: 600 },
    splitLine: { lineStyle: { color: cssVar("--oa-chart-split-line", "#e5e7eb"), type: "dashed" } }
  };
}

export function chartGrid(left = 42) {
  return { top: 28, right: 22, bottom: 34, left, containLabel: true };
}

export function axisTooltip() {
  return {
    trigger: "axis",
    axisPointer: { type: "shadow" },
    backgroundColor: cssVar("--oa-chart-tooltip-bg", "rgba(17, 24, 39, 0.92)"),
    borderWidth: 0,
    textStyle: { color: "#ffffff", fontSize: 12 }
  };
}

export function itemTooltip(formatter?: string) {
  return {
    trigger: "item",
    formatter,
    backgroundColor: cssVar("--oa-chart-tooltip-bg", "rgba(17, 24, 39, 0.92)"),
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
      textStyle: { color: chartSubtleColor(), fontSize: 14, fontWeight: 600 }
    }
  };
}
