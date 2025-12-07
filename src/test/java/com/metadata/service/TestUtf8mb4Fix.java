package com.metadata.service;

import com.metadata.service.CheckConstraintParser;

import java.util.List;

/**
 * Test the fix for _utf8mb4 prefix removal and redundant options field in IN constraint values
 */
public class TestUtf8mb4Fix {

    public static void main(String[] args) {
        // Create CheckConstraintParser instance
        CheckConstraintParser checkConstraintParser = new CheckConstraintParser();
        
        // Test case 1: IN constraint with _utf8mb4 prefix
        System.out.println("=== Test IN Constraint with _utf8mb4 Prefix ===");
        String inValuesStr = "('_utf8mb4文学', '_utf8mb4科技', '_utf8mb4教育', '_utf8mb4历史', '_utf8mb4艺术')";
        System.out.println("Input: " + inValuesStr);
        
        // Extract the values part from the parentheses
        String valuesOnly = inValuesStr.substring(1, inValuesStr.length() - 1);
        List<String> valuesList = checkConstraintParser.parseInValues(valuesOnly);
        
        System.out.println("Parsed values:");
        for (String value : valuesList) {
            System.out.println("  - " + value);
        }
        
        // Verify the fix
        boolean hasUtf8mb4Prefix = valuesList.stream().anyMatch(value -> value.startsWith("_utf8mb4"));
        System.out.println("\nFix verification:");
        System.out.println("Has _utf8mb4 prefix: " + hasUtf8mb4Prefix);
        System.out.println("Expected: false");
        System.out.println("Fix successful: " + !hasUtf8mb4Prefix);
        
        // Test case 2: Test parseCheckConstraint method for IN constraint
        System.out.println("\n=== Test parseCheckConstraint for IN Constraint ===");
        String checkConstraint = "CHECK (`BOOK_CATEGORY` IN ('_utf8mb4文学', '_utf8mb4科技', '_utf8mb4教育', '_utf8mb4历史', '_utf8mb4艺术'))";
        System.out.println("Input: " + checkConstraint);
        
        String parseResult = checkConstraintParser.parseCheckConstraint(checkConstraint);
        System.out.println("Parsed result: " + parseResult);
        
        // Verify the fix for redundant options field
        boolean hasOptionsField = parseResult.contains("options");
        System.out.println("\nFix verification for redundant options field:");
        System.out.println("Has options field: " + hasOptionsField);
        System.out.println("Expected: false");
        System.out.println("Fix successful: " + !hasOptionsField);
        
        // Test case 3: Single value with _utf8mb4 prefix
        System.out.println("\n=== Test Single Value with _utf8mb4 Prefix ===");
        String singleValueStr = "_utf8mb4'测试值'";
        List<String> singleValueList = checkConstraintParser.parseInValues(singleValueStr);
        System.out.println("Input: " + singleValueStr);
        System.out.println("Parsed value: " + singleValueList.get(0));
        System.out.println("Fix successful: " + !singleValueList.get(0).startsWith("_utf8mb4"));
        
        System.out.println("\n=== All Tests Completed ===");
    }
}
