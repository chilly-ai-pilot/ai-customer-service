package com.aicustomer.service;

import com.aicustomer.dto.request.LoginRequest;
import com.aicustomer.dto.request.RegisterRequest;
import com.aicustomer.dto.response.AccountResponse;
import com.aicustomer.entity.CommercialTenant;
import com.aicustomer.exception.AccountAlreadyExistsException;
import com.aicustomer.exception.AccountNotFoundException;
import com.aicustomer.exception.PasswordErrorException;
import com.aicustomer.repository.CommercialTenantRepository;
import com.aicustomer.util.PasswordUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommercialTenantService {

    private final CommercialTenantRepository commercialTenantRepository;

    public CommercialTenantService(CommercialTenantRepository commercialTenantRepository) {
        this.commercialTenantRepository = commercialTenantRepository;
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

        return toResponse(saved);
    }

    public AccountResponse login(LoginRequest request) {
        CommercialTenant tenant = commercialTenantRepository.findByAccount(request.getAccount())
                .orElseThrow(AccountNotFoundException::new);

        String hashedInput = PasswordUtils.hash(request.getPassword());
        if (!hashedInput.equals(tenant.getPassword())) {
            throw new PasswordErrorException();
        }

        return toResponse(tenant);
    }

    private AccountResponse toResponse(CommercialTenant tenant) {
        return new AccountResponse(tenant.getId(), tenant.getAccount(), tenant.getName());
    }
}
