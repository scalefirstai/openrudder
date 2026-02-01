package io.openrudder.query.engine;

import io.openrudder.core.model.ChangeEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class QueryMatcher {

    private final String query;
    private final Map<String, String> entityTypeFilters;
    private final Map<String, Object> fieldFilters;

    public QueryMatcher(String query) {
        this.query = query;
        this.entityTypeFilters = new HashMap<>();
        this.fieldFilters = new HashMap<>();
        parseQuery(query);
    }

    private void parseQuery(String query) {
        Pattern matchPattern = Pattern.compile("MATCH\\s+\\(\\w+:(\\w+)\\)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = matchPattern.matcher(query);
        if (matcher.find()) {
            entityTypeFilters.put("entityType", matcher.group(1));
        }

        Pattern wherePattern = Pattern.compile("WHERE\\s+\\w+\\.(\\w+)\\s*=\\s*['\"]([^'\"]+)['\"]", Pattern.CASE_INSENSITIVE);
        Matcher whereMatcher = wherePattern.matcher(query);
        while (whereMatcher.find()) {
            fieldFilters.put(whereMatcher.group(1), whereMatcher.group(2));
        }
    }

    public boolean matches(ChangeEvent event) {
        if (entityTypeFilters.containsKey("entityType")) {
            String expectedType = entityTypeFilters.get("entityType");
            if (!expectedType.equalsIgnoreCase(event.getEntityType())) {
                return false;
            }
        }

        Map<String, Object> data = event.getData();
        if (data == null) {
            return fieldFilters.isEmpty();
        }

        for (Map.Entry<String, Object> filter : fieldFilters.entrySet()) {
            Object actualValue = data.get(filter.getKey());
            Object expectedValue = filter.getValue();
            
            if (actualValue == null || !actualValue.toString().equals(expectedValue.toString())) {
                return false;
            }
        }

        return true;
    }
}
