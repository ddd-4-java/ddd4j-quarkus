package io.ddd4j.quarkus.akka;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Akka Quarkus 配置（替代 boot 版的 {@code AkkaProperties}）。
 *
 * <p>前缀 {@code ddd4j.akka}，通过 SmallRye {@link ConfigMapping} 映射。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
@ConfigMapping(prefix = "ddd4j.akka")
public interface AkkaConfig {

    /**
     * Actor system 名称。
     */
    @WithDefault("ddd4j-akka-system")
    String name();

    /**
     * 是否启用 Akka 集成。
     */
    @WithDefault("true")
    boolean enabled();
}
