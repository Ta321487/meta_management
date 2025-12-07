package com.metadata.service;

import com.metadata.service.CheckConstraintParser;

/**
 * Test parseCheckConstraint method directly
 */
public class ParseCheckConstraintTest {

    public static void main(String[] args) throws Exception {
        // Create CheckConstraintParser instance
        CheckConstraintParser checkConstraintParser = new CheckConstraintParser();
        
        // 1. Test regex constraint
        System.out.println("=== Test Regex Constraint ===");
        String regexConstraint1 = "CHECK (`STU_CODE` REGEXP '^\\d{10}$')";
        String regexResult1 = checkConstraintParser.parseCheckConstraint(regexConstraint1);
        System.out.println("Input: " + regexConstraint1);
        System.out.println("Output: " + regexResult1);
        System.out.println("Contains pattern: " + (regexResult1 != null && regexResult1.contains("pattern")));
        System.out.println();
        
        String regexConstraint2 = "CHECK (regexp_like(`STU_CODE`,_utf8mb4'^\\d{10}$'))";
        String regexResult2 = checkConstraintParser.parseCheckConstraint(regexConstraint2);
        System.out.println("Input: " + regexConstraint2);
        System.out.println("Output: " + regexResult2);
        System.out.println("Contains pattern: " + (regexResult2 != null && regexResult2.contains("pattern")));
        System.out.println();
        
        // 2. Test BETWEEN AND constraint
        System.out.println("=== Test BETWEEN AND Constraint ===");
        String betweenConstraint = "CHECK (`STU_AGE` BETWEEN 1 AND 100)";
        String betweenResult = checkConstraintParser.parseCheckConstraint(betweenConstraint);
        System.out.println("Input: " + betweenConstraint);
        System.out.println("Output: " + betweenResult);
        System.out.println("Contains field: " + (betweenResult != null && betweenResult.contains("field")));
        System.out.println("Contains min: " + (betweenResult != null && betweenResult.contains("min")));
        System.out.println("Contains max: " + (betweenResult != null && betweenResult.contains("max")));
        System.out.println();
        
        // 3. Test IN constraint
        System.out.println("=== Test IN Constraint ===");
        String inConstraint = "CHECK (`STU_GENDER` IN (_utf8mb4'male',_utf8mb4'female'))";
        String inResult = checkConstraintParser.parseCheckConstraint(inConstraint);
        System.out.println("Input: " + inConstraint);
        System.out.println("Output: " + inResult);
        System.out.println("Contains field: " + (inResult != null && inResult.contains("field")));
        System.out.println("Contains operator: " + (inResult != null && inResult.contains("operator")));
        System.out.println("Contains values: " + (inResult != null && inResult.contains("values")));
        System.out.println();
        
        System.out.println("=== Test Completed ===");
    }
}