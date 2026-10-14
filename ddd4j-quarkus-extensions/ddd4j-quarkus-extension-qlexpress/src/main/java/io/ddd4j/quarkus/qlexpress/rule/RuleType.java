package io.ddd4j.quarkus.qlexpress.rule;

/**
 * 规则用途分类。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
public enum RuleType {
    /** 决策类规则（条件判断输出 true/false）。 */
    DECISION,
    /** 校验类规则（入参合法性校验）。 */
    VALIDATION,
    /** 计算类规则（表达式求值输出结果）。 */
    CALCULATION
}
