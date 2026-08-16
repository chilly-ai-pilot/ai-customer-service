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

/**
 * 用户服务，提供用户注册、登录和信息查询。
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final TokenService tokenService;

    public UserService(UserRepository userRepository, TokenService tokenService) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
    }

    /**
     * 用户注册。
     *
     * @param request 注册信息
     * @return 注册成功响应（含 token）
     * @throws AccountAlreadyExistsException 账号已存在
     */
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

    /**
     * 按用户 ID 查询名称，用于聊天窗口展示对方名字。
     *
     * @param userId 用户 ID
     * @return 用户名称，查不到返回 null（不抛异常，避免打断聊天页）
     */
    public String getName(Long userId) {
        return userRepository.findById(userId).map(User::getName).orElse(null);
    }

    /**
     * 用户登录。
     *
     * @param request 登录信息
     * @return 登录成功响应（含 token）
     * @throws AccountNotFoundException 账号不存在
     * @throws PasswordErrorException   密码错误
     */
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
