package io.ddd4j.quarkus.data.panache;

import io.quarkus.panache.common.Sort;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link RepositoryUtil} 查询构建与排序工具测试。
 *
 * <p>纯单元测试（不启动 Quarkus），覆盖：
 * <ul>
 *   <li>{@link RepositoryUtil#formQuery(Map, Map)} 键前缀解析：
 *       {@code [!]} NOT IN / {@code []} IN / {@code !} 不等 / {@code %} LIKE / {@code >= <= > < =} 比较 / 无前缀等值，
 *       以及命名参数拼接</li>
 *   <li>{@link RepositoryUtil#from(List)} 排序构建：{@code +field} 升序 / {@code -field} 降序 / 无前缀默认升序</li>
 * </ul>
 */
class RepositoryUtilQuarkusTest {

    @Test
    void formQuery_buildsHqlFragmentPerOperatorPrefix() {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("!status", "CANCELLED");
        filters.put("[]ids", List.of(1L, 2L));
        filters.put("[!]excluded", List.of(9L));
        filters.put("%name", "abc");
        filters.put(">=createdAt", "2024-01-01");
        filters.put("<=updatedAt", "2024-12-31");
        filters.put(">count", 5);
        filters.put("<count", 10);
        filters.put("=exact", "x");
        filters.put("plain", "y");

        Map<String, Object> params = new LinkedHashMap<>();
        String fragment = RepositoryUtil.formQuery(filters, params);

        assertThat(fragment).isEqualTo(
                "status <> :status0 AND ids IN :ids1 AND excluded NOT IN :excluded2"
                        + " AND name like :name3 AND createdAt >= :createdAt4"
                        + " AND updatedAt <= :updatedAt5 AND count > :count6"
                        + " AND count < :count7 AND exact = :exact8 AND plain = :plain9");
        // LIKE 前缀会被包装成 %value%
        assertThat(params.get("name3")).isEqualTo("%abc%");
        // 其余参数原样绑定
        assertThat(params.get("status0")).isEqualTo("CANCELLED");
        assertThat(params.get("ids1")).isEqualTo(List.of(1L, 2L));
        assertThat(params.get("excluded2")).isEqualTo(List.of(9L));
        assertThat(params.get("createdAt4")).isEqualTo("2024-01-01");
        assertThat(params.get("updatedAt5")).isEqualTo("2024-12-31");
        assertThat(params.get("count6")).isEqualTo(5);
        assertThat(params.get("count7")).isEqualTo(10);
        assertThat(params.get("exact8")).isEqualTo("x");
        assertThat(params.get("plain9")).isEqualTo("y");
    }

    @Test
    void formQuery_withEmptyFilters_returnsEmptyFragment() {
        assertThat(RepositoryUtil.formQuery(Map.of(), new LinkedHashMap<>())).isEmpty();
    }

    @Test
    void formQuery_singleEqualsFilter() {
        Map<String, Object> params = new LinkedHashMap<>();
        String fragment = RepositoryUtil.formQuery(Map.of("tenantId", "t-1"), params);

        assertThat(fragment).isEqualTo("tenantId = :tenantId0");
        assertThat(params.get("tenantId0")).isEqualTo("t-1");
    }

    @Test
    void from_buildsSortWithDirectionPerPrefix() {
        Sort sort = RepositoryUtil.from(List.of("+name", "-createdAt", "id"));

        List<Sort.Column> columns = sort.getColumns();
        assertThat(columns).extracting(Sort.Column::getName)
                .containsExactly("name", "createdAt", "id");
        assertThat(columns.get(0).getDirection()).isEqualTo(Sort.Direction.Ascending);
        assertThat(columns.get(1).getDirection()).isEqualTo(Sort.Direction.Descending);
        assertThat(columns.get(2).getDirection()).isEqualTo(Sort.Direction.Ascending);
    }

    @Test
    void from_withEmptyList_returnsEmptySort() {
        assertThat(RepositoryUtil.from(List.of()).getColumns()).isEmpty();
    }
}
