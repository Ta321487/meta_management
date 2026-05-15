package com.metadata.util;

import com.metadata.entity.MetadataAdmin;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * 从当前 HTTP 会话解析登录管理员，供操作日志等场景使用。
 */
public final class CurrentUserHolder {

    private static final String SESSION_ADMIN = "admin";
    private static final String FALLBACK_OPERATOR = "system";

    private CurrentUserHolder() {
    }

    public static String getUsername() {
        MetadataAdmin admin = getAdmin();
        return admin != null && admin.getUsername() != null ? admin.getUsername() : null;
    }

    public static MetadataAdmin getAdmin() {
        HttpSession session = getSession(false);
        if (session == null) {
            return null;
        }
        Object attr = session.getAttribute(SESSION_ADMIN);
        return attr instanceof MetadataAdmin ? (MetadataAdmin) attr : null;
    }

    /**
     * 优先使用当前会话用户；无会话时保留非 admin 字面量；否则回退 system（后台任务等）。
     */
    public static String resolveOperateUser(String legacyUser) {
        String current = getUsername();
        if (current != null) {
            return current;
        }
        if (legacyUser != null && !legacyUser.isEmpty() && !"admin".equals(legacyUser)) {
            return legacyUser;
        }
        return FALLBACK_OPERATOR;
    }

    private static HttpSession getSession(boolean create) {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (!(attrs instanceof ServletRequestAttributes)) {
            return null;
        }
        HttpServletRequest request = ((ServletRequestAttributes) attrs).getRequest();
        return request.getSession(create);
    }
}
