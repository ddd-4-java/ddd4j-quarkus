package io.ddd4j.quarkus.qlexpress.rule;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Objects;

import java.time.LocalDateTime;

/**
 * 规则变更事件（Quarkus CDI 事件）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
public final class RuleChangedEvent {

    private static final long serialVersionUID = 0L;

    private final String ruleId;

    private final String ruleCode;

    private final Operation operation;

    private final LocalDateTime occurredAt;

    /**
     * 变更操作类型。
     */
    public enum Operation {

        CREATED, UPDATED, DELETED, ENABLED, DISABLED
    }

    public static RuleChangedEvent of(RuleDefinition rule, Operation operation) {
        return new RuleChangedEvent(rule.getId(), rule.getCode(), operation, LocalDateTime.now());
    }

    @JsonCreator()
    public RuleChangedEvent(@JsonProperty("ruleId") String ruleId, @JsonProperty("ruleCode") String ruleCode, @JsonProperty("operation") Operation operation, @JsonProperty("occurredAt") LocalDateTime occurredAt) {
        this.ruleId = ruleId;
        this.ruleCode = ruleCode;
        this.operation = operation;
        this.occurredAt = occurredAt;
    }

    @JsonProperty("ruleId")
    public String ruleId() {
        return ruleId;
    }

    @JsonProperty("ruleCode")
    public String ruleCode() {
        return ruleCode;
    }

    @JsonProperty("operation")
    public Operation operation() {
        return operation;
    }

    @JsonProperty("occurredAt")
    public LocalDateTime occurredAt() {
        return occurredAt;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (Objects.isNull(obj) || getClass() != obj.getClass()) {
            return false;
        }
        RuleChangedEvent other = (RuleChangedEvent) obj;
        return Objects.equals(this.ruleId, other.ruleId) && Objects.equals(this.ruleCode, other.ruleCode) && Objects.equals(this.operation, other.operation) && Objects.equals(this.occurredAt, other.occurredAt);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + Objects.hashCode(ruleId);
        result = 31 * result + Objects.hashCode(ruleCode);
        result = 31 * result + Objects.hashCode(operation);
        result = 31 * result + Objects.hashCode(occurredAt);
        return result;
    }

    @Override
    public String toString() {
        return "RuleChangedEvent[ruleId=" + ruleId + ", ruleCode=" + ruleCode + ", operation=" + operation + ", occurredAt=" + occurredAt + "]";
    }
}
