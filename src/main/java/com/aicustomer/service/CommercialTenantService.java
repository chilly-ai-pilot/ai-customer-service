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

@Service
public class CommercialTenantService {

    private final CommercialTenantRepository commercialTenantRepository;
    private final TokenService tokenService;

    public CommercialTenantService(CommercialTenantRepository commercialTenantRepository, TokenService tokenService) {
        this.commercialTenantRepository = commercialTenantRepository;
        this.tokenService = tokenService;
    }

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

    /** 供聊天窗口按 ctId 反查商户名称展示，查不到返回 null，不抛异常打断聊天页 */
    public String getName(Long ctId) {
        return commercialTenantRepository.findById(ctId).map(CommercialTenant::getName).orElse(null);
    }

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