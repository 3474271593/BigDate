import * as echarts from 'echarts/core'
import { BarChart, PictorialBarChart, LineChart, PieChart, ScatterChart, HeatmapChart } from 'echarts/charts'
import { DataZoomComponent, GraphicComponent, GridComponent, LegendComponent, MarkPointComponent, TitleComponent, TooltipComponent, VisualMapComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
echarts.use([BarChart,PictorialBarChart,LineChart,PieChart,ScatterChart,HeatmapChart,DataZoomComponent,GraphicComponent,GridComponent,LegendComponent,MarkPointComponent,TitleComponent,TooltipComponent,VisualMapComponent,CanvasRenderer])
export { echarts }
