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

    /**
     * 构造 PageResponse 实例。
     */
    public PageResponse() {
    }

    /**
     * 构造 PageResponse 实例。
     * @param records  记录
     * @param total  总数
     * @param current  参数
     * @param size  大小
     */
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

    /**
     * 获取记录。
     * @return 记录
     */
    public List<T> getRecords() {
        return records;
    }

    /**
     * 设置记录。
     * @param records  记录
     */
    public void setRecords(List<T> records) {
        this.records = records;
    }

    /**
     * 获取总数。
     * @return 总数
     */
    public long getTotal() {
        return total;
    }

    /**
     * 设置总数。
     * @param total  总数
     */
    public void setTotal(long total) {
        this.total = total;
    }

    /**
     * 获取 getCurrent。
     * @return 获取Current
     */
    public long getCurrent() {
        return current;
    }

    /**
     * 设置 setCurrent 参数。
     * @param current  参数
     */
    public void setCurrent(long current) {
        this.current = current;
    }

    /**
     * 获取大小。
     * @return 大小
     */
    public long getSize() {
        return size;
    }

    /**
     * 设置大小。
     * @param size  大小
     */
    public void setSize(long size) {
        this.size = size;
    }
}
