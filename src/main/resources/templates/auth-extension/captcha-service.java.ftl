package ${packageName}.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 字符型验证码服务（内存存储，一次性校验）
 */
@Service
public class CaptchaService {

    private static final char[] CHAR_POOL = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ConcurrentHashMap<String, CaptchaEntry> store = new ConcurrentHashMap<>();

    @Value("${r"${app.captcha.length:4}"}")
    private int length;

    @Value("${r"${app.captcha.expire-seconds:300}"}")
    private int expireSeconds;

    public Map<String, String> createCaptcha() {
        cleanupExpired();
        String code = randomCode();
        String key = UUID.randomUUID().toString().replace("-", "");
        store.put(key, new CaptchaEntry(code.toLowerCase(), System.currentTimeMillis()));
        String svg = buildSvg(code);
        String imageDataUrl = "data:image/svg+xml;base64,"
                + Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
        Map<String, String> result = new HashMap<>();
        result.put("captchaKey", key);
        result.put("captchaImage", imageDataUrl);
        return result;
    }

    public boolean verifyAndConsume(String key, String input) {
        if (key == null || key.isBlank() || input == null || input.isBlank()) {
            return false;
        }
        CaptchaEntry entry = store.remove(key.trim());
        if (entry == null || entry.isExpired(expireSeconds)) {
            return false;
        }
        return entry.code.equals(input.trim().toLowerCase());
    }

    private String randomCode() {
        int len = Math.max(4, Math.min(length, 6));
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(CHAR_POOL[RANDOM.nextInt(CHAR_POOL.length)]);
        }
        return sb.toString();
    }

    private String buildSvg(String code) {
        int width = 28 * code.length() + 20;
        int height = 44;
        StringBuilder chars = new StringBuilder();
        for (int i = 0; i < code.length(); i++) {
            int x = 18 + i * 28;
            int rotate = RANDOM.nextInt(21) - 10;
            int y = 28 + RANDOM.nextInt(7) - 3;
            chars.append("<text x=\"").append(x).append("\" y=\"").append(y)
                    .append("\" font-size=\"22\" font-family=\"Arial,sans-serif\" font-weight=\"bold\"")
                    .append(" fill=\"#2f497a\" transform=\"rotate(").append(rotate).append(" ").append(x).append(" ").append(y).append(")\">")
                    .append(code.charAt(i)).append("</text>");
        }
        String noise = "";
        for (int i = 0; i < 4; i++) {
            int x1 = RANDOM.nextInt(width);
            int y1 = RANDOM.nextInt(height);
            int x2 = RANDOM.nextInt(width);
            int y2 = RANDOM.nextInt(height);
            noise += "<line x1=\"" + x1 + "\" y1=\"" + y1 + "\" x2=\"" + x2 + "\" y2=\"" + y2 + "\" stroke=\"#c0c4cc\" stroke-width=\"1\"/>";
        }
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"" + width + "\" height=\"" + height + "\" viewBox=\"0 0 " + width + " " + height + "\">"
                + "<rect width=\"100%\" height=\"100%\" fill=\"#f5f7fa\" rx=\"4\"/>"
                + noise + chars + "</svg>";
    }

    private void cleanupExpired() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, CaptchaEntry>> it = store.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, CaptchaEntry> e = it.next();
            if (e.getValue().isExpiredAt(now, expireSeconds)) {
                it.remove();
            }
        }
    }

    private static final class CaptchaEntry {
        private final String code;
        private final long createdAt;

        private CaptchaEntry(String code, long createdAt) {
            this.code = code;
            this.createdAt = createdAt;
        }

        private boolean isExpired(int expireSeconds) {
            return isExpiredAt(System.currentTimeMillis(), expireSeconds);
        }

        private boolean isExpiredAt(long now, int expireSeconds) {
            return now - createdAt > expireSeconds * 1000L;
        }
    }
}
