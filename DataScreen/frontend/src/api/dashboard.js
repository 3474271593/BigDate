import client from './client'
const q=(url,params,signal)=>client.get(url,{params,signal})
export const loadDashboard=(params,signal)=>{
 const requests=[
 q('/dashboard/overview',params,signal),q('/trade/trend',params,signal),q('/trade/order-trend',params,signal),
 q('/goods/paid-volume-top',params,signal),q('/goods/order-volume-top',params,signal),
 q('/shop/paid-amount-top',params,signal),q('/shop/order-top',params,signal),q('/shop/user-top',params,signal),
 q('/user/paid-amount-top',params,signal),q('/user/order-top',params,signal),
 q('/dashboard/dataset-meta',params,signal),q('/goods/paid-volume-all',params,signal),
 q('/shop/contribution',params,signal),q('/user/value-distribution',params,signal),q('/trade/active-days',params,signal),
 q('/shop/time-paid-amount',params,signal)
 ]
 if(params.dataset==='test')requests.push(q('/goods/sales-value-quadrants',params,signal))
 if(params.dataset==='historical')requests.push(q('/shop/history-heatmap',params,signal),q('/trade/history-volume-price',params,signal))
 return Promise.allSettled(requests)
}
