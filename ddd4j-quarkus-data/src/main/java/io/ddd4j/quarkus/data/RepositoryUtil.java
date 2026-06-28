package io.ddd4j.quarkus.data;

import io.quarkus.panache.common.Sort;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Quarkus Panache 仓储辅助：从排序字段列表构造 {@link Sort}，以及将过滤 Map 转为 HQL/JPQL 片段与命名参数。
 * <p>
 * 对标 ddd4j-data 的 {@code BaseRepositoryImpl}（MyBatis Plus 方案），
 * Quarkus 轨道采用 Hibernate ORM Panache 方案。
 * </p>
 *
 * <h2>过滤键前缀约定</h2>
 * <ul>
 *   <li>{@code [!]} — NOT IN</li>
 *   <li>{@code []} — IN</li>
 *   <li>{@code !} — 不等</li>
 *   <li>{@code %} — LIKE（模糊匹配）</li>
 *   <li>{@code >=} / {@code <=} / {@code >} / {@code <} / {@code =} — 比较</li>
 *   <li>无前缀 — 等值</li>
 * </ul>
 */
public class RepositoryUtil {

    /**
     * 将形如 {@code +field}、{@code -field} 或 {@code field} 的排序列表转为 Panache {@link Sort}。
     *
     * @param sorting 列名列表，前缀 {@code +} 升序、{@code -} 降序，无前缀默认升序
     * @return 组合后的排序对象
     */
    public static Sort from(List<String> sorting) {
        Sort sort = Sort.empty();
        for (String column : sorting) {
            if (column.startsWith("-")) {
                sort.and(column.substring(1), Sort.Direction.Descending);
            } else if (column.startsWith("+")) {
                sort.and(column.substring(1), Sort.Direction.Ascending);
            } else {
                sort.and(column, Sort.Direction.Ascending);
            }
        }
        return sort;
    }

    /**
     * 根据过滤条件 Map 生成 {@code AND} 连接的查询片段，并向 {@code params} 写入绑定参数。
     *
     * @param filters 字段名到取值的映射（键含操作前缀）
     * @param params  输出：命名参数，键为拼接后的占位名
     * @return WHERE 子句片段（不含 WHERE 关键字）
     */
    public static String formQuery(Map<String, Object> filters, Map<String, Object> params) {
        List<String> parts = new ArrayList<>();
        int seq = 0;
        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            String s = entry.getKey();
            Object value = entry.getValue();
            if (s.startsWith("[!]")) {
                String key = s.substring(3);
                parts.add(key + " NOT IN :" + key + seq);
                params.put(key + seq, value);
            } else if (s.startsWith("[]")) {
                String key = s.substring(2);
                parts.add(key + " IN :" + key + seq);
                params.put(key + seq, value);
            } else if (s.startsWith("!")) {
                String key = s.substring(1);
                parts.add(key + " <> :" + key + seq);
                params.put(key + seq, value);
            } else if (s.startsWith("%")) {
                String key = s.substring(1);
                parts.add(key + " like :" + key + seq);
                params.put(key + seq, "%" + value + "%");
            } else if (s.startsWith(">=") || s.startsWith("<=")) {
                String key = s.substring(2);
                parts.add(key + " " + s.substring(0, 2) + " :" + key + seq);
                params.put(key + seq, value);
            } else if (s.startsWith(">") || s.startsWith("=") || s.startsWith("<")) {
                String key = s.substring(1);
                parts.add(key + " " + s.charAt(0) + " :" + key + seq);
                params.put(key + seq, value);
            } else {
                parts.add(s + " = :" + s + seq);
                params.put(s + seq, value);
            }
            seq++;
        }
        return String.join(" AND ", parts);
    }
}
