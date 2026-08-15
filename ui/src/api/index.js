import request from './request'

export const commercialTenantApi = {
  login: (data) => request.post('/commercialTenant/login', data),
  register: (data) => request.post('/commercialTenant/register', data)
}

export const userApi = {
  login: (data) => request.post('/user/login', data),
  register: (data) => request.post('/user/register', data)
}

export const goodsApi = {
  mine: (params) => request.get('/goods/mine', { params }),
  all: (params) => request.get('/goods/all', { params }),
  add: (data) => request.post('/goods/add', data),
  update: (data) => request.put('/goods/update', data),
  delete: (params) => request.delete('/goods/delete', { params })
}

export const workbenchApi = {
  menu: () => request.get('/workbench/menu')
}
