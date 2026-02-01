package io.openrudder.spring.boot.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "openrudder")
public class OpenRudderProperties {
    
    private boolean enabled = true;
    private boolean autoStart = true;
    
    private Map<String, SourceProperties> sources = new HashMap<>();
    private Map<String, QueryProperties> queries = new HashMap<>();
    private Map<String, ReactionProperties> reactions = new HashMap<>();
    
    @Data
    public static class SourceProperties {
        private String type;
        private boolean enabled = true;
        private Map<String, Object> config = new HashMap<>();
    }
    
    @Data
    public static class QueryProperties {
        private String query;
        private String[] sourceIds;
        private boolean enabled = true;
    }
    
    @Data
    public static class ReactionProperties {
        private String type;
        private String[] queryIds;
        private boolean enabled = true;
        private Map<String, Object> config = new HashMap<>();
    }
}
