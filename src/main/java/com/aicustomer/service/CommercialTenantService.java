package com.aicustomer.service;

import com.aicustomer.dto.request.LoginRequest;
import com.aicustomer.dto.request.RegisterRequest;
import com.aicustomer.dto.response.AccountResponse;
import com.aicustomer.entity.CommercialTenant;
import com.aicustomer.exception.AccountAlreadyExistsException;
import com.aicustomer.exception.AccountNotFoundException;
import com.aicustomer.exception.PasswordErrorException;
import com.aicustomer.constant.SubjectType;
import com.aicustomer.repository.CommercialTenantRepository;
import com.aicustomer.util.PasswordUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 商户服务，提供商户注册、登录和信息查询。
 */
@Service
public class CommercialTenantService {

    private final CommercialTenantRepository commercialTenantRepository;
    private final TokenService tokenService;

    public CommercialTenantService(CommercialTenantRepository commercialTenantRepository, TokenService tokenService) {
        this.commercialTenantRepository = commercialTenantRepository;
        this.tokenService = tokenService;
    }

    /**
     * 商户注册。
     *
     * @param request 注册信息
     * @return 注册成功响应（含 token）
     * @throws AccountAlreadyExistsException 账号已存在
     */
    @Transactional
    public AccountResponse register(RegisterRequest request) {
        if (commercialTenantRepository.findByAccount(request.getAccount()).isPresent()) {
            throw new AccountAlreadyExistsException();
        }

        CommercialTenant tenant = new CommercialTenant();
        tenant.setAccount(request.getAccount());
        tenant.setPassword(PasswordUtils.hash(request.getPassword()));
        tenant.setName(request.getName());
        CommercialTenant saved = commercialTenantRepository.save(tenant);

        String token = tokenService.generateToken(SubjectType.TENANT, saved.getId());
        return AccountResponse.builder()
                .id(saved.getId())
                .account(saved.getAccount())
                .name(saved.getName())
                .token(token)
                .build();
    }

    /**
     * 按商户 ID 查询名称，用于聊天窗口展示对方名字。
     *
     * @param ctId 商户 ID
     * @return 商户名称，查不到返回 null（不抛异常，避免打断聊天页）
     */
    public String getName(Long ctId) {
        return commercialTenantRepository.findById(ctId).map(CommercialTenant::getName).orElse(null);
    }

    /**
     * 商户登录。
     *
     * @param request 登录信息
     * @return 登录成功响应（含 token）
     * @throws AccountNotFoundException 账号不存在
     * @throws PasswordErrorException   密码错误
     */
    public AccountResponse login(LoginRequest request) {
        CommercialTenant tenant = commercialTenantRepository.findByAccount(request.getAccount())
                .orElseThrow(AccountNotFoundException::new);

        String hashedInput = PasswordUtils.hash(request.getPassword());
        if (!hashedInput.equals(tenant.getPassword())) {
            throw new PasswordErrorException();
        }

        String token = tokenService.generateToken(SubjectType.TENANT, tenant.getId());
        return AccountResponse.builder()
                .id(tenant.getId())
                .account(tenant.getAccount())
                .name(tenant.getName())
                .token(token)
                .build();
    }
}
