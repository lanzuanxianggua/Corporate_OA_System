package cn.oa.common.utils;

import cn.oa.common.exception.BusinessException;
import java.util.regex.Pattern;

public class PasswordUtil {

    private static final Pattern LETTER_PATTERN = Pattern.compile("[a-zA-Z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]");

    public static void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new BusinessException("密码长度不能少于8位");
        }
        if (password.length() > 32) {
            throw new BusinessException("密码长度不能超过32位");
        }
        if (!LETTER_PATTERN.matcher(password).find()) {
            throw new BusinessException("密码必须包含字母");
        }
        if (!DIGIT_PATTERN.matcher(password).find()) {
            throw new BusinessException("密码必须包含数字");
        }
    }

    public static String checkPasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            return "weak";
        }
        boolean hasLetter = LETTER_PATTERN.matcher(password).find();
        boolean hasDigit = DIGIT_PATTERN.matcher(password).find();
        boolean hasSpecial = SPECIAL_PATTERN.matcher(password).find();
        boolean hasUpper = !password.equals(password.toLowerCase());

        if (!hasLetter || !hasDigit) {
            return "weak";
        }
        if (hasSpecial && hasUpper && password.length() >= 12) {
            return "strong";
        }
        return "medium";
    }
}
