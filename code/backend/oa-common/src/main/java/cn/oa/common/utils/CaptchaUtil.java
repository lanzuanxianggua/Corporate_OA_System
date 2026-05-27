package cn.oa.common.utils;

import cn.oa.common.service.RedisService;
import lombok.Data;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class CaptchaUtil {

    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;
    private static final int CODE_LENGTH = 2;
    private static final long EXPIRE_MINUTES = 5;

    private static final Random RANDOM = new Random();

    @Data
    public static class CaptchaResult {
        private String uuid;
        private String img;
        private String answer;
    }

    public static CaptchaResult generate(RedisService redisService) {
        int a = RANDOM.nextInt(10) + 1;
        int b = RANDOM.nextInt(10) + 1;
        String[] ops = {"+", "-", "*"};
        String op = ops[RANDOM.nextInt(ops.length)];
        int answer = switch (op) {
            case "+" -> a + b;
            case "-" -> a - b;
            case "*" -> a * b;
            default -> 0;
        };

        String text = a + " " + op + " " + b + " = ?";
        String uuid = UUID.randomUUID().toString().replace("-", "");

        redisService.set("captcha:" + uuid, String.valueOf(answer), EXPIRE_MINUTES, TimeUnit.MINUTES);

        BufferedImage image = createImage(text);

        CaptchaResult result = new CaptchaResult();
        result.setUuid(uuid);
        result.setImg("data:image/png;base64," + encodeBase64(image));
        result.setAnswer(String.valueOf(answer));
        return result;
    }

    public static boolean verify(RedisService redisService, String uuid, String code) {
        if (uuid == null || code == null) return false;
        String key = "captcha:" + uuid;
        Object answer = redisService.get(key);
        if (answer == null) return false;
        redisService.delete(key);
        return code.trim().equals(answer.toString().trim());
    }

    private static BufferedImage createImage(String text) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 22));
        g.drawString(text, 10, 28);

        for (int i = 0; i < 30; i++) {
            g.setColor(new Color(RANDOM.nextInt(256), RANDOM.nextInt(256), RANDOM.nextInt(256)));
            g.drawLine(RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT), RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT));
        }
        g.dispose();
        return image;
    }

    private static String encodeBase64(BufferedImage image) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("验证码图片编码失败", e);
        }
    }
}
