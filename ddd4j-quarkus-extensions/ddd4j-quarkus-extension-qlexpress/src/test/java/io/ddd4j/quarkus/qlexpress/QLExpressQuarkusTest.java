package io.ddd4j.quarkus.qlexpress;

import io.ddd4j.extension.qlexpress.QLExpressEngine;
import io.ddd4j.extension.qlexpress.model.QLExpressExecutionResult;
import io.ddd4j.quarkus.qlexpress.rule.RuleDefinition;
import io.ddd4j.quarkus.qlexpress.rule.RuleService;
import io.ddd4j.quarkus.qlexpress.rule.RuleType;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link QLExpressCdiProducer} Quarkus 集成测试：验证 QLExpress 引擎、
 * 规则服务与规则仓储的 CDI 装配与真实执行。
 */
@QuarkusTest
class QLExpressQuarkusTest {

    @Inject
    QLExpressEngine engine;

    @Inject
    RuleService ruleService;

    @Test
    void shouldInjectEngineAndEvaluateExpression() {
        assertNotNull(engine);
        Object result = engine.execute("1+2", Map.of());
        assertEquals(3, result);
    }

    @Test
    void shouldEvaluateWithContextVariables() {
        Object result = engine.execute("a+b*2", Map.of("a", 1, "b", 2));
        assertEquals(5, result);
    }

    @Test
    void shouldCreateAndExecuteRule() {
        RuleDefinition rule = new RuleDefinition();
        rule.setId("rule-001");
        rule.setCode("CALC_ADD");
        rule.setName("加法规则");
        rule.setType(RuleType.CALCULATION);
        rule.setExpression("a+b");
        rule.setEnabled(true);

        RuleDefinition created = ruleService.create(rule);
        assertNotNull(created);
        assertTrue(ruleService.findByCode("CALC_ADD").isPresent());

        QLExpressExecutionResult<Object> result = ruleService.execute("CALC_ADD", Map.of("a", 3, "b", 4));
        assertNotNull(result);
        assertTrue(result.success());
        assertEquals(7, result.value());
    }
}
