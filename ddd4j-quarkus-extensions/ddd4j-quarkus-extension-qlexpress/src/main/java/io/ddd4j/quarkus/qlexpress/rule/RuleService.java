package io.ddd4j.quarkus.qlexpress.rule;

import io.ddd4j.extension.qlexpress.QLExpressEngine;
import io.ddd4j.extension.qlexpress.model.QLExpressExecutionResult;
import io.ddd4j.extension.qlexpress.model.QLExpressValidationResult;
import io.ddd4j.kit.lang.StrKit;
import jakarta.enterprise.event.Event;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * 规则 CRUD、缓存协调、执行和事件发布服务（Quarkus 版）。
 *
 * <p>与 boot 版差异：boot 使用 Spring {@code ApplicationEventPublisher} 发布
 * {@link RuleChangedEvent}，Quarkus 版改为 CDI {@link Event}（{@code fire} 发布）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
public class RuleService {

    private final RuleRepository repository;
    private final RuleCache cache;
    private final QLExpressEngine engine;
    private final Event<RuleChangedEvent> eventPublisher;

    public RuleService(RuleRepository repository, RuleCache cache, QLExpressEngine engine,
                       Event<RuleChangedEvent> eventPublisher) {
        this.repository = Objects.requireNonNull(repository, "repository 不能为空");
        this.cache = Objects.requireNonNull(cache, "cache 不能为空");
        this.engine = Objects.requireNonNull(engine, "engine 不能为空");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher 不能为空");
    }

    /**
     * 创建规则。
     *
     * @param rule 规则定义
     * @return 已保存的规则
     */
    public RuleDefinition create(RuleDefinition rule) {
        RuleDefinition checked = requireValidRule(rule);
        if (repository.findByCode(checked.getCode()).isPresent()) {
            throw new IllegalArgumentException("规则编码已存在: " + checked.getCode());
        }
        LocalDateTime now = LocalDateTime.now();
        if (!StrKit.hasText(checked.getId())) {
            checked.setId(UUID.randomUUID().toString());
        }
        checked.setCreatedAt(now);
        checked.setUpdatedAt(now);
        RuleDefinition saved = repository.save(checked);
        cache.put(saved.getCode(), saved);
        publish(saved, RuleChangedEvent.Operation.CREATED);
        return saved;
    }

