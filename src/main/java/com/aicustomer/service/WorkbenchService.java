package com.aicustomer.service;

import com.aicustomer.dto.response.MenuItemResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WorkbenchService {

    public List<MenuItemResponse> getMenu() {
        return List.of(
                MenuItemResponse.builder()
                        .name("商品管理")
                        .path("/goods")
                        .placeholder(false)
                        .build(),
                MenuItemResponse.builder()
                        .name("知识库")
                        .path("/knowledge")
                        .placeholder(true)
                        .build(),
                MenuItemResponse.builder()
                        .name("AI设置")
                        .path("/ai-config")
                        .placeholder(true)
                        .build(),
                MenuItemResponse.builder()
                        .name("会话收件箱")
                        .path("/inbox")
                        .placeholder(true)
                        .build(),
                MenuItemResponse.builder()
                        .name("经营数据")
                        .path("/stats")
                        .placeholder(true)
                        .build()
        );
    }
}
