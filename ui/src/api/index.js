import axios from './request'

export const sessionApi = {
  listByUser(params) {
    return axios.get('/session/user/list', { params })
  },
  listByTenant(params) {
    return axios.get('/session/ct/list', { params })
  },
  getMessages(sessionId, params) {
    return axios.get(`/session/${sessionId}/message/list`, { params })
  },
  markRead(sessionId) {
    return axios.put(`/session/${sessionId}/message/read`)
  },
  getUnreadCount(sessionId) {
    return axios.get(`/session/${sessionId}/message/unread-count`)
  },
  detail(sessionId) {
    return axios.get(`/session/${sessionId}`)
  }
}

export const workbenchApi = {
  menu() {
    return axios.get('/workbench/menu')
  }
}

export const goodsApi = {
  all(params) {
    return axios.get('/goods/all', { params })
  },
  mine(params) {
    return axios.get('/goods/mine', { params })
  },
  add(data) {
    return axios.post('/goods/add', data)
  },
  update(data) {
    return axios.put('/goods/update', data)
  },
  delete(params) {
    return axios.delete('/goods/delete', { params })
  },
  detail(params) {
    return axios.get('/goods/detail', { params })
  }
}

export const userApi = {
  login(data) {
    return axios.post('/user/login', data)
  },
  register(data) {
    return axios.post('/user/register', data)
  }
}

export const commercialTenantApi = {
  login(data) {
    return axios.post('/commercialTenant/login', data)
  },
  register(data) {
    return axios.post('/commercialTenant/register', data)
  }
}
