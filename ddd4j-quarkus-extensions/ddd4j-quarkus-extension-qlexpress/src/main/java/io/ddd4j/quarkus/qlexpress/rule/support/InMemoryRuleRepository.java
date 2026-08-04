package io.ddd4j.quarkus.qlexpress.rule.support;

import io.ddd4j.quarkus.qlexpress.rule.RuleDefinition;
import io.ddd4j.quarkus.qlexpress.rule.RuleRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 无外部存储时使用的进程内规则仓储。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
public final class InMemoryRuleRepository implements RuleRepository {

    private final ConcurrentHashMap<String, RuleDefinition> rulesById = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> idsByCode = new ConcurrentHashMap<>();

    @Override
    public synchronized RuleDefinition save(RuleDefinition rule) {
        RuleDefinition checked = Objects.requireNonNull(rule, "rule 不能为空");
        if (Objects.isNull(checked.getId())) {
            throw new IllegalArgumentException("rule.id 不能为空");
        }
        String existingId = idsByCode.get(checked.getCode());
        if (Objects.nonNull(existingId) && !Objects.equals(existingId, checked.getId())) {
            throw new IllegalArgumentException("规则编码已存在: " + checked.getCode());
        }
        RuleDefinition previous = rulesById.put(checked.getId(), checked);
        if (Objects.nonNull(previous) && !Objects.equals(previous.getCode(), checked.getCode())) {
            idsByCode.remove(previous.getCode());
        }
        idsByCode.put(checked.getCode(), checked.getId());
        return checked;
    }

    @Override
    public Optional<RuleDefinition> findById(String id) {
        if (Objects.isNull(id)) {
            return Optional.empty();
        }
        return Optional.ofNullable(rulesById.get(id));
    }

    @Override
    public Optional<RuleDefinition> findByCode(String code) {
        if (Objects.isNull(code)) {
            return Optional.empty();
        }
        String id = idsByCode.get(code);
        return Objects.isNull(id) ? Optional.empty() : findById(id);
    }

    @Override
    public List<RuleDefinition> findAll() {
        return new ArrayList<>(rulesById.values());
    }

    @Override
    public synchronized void deleteById(String id) {
        RuleDefinition removed = rulesById.remove(id);
        if (Objects.nonNull(removed)) {
            idsByCode.remove(removed.getCode());
        }
    }
}
