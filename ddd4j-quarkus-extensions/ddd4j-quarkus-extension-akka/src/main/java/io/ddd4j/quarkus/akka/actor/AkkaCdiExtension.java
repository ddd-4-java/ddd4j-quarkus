package io.ddd4j.quarkus.akka.actor;

import akka.actor.AbstractExtensionId;
import akka.actor.ExtendedActorSystem;
import akka.actor.Extension;
import akka.actor.Props;

/**
 * Akka 扩展：用于创建注册在 Quarkus Arc 容器中的 Actor CDI Bean（Quarkus 版）。
 *
 * <p>对应 boot 模块的 {@code SpringExtension}：boot 版通过
 * {@code io.ddd4j.core.context.Contexts} 查找，Quarkus 版由
 * {@link AkkaCdiActorProducer} 通过 {@code io.quarkus.arc.Arc} 按 Bean 名称查找。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
public class AkkaCdiExtension extends AbstractExtensionId<AkkaCdiExtension.AkkaCdiExt> {

    public static final AkkaCdiExtension AKKA_CDI_EXTENSION_PROVIDER = new AkkaCdiExtension();

    public AkkaCdiExtension() {
    }

    @Override
    public AkkaCdiExt createExtension(ExtendedActorSystem system) {
        return new AkkaCdiExt();
    }

    /**
     * Akka 扩展实例：Actor 实例由 {@link AkkaCdiActorProducer} 通过
     * Quarkus Arc 容器按 Bean 名称查找。
     */
    public static class AkkaCdiExt implements Extension {

        public AkkaCdiExt() {
        }

        /**
         * 创建 Actor Props。
         *
         * @param actorBeanName Actor CDI Bean 名称（{@code @Named} 名称）
         * @return Props 对象
         */
        public Props props(String actorBeanName) {
            return Props.create(AkkaCdiActorProducer.class, actorBeanName);
        }
    }
}
