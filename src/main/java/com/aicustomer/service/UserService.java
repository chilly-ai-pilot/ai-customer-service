package com.aicustomer.service;

import com.aicustomer.dto.request.LoginRequest;
import com.aicustomer.dto.request.RegisterRequest;
import com.aicustomer.dto.response.AccountResponse;
import com.aicustomer.entity.User;
import com.aicustomer.exception.AccountAlreadyExistsException;
import com.aicustomer.exception.AccountNotFoundException;
import com.aicustomer.exception.PasswordErrorException;
import com.aicustomer.constant.SubjectType;
import com.aicustomer.repository.UserRepository;
import com.aicustomer.util.PasswordUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final TokenService tokenService;

    public UserService(UserRepository userRepository, TokenService tokenService) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
    }

    @Transactional
    public AccountResponse register(RegisterRequest request) {
        if (userRepository.findByAccount(request.getAccount()).isPresent()) {
            throw new AccountAlreadyExistsException();
        }

        User user = new User();
        user.setAccount(request.getAccount());
        user.setPassword(PasswordUtils.hash(request.getPassword()));
        user.setName(request.getName());
        User saved = userRepository.save(user);

        String token = tokenService.generateToken(SubjectType.USER, saved.getId());
        return AccountResponse.builder()
                .id(saved.getId())
                .account(saved.getAccount())
                .name(saved.getName())
                .token(token)
                .build();
    }

    public AccountResponse login(LoginRequest request) {
        User user = userRepository.findByAccount(request.getAccount())
                .orElseThrow(AccountNotFoundException::new);

        String hashedInput = PasswordUtils.hash(request.getPassword());
        if (!hashedInput.equals(user.getPassword())) {
            throw new PasswordErrorException();
        }

        String token = tokenService.generateToken(SubjectType.USER, user.getId());
        return AccountResponse.builder()
                .id(user.getId())
                .account(user.getAccount())
                .name(user.getName())
                .token(token)
                .build();
    }
}
