package io.ddd4j.quarkus.qlexpress.rule;

import java.time.LocalDateTime;

/**
 * 规则变更事件（Quarkus CDI 事件）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
public record RuleChangedEvent(String ruleId, String ruleCode, Operation operation,
                               LocalDateTime occurredAt) {

    /**
     * 变更操作类型。
     */
    public enum Operation {
        CREATED,
        UPDATED,
        DELETED,
        ENABLED,
        DISABLED
    }

    public static RuleChangedEvent of(RuleDefinition rule, Operation operation) {
        return new RuleChangedEvent(rule.getId(), rule.getCode(), operation, LocalDateTime.now());
    }
}
