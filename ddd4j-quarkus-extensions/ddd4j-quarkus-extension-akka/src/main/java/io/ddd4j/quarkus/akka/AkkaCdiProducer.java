package io.ddd4j.quarkus.akka;

import akka.actor.ActorSystem;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * Akka Quarkus CDI Producer（替代 boot 版的 {@code Ddd4jAkkaBootAutoConfiguration}）。
 *
 * <p>装配 {@link ActorSystem} Bean：由 {@link AkkaFactory} 创建，容器销毁时
 * {@code @Disposes} 关闭（{@code terminate()}）。
 *
 * <p>Actor 实例的查找由 {@link io.ddd4j.quarkus.akka.actor.AkkaCdiActorProducer} /
 * {@link io.ddd4j.quarkus.akka.actor.AkkaCdiExtension} 通过 Quarkus Arc 容器
 * （{@link io.quarkus.arc.Arc}）按 CDI Bean 名称完成。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@ApplicationScoped
@IfBuildProperty(name = "ddd4j.akka.enabled", stringValue = "true", enableIfMissing = true)
public class AkkaCdiProducer {

    /**
     * 装配 ActorSystem。
     *
     * @param config Akka 配置
     * @return ActorSystem 实例
     */
    @Produces
    @Singleton
    public ActorSystem actorSystem(AkkaConfig config) {
        return new AkkaFactory().actorSystem(config.name());
    }

    /**
     * 容器销毁时关闭 ActorSystem。
     *
     * @param actorSystem ActorSystem 实例
     */
    public void close(@Disposes ActorSystem actorSystem) {
        actorSystem.terminate();
    }
}