    /**
     * 更新规则。
     *
     * @param id      规则 ID
     * @param changes 变更内容（编码不可修改）
     * @return 已保存的规则
     */
    public RuleDefinition update(String id, RuleDefinition changes) {
        requireText(id, "id");
        RuleDefinition existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("规则不存在: " + id));
        RuleDefinition checked = Objects.requireNonNull(changes, "changes 不能为空");
        if (StrKit.hasText(checked.getCode()) && !Objects.equals(existing.getCode(), checked.getCode())) {
            throw new IllegalArgumentException("规则编码不允许修改: " + existing.getCode());
        }
        checked.setCode(existing.getCode());
        requireValidRule(checked);
        existing.setName(checked.getName());
        existing.setExpression(checked.getExpression());
        existing.setDescription(checked.getDescription());
        existing.setType(checked.getType());
        existing.setEnabled(Objects.nonNull(checked.getEnabled()) ? checked.getEnabled() : existing.getEnabled());
        existing.setPriority(Objects.nonNull(checked.getPriority()) ? checked.getPriority() : existing.getPriority());
        existing.setUpdatedAt(LocalDateTime.now());
        RuleDefinition saved = repository.save(existing);
        cache.put(saved.getCode(), saved);
        publish(saved, RuleChangedEvent.Operation.UPDATED);
        return saved;
    }

    /**
     * 删除规则。
     *
     * @param id 规则 ID
     */
    public void delete(String id) {
        requireText(id, "id");
        RuleDefinition existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("规则不存在: " + id));
        repository.deleteById(id);
        cache.evict(existing.getCode());
        publish(existing, RuleChangedEvent.Operation.DELETED);
    }

    /**
     * 启用规则。
     *
     * @param id 规则 ID
     * @return 已保存的规则
     */
    public RuleDefinition enable(String id) {
        return changeAvailability(id, true, RuleChangedEvent.Operation.ENABLED);
    }

    /**
     * 禁用规则。
     *
     * @param id 规则 ID
     * @return 已保存的规则
     */
    public RuleDefinition disable(String id) {
        return changeAvailability(id, false, RuleChangedEvent.Operation.DISABLED);
    }

    /**
     * 按 ID 查找规则。
     *
     * @param id 规则 ID
     * @return 规则（可能为空）
     */
    public Optional<RuleDefinition> findById(String id) {
        if (!StrKit.hasText(id)) {
            return Optional.empty();
        }
        return repository.findById(id);
    }

    /**
     * 按编码查找规则（走缓存）。
     *
     * @param code 规则编码
     * @return 规则（可能为空）
     */
    public Optional<RuleDefinition> findByCode(String code) {
        requireText(code, "code");
        RuleDefinition cached = cache.get(code);
        if (Objects.nonNull(cached)) {
            return Optional.of(cached);
        }
        Optional<RuleDefinition> rule = repository.findByCode(code);
        rule.ifPresent(value -> cache.put(code, value));
        return rule;
    }

    /**
     * 查询全部规则（按优先级倒序）。
     *
     * @return 规则列表
     */
    public List<RuleDefinition> findAll() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(
                        rule -> Objects.nonNull(rule.getPriority()) ? rule.getPriority() : 0,
                        Comparator.reverseOrder()))
                .toList();
    }

    /**
     * 按编码执行规则表达式。
     *
     * @param code    规则编码
     * @param context 执行上下文
     * @return 执行结果
     */
    public QLExpressExecutionResult<Object> execute(String code, Map<String, Object> context) {
        Optional<RuleDefinition> rule = findByCode(code);
        if (rule.isEmpty()) {
            return QLExpressExecutionResult.failure("RULE_NOT_FOUND", "规则不存在: " + code, 0L);
        }
        if (!rule.get().isAvailable()) {
            return QLExpressExecutionResult.failure("RULE_DISABLED", "规则已禁用: " + code, 0L);
        }
        return engine.executeSafely(rule.get().getExpression(), context);
    }

    /**
     * 清空规则缓存。
     */
    public void clearCache() {
        cache.clear();
    }

    private RuleDefinition changeAvailability(String id, boolean enabled,
                                              RuleChangedEvent.Operation operation) {
        requireText(id, "id");
        RuleDefinition rule = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("规则不存在: " + id));
        rule.setEnabled(enabled);
        rule.setUpdatedAt(LocalDateTime.now());
        RuleDefinition saved = repository.save(rule);
        if (enabled) {
            cache.put(saved.getCode(), saved);
        } else {
            cache.evict(saved.getCode());
        }
        publish(saved, operation);
        return saved;
    }

    private RuleDefinition requireValidRule(RuleDefinition rule) {
        RuleDefinition checked = Objects.requireNonNull(rule, "rule 不能为空");
        requireText(checked.getCode(), "rule.code");
        requireText(checked.getName(), "rule.name");
        requireText(checked.getExpression(), "rule.expression");
        Objects.requireNonNull(checked.getType(), "rule.type 不能为空");
        QLExpressValidationResult validation = engine.validate(checked.getExpression());
        if (!validation.valid()) {
            throw new IllegalArgumentException("规则表达式无效: " + validation.message());
        }
        return checked;
    }

    private void publish(RuleDefinition rule, RuleChangedEvent.Operation operation) {
        eventPublisher.fire(RuleChangedEvent.of(rule, operation));
    }

    private static void requireText(String value, String field) {
        if (!StrKit.hasText(value)) {
            throw new IllegalArgumentException(field + " 不能为空");
        }
    }
}
