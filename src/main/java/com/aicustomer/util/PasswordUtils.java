package com.aicustomer.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 密码工具类，提供 SHA-256 哈希。
 */
public final class PasswordUtils {

    private PasswordUtils() {}

    /**
     * 对明文密码进行 SHA-256 哈希。
     *
     * @param password 明文密码
     * @return 十六进制哈希字符串
     */
    public static String hash(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
