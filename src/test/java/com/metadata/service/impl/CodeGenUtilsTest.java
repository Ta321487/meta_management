package com.metadata.service.impl;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试CodeGenUtils类的关键字处理逻辑
 */
public class CodeGenUtilsTest {

    @Test
    public void testConvertToClassNameWithKeyword() {
        // 测试表名为CLASS_TABLE的情况
        String result = CodeGenUtils.convertToClassName("CLASS_TABLE");
        // 预期结果应该是_Class，因为class是Java关键字
        assertEquals("_Class", result);
        System.out.println("测试convertToClassNameWithKeyword通过：CLASS_TABLE -> " + result);
    }

    @Test
    public void testConvertToEntityNameWithKeyword() {
        // 测试表名为CLASS_TABLE的情况
        String result = CodeGenUtils.convertToEntityName("CLASS_TABLE");
        // 预期结果应该是_class，因为class是Java关键字
        assertEquals("_class", result);
        System.out.println("测试convertToEntityNameWithKeyword通过：CLASS_TABLE -> " + result);
    }

    @Test
    public void testConvertToCamelCaseWithKeyword() {
        // 测试直接转换class关键字的情况
        String result = CodeGenUtils.convertToCamelCase("class", false);
        // 预期结果应该是_class，因为class是Java关键字
        assertEquals("_class", result);
        System.out.println("测试convertToCamelCaseWithKeyword通过：class -> " + result);
    }

    @Test
    public void testConvertToCamelCaseWithNonKeyword() {
        // 测试转换非关键字的情况
        String result = CodeGenUtils.convertToCamelCase("student_name", false);
        // 预期结果应该是studentName，不需要添加前缀
        assertEquals("studentName", result);
        System.out.println("测试convertToCamelCaseWithNonKeyword通过：student_name -> " + result);
    }

    @Test
    public void testParseValidationRulePatternKind() {
        Map<String, Object> lineNo = CodeGenUtils.parseValidationRule(
                "{\"type\":\"number\",\"integer\":true,\"min\":1,\"message\":\"行号>=1\",\"trigger\":\"blur\"}");
        assertEquals("integer", lineNo.get("patternKind"));
        assertTrue((Boolean) lineNo.get("hasRange"));

        Map<String, Object> qty = CodeGenUtils.parseValidationRule(
                "{\"type\":\"number\",\"min\":0.0001,\"message\":\"数量>0\",\"trigger\":\"blur\"}");
        assertEquals("builtinNumber", qty.get("patternKind"));
        assertTrue((Boolean) qty.get("hasRange"));

        Map<String, Object> phone = CodeGenUtils.parseValidationRule(
                "{\"pattern\":\"^1[3-9]\\\\d{9}$\",\"message\":\"手机号格式不正确\"}");
        assertEquals("custom", phone.get("patternKind"));
    }

    @Test
    public void testConvertToClassNameWithNonKeyword() {
        // 测试表名为STUDENT_TABLE的情况
        String result = CodeGenUtils.convertToClassName("STUDENT_TABLE");
        // 预期结果应该是Student，不需要添加前缀
        assertEquals("Student", result);
        System.out.println("测试convertToClassNameWithNonKeyword通过：STUDENT_TABLE -> " + result);
    }
}
