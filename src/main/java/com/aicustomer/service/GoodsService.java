package com.aicustomer.service;

import com.aicustomer.dto.request.AddGoodsRequest;
import com.aicustomer.dto.request.UpdateGoodsRequest;
import com.aicustomer.dto.response.GoodsResponse;
import com.aicustomer.entity.Goods;
import com.aicustomer.exception.ForbiddenException;
import com.aicustomer.exception.UnauthorizedException;
import com.aicustomer.constant.SubjectType;
import com.aicustomer.repository.GoodsRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GoodsService {

    private final GoodsRepository goodsRepository;
    private final TokenService tokenService;

    public GoodsService(GoodsRepository goodsRepository, TokenService tokenService) {
        this.goodsRepository = goodsRepository;
        this.tokenService = tokenService;
    }

    @Transactional
    public GoodsResponse add(String token, AddGoodsRequest request) {
        Long ctId = resolveTenant(token);
        Goods goods = Goods.builder()
                .name(request.getName())
                .ctId(ctId)
                .build();
        Goods saved = goodsRepository.save(goods);
        return toResponse(saved);
    }

    @Transactional
    public GoodsResponse update(String token, UpdateGoodsRequest request) {
        Long ctId = resolveTenant(token);
        Goods goods = goodsRepository.findById(request.getId())
                .orElseThrow(() -> new ForbiddenException());
        if (!goods.getCtId().equals(ctId)) {
            throw new ForbiddenException();
        }
        goods.setName(request.getName());
        Goods saved = goodsRepository.save(goods);
        return toResponse(saved);
    }

    @Transactional
    public void delete(String token, Long id) {
        Long ctId = resolveTenant(token);
        Goods goods = goodsRepository.findById(id)
                .orElseThrow(() -> new ForbiddenException());
        if (!goods.getCtId().equals(ctId)) {
            throw new ForbiddenException();
        }
        goodsRepository.delete(goods);
    }

    public Page<GoodsResponse> mine(String token, int pageNum, int pageSize) {
        Long ctId = resolveTenant(token);
        PageRequest pageRequest = PageRequest.of(pageNum - 1, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        return goodsRepository.findByCtId(ctId, pageRequest).map(this::toResponse);
    }

    public Page<GoodsResponse> all(int pageNum, int pageSize) {
        PageRequest pageRequest = PageRequest.of(pageNum - 1, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        return goodsRepository.findAll(pageRequest).map(this::toResponse);
    }

    public GoodsResponse detail(Long id) {
        Goods goods = goodsRepository.findById(id)
                .orElseThrow(() -> new ForbiddenException());
        return toResponse(goods);
    }

    private Long resolveTenant(String token) {
        if (token == null || token.isBlank()) {
            throw new UnauthorizedException();
        }
        Long ctId = tokenService.resolve(SubjectType.TENANT, token);
        if (ctId == null) {
            throw new UnauthorizedException();
        }
        return ctId;
    }

    private GoodsResponse toResponse(Goods goods) {
        return GoodsResponse.builder()
                .id(goods.getId())
                .name(goods.getName())
                .ctId(goods.getCtId())
                .build();
    }
}
