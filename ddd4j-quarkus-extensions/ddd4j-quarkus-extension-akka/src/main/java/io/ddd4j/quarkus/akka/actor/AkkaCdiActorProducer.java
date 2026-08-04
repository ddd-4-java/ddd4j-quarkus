package io.ddd4j.quarkus.akka.actor;

import akka.actor.Actor;
import akka.actor.IndirectActorProducer;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;

/**
 * 该类由 Akka 的扩展用于创建 Actor 实例（Quarkus CDI 版）。
 *
 * <p>对应 boot 模块的 {@code SpringActorProducer}：boot 版通过
 * {@code io.ddd4j.core.context.Contexts} 按 SPI key 查找，Quarkus 版改为通过
 * {@link Arc} 容器按 CDI Bean 名称（{@code @Named} 名称）查找，无任何框架耦合。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
public final class AkkaCdiActorProducer implements IndirectActorProducer {

    /**
     * 保留的 Actor 命名前缀（与 boot 版 {@code ACTOR_BEAN_KEY_PREFIX} 对齐，便于迁移）。
     */
    public static final String ACTOR_BEAN_PREFIX = "ddd4j.akka.actor.";

    private final String beanActorName;

    /**
     * 构造函数。
     *
     * @param beanActorName Actor CDI Bean 名称
     */
    public AkkaCdiActorProducer(String beanActorName) {
        this.beanActorName = beanActorName;
    }

    /**
     * 创建 Actor 实例。
     *
     * @return Actor 实例
     */
    @Override
    public Actor produce() {
        InstanceHandle<Object> handle = Arc.container().instance(beanActorName);
        Object instance = handle.get();
        if (!(instance instanceof Actor actor)) {
            throw new IllegalStateException(
                    "Actor bean not found or not an Actor: name=" + beanActorName
                            + ". Ensure the actor is registered as a CDI bean named '" + beanActorName + "'.");
        }
        return actor;
    }

    /**
     * 获取 Actor 类类型。
     * <p>
     * 通过 Arc 容器已注册的实例解析其运行时类型。
     *
     * @return Actor 的 Class 对象
     */
    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends Actor> actorClass() {
        Object instance = Arc.container().instance(beanActorName).get();
        return instance == null ? Actor.class : (Class<? extends Actor>) instance.getClass();
    }
}
