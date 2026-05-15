package com.metadata.util;

/**
 * 在保持 MySQL JDBC URL 其它参数不变的前提下，替换路径中的 catalog（库名）。
 */
public final class JdbcCatalogUrlRewriter {

    private JdbcCatalogUrlRewriter() {
    }

    /**
     * 从 jdbc:mysql://host:port/db?params 中解析当前 catalog；解析失败返回 null。
     */
    public static String extractCatalog(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isEmpty()) {
            return null;
        }
        int scheme = jdbcUrl.indexOf("://");
        if (scheme < 0) {
            return null;
        }
        int slash = jdbcUrl.indexOf('/', scheme + 3);
        if (slash < 0 || slash >= jdbcUrl.length() - 1) {
            return null;
        }
        int q = jdbcUrl.indexOf('?', slash + 1);
        String segment = q > 0 ? jdbcUrl.substring(slash + 1, q) : jdbcUrl.substring(slash + 1);
        return segment.isEmpty() ? null : segment;
    }

    /**
     * 将 URL 中的 catalog 替换为 newCatalog；若无法识别结构则原样返回。
     */
    public static String replaceCatalog(String jdbcUrl, String newCatalog) {
        if (jdbcUrl == null || jdbcUrl.isEmpty() || newCatalog == null || newCatalog.isBlank()) {
            return jdbcUrl;
        }
        String catalog = newCatalog.trim();
        int scheme = jdbcUrl.indexOf("://");
        if (scheme < 0) {
            return jdbcUrl;
        }
        int slash = jdbcUrl.indexOf('/', scheme + 3);
        int q = jdbcUrl.indexOf('?', scheme + 3);
        String suffix = "";
        if (q > 0) {
            suffix = jdbcUrl.substring(q);
        }
        if (slash < 0) {
            return jdbcUrl + "/" + catalog + suffix;
        }
        return jdbcUrl.substring(0, slash + 1) + catalog + suffix;
    }

    public static boolean sameCatalog(String jdbcUrl, String candidateCatalog) {
        if (candidateCatalog == null || candidateCatalog.isBlank()) {
            return false;
        }
        String current = extractCatalog(jdbcUrl);
        return current != null && current.equalsIgnoreCase(candidateCatalog.trim());
    }
}
