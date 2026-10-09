package io.ddd4j.quarkus.qlexpress.rule;

import java.time.LocalDateTime;

/**
 * 规则变更事件（Quarkus CDI 事件）。
 *
 * @param occurredAt  参数
 * @param operation  操作
 * @param ruleCode  规则编码
 * @param ruleId  规则标识
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
public record RuleChangedEvent(String ruleId, String ruleCode, Operation operation,
                               LocalDateTime occurredAt) {

    /**
     * 变更操作类型。
     */
    public enum Operation {
        /**
         * CREATED 枚举常量。
         */
        CREATED,
        /**
         * UPDATED 枚举常量。
         */
        UPDATED,
        /**
         * DELETED 枚举常量。
         */
        DELETED,
        /**
         * ENABLED 枚举常量。
         */
        ENABLED,
        /**
         * DISABLED 枚举常量。
         */
        DISABLED
    }

    /**
     * 执行 of 操作。
     * @param rule  规则
     * @param operation  操作
     * @return 方法返回结果
     */
    public static RuleChangedEvent of(RuleDefinition rule, Operation operation) {
        return new RuleChangedEvent(rule.getId(), rule.getCode(), operation, LocalDateTime.now());
    }
}
