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

    public static Map<String, Object> getById(String businessCode, String entityName, String id, String pkName) {
        String pk = normalizePkName(pkName);
        return list(businessCode, entityName).stream()
                .filter(row -> rowPkEquals(row, id, pk))
                .findFirst()
                .orElse(null);
    }

    public static void add(String businessCode, String entityName, Map<String, Object> body, String pkName) {
        String pk = normalizePkName(pkName);
        Map<String, Object> row = new java.util.LinkedHashMap<>(body);
        if (!row.containsKey("id")) {
            long next = ID_SEQ.incrementAndGet();
            row.put("id", next);
            if (!pk.equals("id") && !row.containsKey(pk)) {
                row.put(pk, next);
            }
        } else if (!pk.equals("id") && !row.containsKey(pk)) {
            row.put(pk, row.get("id"));
        }
        list(businessCode, entityName).add(0, row);
    }

    public static void add(String businessCode, String entityName, Map<String, Object> body) {
        add(businessCode, entityName, body, "id");
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

    public static boolean update(String businessCode, String entityName, Map<String, Object> body, String pkName) {
        String pk = normalizePkName(pkName);
        Object idVal = resolvePkFromBody(body, pk);
        if (idVal == null) {
            return false;
        }
        List<Map<String, Object>> rows = list(businessCode, entityName);
        for (int i = 0; i < rows.size(); i++) {
            if (rowPkEquals(rows.get(i), idVal.toString(), pk)) {
                Map<String, Object> merged = new java.util.LinkedHashMap<>(rows.get(i));
                merged.putAll(body);
                if (!merged.containsKey("id") && merged.containsKey(pk)) {
                    merged.put("id", merged.get(pk));
                }
                rows.set(i, merged);
                return true;
            }
        }
        return false;
    }

    public static boolean delete(String businessCode, String entityName, String id, String pkName) {
        String pk = normalizePkName(pkName);
        return list(businessCode, entityName).removeIf(row -> rowPkEquals(row, id, pk));
    }

    public static int batchDelete(String businessCode, String entityName, List<?> ids, String pkName) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        String pk = normalizePkName(pkName);
        java.util.Set<String> idSet = ids.stream().map(String::valueOf).collect(Collectors.toSet());
        List<Map<String, Object>> rows = list(businessCode, entityName);
        int before = rows.size();
        rows.removeIf(row -> idSet.stream().anyMatch(id -> rowPkEquals(row, id, pk)));
        return before - rows.size();
    }

    /** 返回 true 表示已存在（与生成表单中「重复」语义一致） */
    public static boolean checkUnique(String businessCode, String entityName, Map<String, Object> params, String pkName) {
        String pk = normalizePkName(pkName);
        Object excludeId = params.get(pk);
        if (excludeId == null) {
            excludeId = params.get("id");
        }
        for (Map.Entry<String, Object> e : params.entrySet()) {
            if (pk.equals(e.getKey()) || "id".equals(e.getKey()) || e.getValue() == null
                    || String.valueOf(e.getValue()).isEmpty()) {
                continue;
            }
            String field = e.getKey();
            String value = String.valueOf(e.getValue());
            Object finalExcludeId = excludeId;
            boolean exists = list(businessCode, entityName).stream().anyMatch(row -> {
                if (finalExcludeId != null && rowPkEquals(row, finalExcludeId.toString(), pk)) {
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

    private static String normalizePkName(String pkName) {
        return pkName == null || pkName.isBlank() ? "id" : pkName.trim();
    }

    private static Object resolvePkFromBody(Map<String, Object> body, String pkName) {
        if (body == null) {
            return null;
        }
        Object v = body.get(pkName);
        if (v == null && !"id".equals(pkName)) {
            v = body.get("id");
        }
        return v;
    }

    private static boolean rowPkEquals(Map<String, Object> row, String id, String pkName) {
        if (id == null) {
            return false;
        }
        if (id.equals(String.valueOf(row.get("id")))) {
            return true;
        }
        if (!"id".equals(pkName) && id.equals(String.valueOf(row.get(pkName)))) {
            return true;
        }
        return false;
    }
}
