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
        path: 'inbox/new',
        name: 'UserNewChat',
        component: () => import('@/views/UserChatWindowView.vue')
      },
      {
        path: 'inbox/:sessionId',
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
