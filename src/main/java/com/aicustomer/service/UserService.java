package com.aicustomer.service;

import com.aicustomer.dto.request.LoginRequest;
import com.aicustomer.dto.request.RegisterRequest;
import com.aicustomer.dto.response.AccountResponse;
import com.aicustomer.entity.User;
import com.aicustomer.exception.AccountAlreadyExistsException;
import com.aicustomer.exception.AccountNotFoundException;
import com.aicustomer.exception.PasswordErrorException;
import com.aicustomer.repository.UserRepository;
import com.aicustomer.util.PasswordUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
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

        return toResponse(saved);
    }

    public AccountResponse login(LoginRequest request) {
        User user = userRepository.findByAccount(request.getAccount())
                .orElseThrow(AccountNotFoundException::new);

        String hashedInput = PasswordUtils.hash(request.getPassword());
        if (!hashedInput.equals(user.getPassword())) {
            throw new PasswordErrorException();
        }

        return toResponse(user);
    }

    private AccountResponse toResponse(User user) {
        return new AccountResponse(user.getId(), user.getAccount(), user.getName());
    }
}
