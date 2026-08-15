package com.aicustomer.service;

import com.aicustomer.dto.request.LoginRequest;
import com.aicustomer.dto.request.RegisterRequest;
import com.aicustomer.dto.response.AccountResponse;
import com.aicustomer.entity.CommercialTenant;
import com.aicustomer.exception.AccountAlreadyExistsException;
import com.aicustomer.exception.AccountNotFoundException;
import com.aicustomer.exception.PasswordErrorException;
import com.aicustomer.repository.CommercialTenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

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
        tenant.setPassword(hashPassword(request.getPassword()));
        tenant.setName(request.getName());
        CommercialTenant saved = commercialTenantRepository.save(tenant);

        return toResponse(saved);
    }

    public AccountResponse login(LoginRequest request) {
        CommercialTenant tenant = commercialTenantRepository.findByAccount(request.getAccount())
                .orElseThrow(AccountNotFoundException::new);

        String hashedInput = hashPassword(request.getPassword());
        if (!hashedInput.equals(tenant.getPassword())) {
            throw new PasswordErrorException();
        }

        return toResponse(tenant);
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private AccountResponse toResponse(CommercialTenant tenant) {
        return new AccountResponse(tenant.getId(), tenant.getAccount(), tenant.getName());
    }

    @Transactional
    public void cleanup(String account) {
        commercialTenantRepository.findByAccount(account).ifPresent(commercialTenantRepository::delete);
    }
}
