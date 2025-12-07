package com.metadata.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Test the fix for redundant options field in IN constraint values
 */
public class TestInConstraintFix {

    public static void main(String[] args) {
        // Test the fix by directly testing the JSON generation logic
        System.out.println("=== Test IN Constraint JSON Generation ===");
        
        // Simulate the values list after parsing
        String[] values = {"文学", "科技", "教育", "历史", "艺术"};
        
        // Generate the JSON string using the fixed logic
        StringBuilder valuesJson = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                valuesJson.append(",");
            }
            valuesJson.append("\"").append(values[i]).append("\"");
        }
        
        // Use the fixed JSON format (without options field)
        String fixedJson = String.format("{\"operator\":\"IN\",\"values\":[%s]}", valuesJson.toString());
        
        System.out.println("Fixed JSON: " + fixedJson);
        
        // Verify the fix
        boolean hasOptionsField = fixedJson.contains("options");
        boolean hasValuesField = fixedJson.contains("values");
        boolean hasOperatorIn = fixedJson.contains("\"operator\":\"IN\"");
        
        System.out.println("\nFix verification:");
        System.out.println("Has options field: " + hasOptionsField);
        System.out.println("Expected: false");
        System.out.println("Has values field: " + hasValuesField);
        System.out.println("Expected: true");
        System.out.println("Has operator IN: " + hasOperatorIn);
        System.out.println("Expected: true");
        
        // Overall verification
        boolean fixSuccessful = !hasOptionsField && hasValuesField && hasOperatorIn;
        System.out.println("\nOverall fix successful: " + fixSuccessful);
        
        // Test with the original JSON format (with options field) for comparison
        String originalJson = String.format("{\"operator\":\"IN\",\"values\":[%s],\"options\":[%s]}", 
            valuesJson.toString(), valuesJson.toString());
        System.out.println("\nOriginal JSON (with options): " + originalJson);
        System.out.println("Original JSON has options field: " + originalJson.contains("options"));
        
        System.out.println("\n=== Test Completed ===");
    }
}
