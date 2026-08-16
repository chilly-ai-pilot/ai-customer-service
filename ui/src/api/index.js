import axios from './request'

/** 会话相关接口 */
export const sessionApi = {
  /** 用户查询自己的会话列表 */
  listByUser(params) {
    return axios.get('/session/user/list', { params })
  },
  /** 商户查询自己的会话列表 */
  listByTenant(params) {
    return axios.get('/session/ct/list', { params })
  },
  /** 查询会话消息列表 */
  getMessages(sessionId, params) {
    return axios.get(`/session/${sessionId}/message/list`, { params })
  },
  /** 标记消息已读 */
  markRead(sessionId) {
    return axios.put(`/session/${sessionId}/message/read`)
  },
  /** 查询会话详情 */
  detail(sessionId) {
    return axios.get(`/session/${sessionId}`)
  }
}

/** 工作台相关接口 */
export const workbenchApi = {
  /** 获取工作台菜单 */
  menu() {
    return axios.get('/workbench/menu')
  }
}

/** 商品相关接口 */
export const goodsApi = {
  /** 查询全部商品（无需登录） */
  all(params) {
    return axios.get('/goods/all', { params })
  },
  /** 查询我的商品（需登录） */
  mine(params) {
    return axios.get('/goods/mine', { params })
  },
  /** 新增商品 */
  add(data) {
    return axios.post('/goods/add', data)
  },
  /** 更新商品 */
  update(data) {
    return axios.put('/goods/update', data)
  },
  /** 删除商品 */
  delete(params) {
    return axios.delete('/goods/delete', { params })
  },
  /** 查询商品详情 */
  detail(params) {
    return axios.get('/goods/detail', { params })
  }
}

/** 用户相关接口 */
export const userApi = {
  /** 用户登录 */
  login(data) {
    return axios.post('/user/login', data)
  },
  /** 用户注册 */
  register(data) {
    return axios.post('/user/register', data)
  },
  /** 按用户 ID 查询名称（供聊天窗口展示对方名字用） */
  name(id) {
    return axios.get(`/user/${id}/name`)
  }
}

/** 商户相关接口 */
export const commercialTenantApi = {
  /** 商户登录 */
  login(data) {
    return axios.post('/commercialTenant/login', data)
  },
  /** 商户注册 */
  register(data) {
    return axios.post('/commercialTenant/register', data)
  },
  /** 按商户 ID 查询名称（供聊天窗口展示对方名字用） */
  name(id) {
    return axios.get(`/commercialTenant/${id}/name`)
  }
}
