package com.aicustomer.service;

import com.aicustomer.dto.request.AddGoodsRequest;
import com.aicustomer.dto.request.UpdateGoodsRequest;
import com.aicustomer.dto.response.GoodsResponse;
import com.aicustomer.entity.Goods;
import com.aicustomer.exception.ForbiddenException;
import com.aicustomer.exception.ResourceNotFoundException;
import com.aicustomer.repository.GoodsRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 商品服务，提供商品的增删改查。
 */
@Service
public class GoodsService {

    private final GoodsRepository goodsRepository;

    public GoodsService(GoodsRepository goodsRepository) {
        this.goodsRepository = goodsRepository;
    }

    /**
     * 新增商品（归属当前登录商户）。
     */
    @Transactional
    public GoodsResponse add(Long ctId, AddGoodsRequest request) {
        Goods goods = Goods.builder()
                .name(request.getName())
                .ctId(ctId)
                .build();
        Goods saved = goodsRepository.save(goods);
        return toResponse(saved);
    }

    /**
     * 更新商品（仅商品所属商户可操作）。
     *
     * @throws ResourceNotFoundException 商品不存在
     * @throws ForbiddenException        无权操作（不是所属商户）
     */
    @Transactional
    public GoodsResponse update(Long ctId, UpdateGoodsRequest request) {
        Goods goods = goodsRepository.findById(request.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Goods", request.getId()));
        if (!goods.getCtId().equals(ctId)) {
            throw new ForbiddenException();
        }
        goods.setName(request.getName());
        Goods saved = goodsRepository.save(goods);
        return toResponse(saved);
    }

    /**
     * 删除商品（仅商品所属商户可操作）。
     *
     * @throws ResourceNotFoundException 商品不存在
     * @throws ForbiddenException        无权操作
     */
    @Transactional
    public void delete(Long ctId, Long id) {
        Goods goods = goodsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Goods", id));
        if (!goods.getCtId().equals(ctId)) {
            throw new ForbiddenException();
        }
        goodsRepository.delete(goods);
    }

    /** 查询当前商户的全部商品 */
    public Page<GoodsResponse> mine(Long ctId, int pageNum, int pageSize) {
        PageRequest pageRequest = PageRequest.of(pageNum - 1, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        return goodsRepository.findByCtId(ctId, pageRequest).map(this::toResponse);
    }

    /** 查询全部商品（无需登录） */
    public Page<GoodsResponse> all(int pageNum, int pageSize) {
        PageRequest pageRequest = PageRequest.of(pageNum - 1, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        return goodsRepository.findAll(pageRequest).map(this::toResponse);
    }

    /** 查询商品详情 */
    public GoodsResponse detail(Long id) {
        Goods goods = goodsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Goods", id));
        return toResponse(goods);
    }

    private GoodsResponse toResponse(Goods goods) {
        return GoodsResponse.builder()
                .id(goods.getId())
                .name(goods.getName())
                .ctId(goods.getCtId())
                .build();
    }
}
