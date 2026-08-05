package io.ddd4j.quarkus.sample.common.api;

import io.ddd4j.core.api.Page;

import java.util.List;
import java.util.Objects;

/**
 * 泛型分页响应。
 *
 * <p>面向 API 出参的分页结构，与核心分页对象 {@link Page} 解耦，
 * 避免领域层 / 基础设施层分页模型直接暴露给接口层。</p>
 *
 * @param <T> 记录类型
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
public class PageResponse<T> {

    /**
     * 当前页数据
     */
    private List<T> records;

    /**
     * 总记录数
     */
    private long total;

    /**
     * 当前页码（从 1 开始）
     */
    private long current;

    /**
     * 每页大小
     */
    private long size;

    public PageResponse() {
    }

    public PageResponse(List<T> records, long total, long current, long size) {
        this.records = records;
        this.total = total;
        this.current = current;
        this.size = size;
    }

    /**
     * 从核心分页对象构建分页响应。
     *
     * @param page 核心分页对象
     * @param <T>  记录类型
     * @return 分页响应
     */
    public static <T> PageResponse<T> of(Page<T> page) {
        Objects.requireNonNull(page, "page must not be null");
        return new PageResponse<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 创建空分页响应。
     *
     * @param <T> 记录类型
     * @return 空分页响应
     */
    public static <T> PageResponse<T> empty() {
        return new PageResponse<>(List.of(), 0L, 1L, 10L);
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getCurrent() {
        return current;
    }

    public void setCurrent(long current) {
        this.current = current;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
