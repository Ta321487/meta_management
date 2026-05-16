package com.metadata.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 业务系统预览用内存 Mock 数据（按 businessCode + entityName 隔离，重启后清空）
 */
public final class CodegenPreviewMockStore {

    private static final ConcurrentHashMap<String, List<Map<String, Object>>> TABLES = new ConcurrentHashMap<>();
    private static final AtomicLong ID_SEQ = new AtomicLong(1000);

    private CodegenPreviewMockStore() {
    }

    static String key(String businessCode, String entityName) {
        return businessCode + ":" + entityName;
    }

    public static List<Map<String, Object>> list(String businessCode, String entityName) {
        return TABLES.computeIfAbsent(key(businessCode, entityName), k -> new ArrayList<>());
    }

    public static void resetBusiness(String businessCode) {
        TABLES.keySet().removeIf(k -> k.startsWith(businessCode + ":"));
    }

    public static Map<String, Object> page(String businessCode, String entityName, int page, int size) {
        List<Map<String, Object>> all = new ArrayList<>(list(businessCode, entityName));
        int total = all.size();
        int from = Math.max(0, (page - 1) * size);
        int to = Math.min(total, from + size);
        List<Map<String, Object>> records = from >= total ? List.of() : all.subList(from, to);
        return Map.of(
                "records", records,
                "total", total,
                "current", page,
                "size", size
        );
    }

    public static Map<String, Object> getById(String businessCode, String entityName, String id) {
        return list(businessCode, entityName).stream()
                .filter(row -> id.equals(String.valueOf(row.get("id"))))
                .findFirst()
                .orElse(null);
    }

    public static void add(String businessCode, String entityName, Map<String, Object> body) {
        Map<String, Object> row = new java.util.LinkedHashMap<>(body);
        if (!row.containsKey("id")) {
            row.put("id", ID_SEQ.incrementAndGet());
        }
        list(businessCode, entityName).add(0, row);
    }

    /**
     * 批量写入示例数据（预览「填充示例」）
     *
     * @return 写入条数
     */
    public static int seedRows(String businessCode, String entityName,
                               List<Map<String, Object>> rows, boolean clearFirst) {
        List<Map<String, Object>> target = list(businessCode, entityName);
        if (clearFirst) {
            target.clear();
        }
        if (rows == null || rows.isEmpty()) {
            return 0;
        }
        int n = 0;
        for (Map<String, Object> body : rows) {
            Map<String, Object> row = new java.util.LinkedHashMap<>(body);
            if (!row.containsKey("id")) {
                row.put("id", ID_SEQ.incrementAndGet());
            }
            target.add(row);
            n++;
        }
        return n;
    }

    public static boolean update(String businessCode, String entityName, Map<String, Object> body) {
        Object id = body.get("id");
        if (id == null) {
            return false;
        }
        List<Map<String, Object>> rows = list(businessCode, entityName);
        for (int i = 0; i < rows.size(); i++) {
            if (id.toString().equals(String.valueOf(rows.get(i).get("id")))) {
                Map<String, Object> merged = new java.util.LinkedHashMap<>(rows.get(i));
                merged.putAll(body);
                rows.set(i, merged);
                return true;
            }
        }
        return false;
    }

    public static boolean delete(String businessCode, String entityName, String id) {
        return list(businessCode, entityName).removeIf(row -> id.equals(String.valueOf(row.get("id"))));
    }

    public static int batchDelete(String businessCode, String entityName, List<?> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        java.util.Set<String> idSet = ids.stream().map(String::valueOf).collect(Collectors.toSet());
        List<Map<String, Object>> rows = list(businessCode, entityName);
        int before = rows.size();
        rows.removeIf(row -> idSet.contains(String.valueOf(row.get("id"))));
        return before - rows.size();
    }

    /** 返回 true 表示已存在（与生成表单中「重复」语义一致） */
    public static boolean checkUnique(String businessCode, String entityName, Map<String, Object> params, String pkName) {
        Object excludeId = params.get(pkName);
        for (Map.Entry<String, Object> e : params.entrySet()) {
            if (pkName.equals(e.getKey()) || e.getValue() == null || String.valueOf(e.getValue()).isEmpty()) {
                continue;
            }
            String field = e.getKey();
            String value = String.valueOf(e.getValue());
            boolean exists = list(businessCode, entityName).stream().anyMatch(row -> {
                if (excludeId != null && excludeId.toString().equals(String.valueOf(row.get("id")))) {
                    return false;
                }
                return value.equals(String.valueOf(row.get(field)));
            });
            if (exists) {
                return true;
            }
        }
        return false;
    }
}
