package io.openrudder.reactions.webhook;

import io.openrudder.core.reaction.ReactionConfig;
import lombok.Builder;
import lombok.Value;

import java.util.Map;
import java.util.Set;

@Value
@Builder
public class WebhookReactionConfig implements ReactionConfig {
    String name;
    Set<String> queryIds;
    String url;
    String method;
    Map<String, String> headers;
    @Builder.Default
    boolean enabled = true;
    @Builder.Default
    int timeoutSeconds = 30;
}
