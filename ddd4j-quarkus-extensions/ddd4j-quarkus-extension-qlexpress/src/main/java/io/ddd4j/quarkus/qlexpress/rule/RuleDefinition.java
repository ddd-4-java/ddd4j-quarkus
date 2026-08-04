package io.ddd4j.quarkus.qlexpress.rule;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 规则管理使用的可持久化规则定义（Quarkus 版，与 boot 版保持一致）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
public class RuleDefinition implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String code;
    private String name;
    private String expression;
    private String description;
    private RuleType type;
    private Boolean enabled = true;
    private Integer priority = 0;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public RuleDefinition() {
    }

    public RuleDefinition(String id, String code, String name, String expression, String description,
                          RuleType type, Boolean enabled, Integer priority,
                          LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.expression = expression;
        this.description = description;
        this.type = type;
        this.enabled = enabled;
        this.priority = priority;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RuleType getType() {
        return type;
    }

    public void setType(RuleType type) {
        this.type = type;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 规则是否可用。
     *
     * @return true 表示启用
     */
    public boolean isAvailable() {
        return Boolean.TRUE.equals(enabled);
    }

    public static class Builder {

        private String id;
        private String code;
        private String name;
        private String expression;
        private String description;
        private RuleType type;
        private Boolean enabled = true;
        private Integer priority = 0;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(String value) {
            id = value;
            return this;
        }

        public Builder code(String value) {
            code = value;
            return this;
        }

        public Builder name(String value) {
            name = value;
            return this;
        }

        public Builder expression(String value) {
            expression = value;
            return this;
        }

        public Builder description(String value) {
            description = value;
            return this;
        }

        public Builder type(RuleType value) {
            type = value;
            return this;
        }

        public Builder enabled(Boolean value) {
            enabled = value;
            return this;
        }

        public Builder priority(Integer value) {
            priority = value;
            return this;
        }

        public Builder createdAt(LocalDateTime value) {
            createdAt = value;
            return this;
        }

        public Builder updatedAt(LocalDateTime value) {
            updatedAt = value;
            return this;
        }

        public RuleDefinition build() {
            return new RuleDefinition(id, code, name, expression, description, type, enabled, priority,
                    createdAt, updatedAt);
        }
    }
}
