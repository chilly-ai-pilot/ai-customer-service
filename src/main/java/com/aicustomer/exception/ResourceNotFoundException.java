package com.aicustomer.exception;

/**
 * 404 Not Found：请求的资源（Session、Goods 等）在数据库中不存在。
 */
public class ResourceNotFoundException extends BusinessException {
    public ResourceNotFoundException(String resourceName, Long id) {
        super(404, resourceName + " not found, id: " + id);
    }
}
