import { createRouter, createWebHistory } from 'vue-router'

/**
 * 路由配置。
 *
 * 设计说明：
 * - /merchant 下的路由对应商户端
 * - /user 下的路由对应用户端
 * - 用户端聊天路由 /user/inbox/:sessionId?：
 *   sessionId 可选（从商品"咨询"进入时不带，从会话列表进入时带）。
 *   同一个路由名（UserChatWindow），SESSION_CREATED 后用 router.replace 更新 params
 *   不会重新挂载组件，WebSocket 连接得以保留。
 */
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
  // 商户端
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
  // 用户端
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
        // sessionId 可选：从商品"咨询"进入时不带 sessionId（带 goodsId/ctId query），
        // 从"我的咨询"列表进入时带 sessionId。
        // 同一路由名（UserChatWindow），SESSION_CREATED 后 router.replace 更新 params
        // 不会重新挂载组件，WebSocket 连接得以保留。
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
