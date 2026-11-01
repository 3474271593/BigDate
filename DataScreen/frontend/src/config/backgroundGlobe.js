const range=(start,end,step)=>Array.from({length:Math.floor((end-start)/step)+1},(_,index)=>start+(index*step))

const longitudeLines=range(-150,180,30).map(longitude=>({coords:range(-75,75,5).map(latitude=>[longitude,latitude])}))
const latitudeLines=range(-60,60,30).map(latitude=>({coords:range(-180,180,6).map(longitude=>[longitude,latitude])}))

export const decorativeArcs=[
  {coords:[[-146,38],[-73,58]]},{coords:[[-122,-18],[-35,14]]},{coords:[[-89,8],[-18,48]]},
  {coords:[[-38,-42],[42,-18]]},{coords:[[4,58],[76,34]]},{coords:[[18,12],[104,-12]]},
  {coords:[[56,-36],[132,-14]]},{coords:[[92,52],[156,24]]},{coords:[[142,-36],[-168,-8]]}
]

export const createBackgroundGlobeOption=(reduceMotion=false)=>({
  backgroundColor:'transparent',tooltip:{show:false},
  globe:{baseColor:'#071326',shading:'lambert',atmosphere:{show:true},light:{ambient:{intensity:.85},main:{intensity:.7,shadow:false,alpha:32,beta:145}},postEffect:{enable:false},temporalSuperSampling:{enable:false},viewControl:{projection:'perspective',autoRotate:!reduceMotion,autoRotateSpeed:1.8,autoRotateAfterStill:4,distance:188,alpha:19,beta:158,minDistance:160,maxDistance:220,damping:.8,rotateSensitivity:0,zoomSensitivity:0,panSensitivity:0}},
  series:[
    {name:'装饰经纬网',type:'lines3D',coordinateSystem:'globe',silent:true,polyline:true,blendMode:'lighter',data:[...longitudeLines,...latitudeLines],lineStyle:{color:'#2f86c9',width:.55,opacity:.13},effect:{show:false}},
    {name:'装饰光弧',type:'lines3D',coordinateSystem:'globe',silent:true,blendMode:'lighter',data:decorativeArcs,lineStyle:{color:'#4bd8ff',width:1.2,opacity:.28},effect:{show:!reduceMotion,trailWidth:1.5,trailLength:.2,trailOpacity:.45,trailColor:'#c66cff',constantSpeed:3.5}}
  ]
})
