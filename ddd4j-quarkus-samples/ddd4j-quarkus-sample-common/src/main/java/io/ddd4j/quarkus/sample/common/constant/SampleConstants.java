package io.ddd4j.quarkus.sample.common.constant;

/**
 * 示例常量定义。
 *
 * <p>跨模块共享的示例常量，供领域 / 应用 / 接口各层引用，
 * 避免魔法字符串散落。</p>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
public final class SampleConstants {

    /**
     * 默认命名空间（用于 MQ 主题前缀、缓存 Key 前缀等）
     */
    public static final String DEFAULT_NAMESPACE = "ddd4j.quarkus.sample";

    /**
     * 订单领域命名空间
     */
    public static final String ORDER_NAMESPACE = DEFAULT_NAMESPACE + ".order";

    /**
     * 订单领域事件主题前缀（事件类 TOPIC 常量约定使用该前缀）
     */
    public static final String ORDER_TOPIC_PREFIX = "ORDER";

    /**
     * 默认页码
     */
    public static final long DEFAULT_PAGE_CURRENT = 1L;

    /**
     * 默认每页大小
     */
    public static final long DEFAULT_PAGE_SIZE = 10L;

    private SampleConstants() {
    }
}
