package io.ddd4j.quarkus.akka;

import akka.actor.ActorSystem;

import java.util.Objects;

/**
 * Akka 工厂（纯 Java，无框架依赖），提供 {@link ActorSystem} 创建逻辑。
 *
 * <p>对应 boot 模块的 {@code AkkaAutoConfiguration} 工厂类。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
public class AkkaFactory {

    public AkkaFactory() {
    }

    /**
     * 创建并配置 ActorSystem。
     *
     * @param name Actor system 名称
     * @return ActorSystem 实例
     */
    public ActorSystem actorSystem(String name) {
        return ActorSystem.create(Objects.requireNonNull(name, "name 不能为空"));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        return o instanceof AkkaFactory;
    }

    @Override
    public int hashCode() {
        return Objects.hash(AkkaFactory.class);
    }

    @Override
    public String toString() {
        return "AkkaFactory()";
    }
}
