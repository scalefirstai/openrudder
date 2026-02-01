package io.openrudder.core.query;

import io.openrudder.core.model.ChangeEvent;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class SimpleQueryEvaluator {
    
    private static final Pattern WHERE_PATTERN = Pattern.compile(
        "WHERE\\s+(.+?)(?:RETURN|$)", 
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    private static final Pattern RETURN_PATTERN = Pattern.compile(
        "RETURN\\s+(.+?)$", 
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    private static final Pattern ENTITY_PATTERN = Pattern.compile(
        "MATCH\\s+\\((\\w+):(\\w+)\\)",
        Pattern.CASE_INSENSITIVE
    );

    public static boolean evaluateConditions(ChangeEvent event, String query) {
        try {
            Matcher whereMatcher = WHERE_PATTERN.matcher(query);
            if (!whereMatcher.find()) {
                return true;
            }
            
            String whereClause = whereMatcher.group(1).trim();
            String[] conditions = whereClause.split("\\s+AND\\s+");
            
            for (String condition : conditions) {
                if (!evaluateCondition(event, condition.trim())) {
                    return false;
                }
            }
            
            return true;
        } catch (Exception e) {
            log.error("Error evaluating query conditions", e);
            return false;
        }
    }
    
    private static boolean evaluateCondition(ChangeEvent event, String condition) {
        condition = condition.trim();
        
        if (condition.startsWith("NOT EXISTS")) {
            return evaluateNotExists(event, condition);
        }
        
        if (condition.contains("=")) {
            return evaluateEquality(event, condition);
        }
        
        return true;
    }
    
    private static boolean evaluateNotExists(ChangeEvent event, String condition) {
        Pattern notExistsPattern = Pattern.compile("NOT EXISTS\\s*\\(\\s*(\\w+)\\.(\\w+)\\s*\\)");
        Matcher matcher = notExistsPattern.matcher(condition);
        
        if (matcher.find()) {
            String fieldName = matcher.group(2);
            Object value = event.getFieldValue(fieldName);
            return value == null || (value instanceof Boolean && !((Boolean) value));
        }
        
        return true;
    }
    
    private static boolean evaluateEquality(ChangeEvent event, String condition) {
        String[] parts = condition.split("=");
        if (parts.length != 2) {
            return true;
        }
        
        String left = parts[0].trim();
        String right = parts[1].trim().replace("'", "").replace("\"", "");
        
        String fieldName = left.contains(".") ? left.split("\\.")[1] : left;
        Object actualValue = event.getFieldValue(fieldName);
        
        if (actualValue == null) {
            return false;
        }
        
        return actualValue.toString().equals(right);
    }
    
    public static Map<String, Object> extractReturnFields(ChangeEvent event, String query) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Matcher returnMatcher = RETURN_PATTERN.matcher(query);
            if (!returnMatcher.find()) {
                result.putAll(event.getData());
                return result;
            }
            
            String returnClause = returnMatcher.group(1).trim();
            String[] fields = returnClause.split(",");
            
            for (String field : fields) {
                field = field.trim();
                String fieldName = field.contains(".") ? field.split("\\.")[1] : field;
                Object value = event.getFieldValue(fieldName);
                if (value != null) {
                    result.put(fieldName, value);
                }
            }
        } catch (Exception e) {
            log.error("Error extracting return fields", e);
            result.putAll(event.getData());
        }
        
        return result;
    }
    
    public static QueryResult createQueryResult(ChangeEvent event, String queryId, String query) {
        Map<String, Object> data = extractReturnFields(event, query);
        
        return QueryResult.builder()
            .id(UUID.randomUUID().toString())
            .queryId(queryId)
            .data(data)
            .timestamp(Instant.now())
            .metadata(Map.of(
                "sourceEventId", event.getId(),
                "eventType", event.getType().toString()
            ))
            .build();
    }
}
