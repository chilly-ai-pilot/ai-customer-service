import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue')
  },
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/merchant',
    component: () => import('@/views/MerchantLayout.vue'),
    children: [
      {
        path: 'goods',
        name: 'MerchantGoods',
        component: () => import('@/views/MerchantGoodsView.vue')
      },
      {
        path: 'inbox',
        name: 'MerchantChat',
        component: () => import('@/views/MerchantChatView.vue')
      },
      {
        path: 'inbox/:sessionId',
        name: 'MerchantChatWindow',
        component: () => import('@/views/MerchantChatWindowView.vue')
      }
    ]
  },
  {
    path: '/user',
    component: () => import('@/views/UserLayout.vue'),
    children: [
      {
        path: 'goods',
        name: 'UserGoods',
        component: () => import('@/views/UserGoodsView.vue')
      },
      {
        path: 'inbox',
        name: 'UserChat',
        component: () => import('@/views/UserChatView.vue')
      },
      {
        // sessionId 可选：从商品卡片"咨询"进入时不带 sessionId（带 goodsId/ctId query），
        // 从"我的咨询"列表进入时带 sessionId。同一个路由名，SESSION_CREATED 后
        // router.replace 更新 params 不会重新挂载组件，WebSocket 连接得以保留。
        path: 'inbox/:sessionId?',
        name: 'UserChatWindow',
        component: () => import('@/views/UserChatWindowView.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
