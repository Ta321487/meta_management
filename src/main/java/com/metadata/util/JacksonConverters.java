package com.metadata.util;

import com.fasterxml.jackson.databind.util.StdConverter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Jackson序列化反序列化工具类
 * 提供常用的类型转换器
 */
public class JacksonConverters {

    /**
     * List转String转换器（用于序列化到数据库）
     * 将List集合转换为逗号分隔的字符串
     */
    public static class ListToStringConverter extends StdConverter<List<String>, String> {
        @Override
        public String convert(List<String> value) {
            if (value == null || value.isEmpty()) {
                return "";
            }
            return String.join(",", value);
        }
    }

    /**
     * String转List转换器（用于从数据库反序列化）
     * 将逗号分隔的字符串转换为List集合
     */
    public static class StringToListConverter extends StdConverter<String, List<String>> {
        @Override
        public List<String> convert(String value) {
            if (value == null || value.trim().isEmpty()) {
                return Collections.emptyList();
            }
            return Arrays.stream(value.split(",")).collect(Collectors.toList());
        }
    }
}