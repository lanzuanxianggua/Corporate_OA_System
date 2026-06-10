import { init, graphic, use, type ECharts, type EChartsCoreOption } from "echarts/core";
import {
  BarChart,
  FunnelChart,
  GaugeChart,
  HeatmapChart,
  LineChart,
  PieChart,
  RadarChart,
  ScatterChart
} from "echarts/charts";
import {
  DataZoomComponent,
  GridComponent,
  LegendComponent,
  RadarComponent,
  TitleComponent,
  TooltipComponent,
  VisualMapComponent
} from "echarts/components";
import { CanvasRenderer } from "echarts/renderers";

use([
  BarChart,
  FunnelChart,
  GaugeChart,
  HeatmapChart,
  LineChart,
  PieChart,
  RadarChart,
  ScatterChart,
  DataZoomComponent,
  GridComponent,
  LegendComponent,
  RadarComponent,
  TitleComponent,
  TooltipComponent,
  VisualMapComponent,
  CanvasRenderer
]);

export { graphic, init };
export type EChartsOption = EChartsCoreOption;
export type { ECharts };
