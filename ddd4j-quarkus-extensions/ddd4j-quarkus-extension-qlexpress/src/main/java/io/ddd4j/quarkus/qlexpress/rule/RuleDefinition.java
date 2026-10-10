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

    /**
     * 无参构造函数，供序列化框架（JDK/Jackson/持久化）反射实例化。
     */
    public RuleDefinition() {
    }

    /**
     * 全参构造函数。
     *
     * @param id          规则唯一标识
     * @param code        规则编码
     * @param name        规则名称
     * @param expression  规则表达式
     * @param description 规则描述
     * @param type        规则类型
     * @param enabled     是否启用
     * @param priority    优先级
     * @param createdAt   创建时间
     * @param updatedAt   最后更新时间
     */
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

    /**
     * 获取规则唯一标识。
     *
     * @return 规则唯一标识
     */
    public String getId() {
        return id;
    }

    /**
     * 设置规则唯一标识。
     *
     * @param id 规则唯一标识
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * 获取规则编码。
     *
     * @return 规则编码
     */
    public String getCode() {
        return code;
    }

    /**
     * 设置规则编码。
     *
     * @param code 规则编码
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * 获取规则名称。
     *
     * @return 规则名称
     */
    public String getName() {
        return name;
    }

    /**
     * 设置规则名称。
     *
     * @param name 规则名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取规则表达式。
     *
     * @return 规则表达式
     */
    public String getExpression() {
        return expression;
    }

    /**
     * 设置规则表达式。
     *
     * @param expression 规则表达式
     */
    public void setExpression(String expression) {
        this.expression = expression;
    }

    /**
     * 获取规则描述。
     *
     * @return 规则描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 设置规则描述。
     *
     * @param description 规则描述
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 获取规则类型。
     *
     * @return 规则类型
     */
    public RuleType getType() {
        return type;
    }

    /**
     * 设置规则类型。
     *
     * @param type 规则类型
     */
    public void setType(RuleType type) {
        this.type = type;
    }

    /**
     * 获取是否启用。
     *
     * @return 是否启用
     */
    public Boolean getEnabled() {
        return enabled;
    }

    /**
     * 设置是否启用。
     *
     * @param enabled 是否启用
     */
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 获取优先级。
     *
     * @return 优先级
     */
    public Integer getPriority() {
        return priority;
    }

    /**
     * 设置优先级。
     *
     * @param priority 优先级
     */
    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    /**
     * 获取创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置创建时间。
     *
     * @param createdAt 创建时间
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 获取最后更新时间。
     *
     * @return 最后更新时间
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 设置最后更新时间。
     *
     * @param updatedAt 最后更新时间
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * 创建规则定义构建器。
     *
     * @return 新建的构建器实例
     */
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

    /**
     * {@link RuleDefinition} 的流式构建器。
     */
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

        /**
         * 设置规则唯一标识。
         *
         * @param value 规则唯一标识
         * @return 当前构建器
         */
        public Builder id(String value) {
            id = value;
            return this;
        }

        /**
         * 设置规则编码。
         *
         * @param value 规则编码
         * @return 当前构建器
         */
        public Builder code(String value) {
            code = value;
            return this;
        }

        /**
         * 设置规则名称。
         *
         * @param value 规则名称
         * @return 当前构建器
         */
        public Builder name(String value) {
            name = value;
            return this;
        }

        /**
         * 设置规则表达式。
         *
         * @param value 规则表达式
         * @return 当前构建器
         */
        public Builder expression(String value) {
            expression = value;
            return this;
        }

        /**
         * 设置规则描述。
         *
         * @param value 规则描述
         * @return 当前构建器
         */
        public Builder description(String value) {
            description = value;
            return this;
        }

        /**
         * 设置规则类型。
         *
         * @param value 规则类型
         * @return 当前构建器
         */
        public Builder type(RuleType value) {
            type = value;
            return this;
        }

        /**
         * 设置是否启用。
         *
         * @param value 是否启用
         * @return 当前构建器
         */
        public Builder enabled(Boolean value) {
            enabled = value;
            return this;
        }

        /**
         * 设置优先级。
         *
         * @param value 优先级
         * @return 当前构建器
         */
        public Builder priority(Integer value) {
            priority = value;
            return this;
        }

        /**
         * 设置创建时间。
         *
         * @param value 创建时间
         * @return 当前构建器
         */
        public Builder createdAt(LocalDateTime value) {
            createdAt = value;
            return this;
        }

        /**
         * 设置最后更新时间。
         *
         * @param value 最后更新时间
         * @return 当前构建器
         */
        public Builder updatedAt(LocalDateTime value) {
            updatedAt = value;
            return this;
        }

        /**
         * 依据当前构建器状态构建规则定义。
         *
         * @return 新建的规则定义实例
         */
        public RuleDefinition build() {
            return new RuleDefinition(id, code, name, expression, description, type, enabled, priority,
                    createdAt, updatedAt);
        }
    }
}
