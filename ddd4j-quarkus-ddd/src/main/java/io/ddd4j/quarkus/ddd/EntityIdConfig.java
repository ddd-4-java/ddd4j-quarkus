/*
 * Copyright (c) 2024-2026 ddd4j project. All rights reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.ddd4j.quarkus.ddd;

import io.ddd4j.core.ddd.event.EntityIdRegistry;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * EntityIdRegistry 自动注册配置。
 *
 * <p>业务方通过配置注册自定义 EntityId 类型：
 * <pre>{@code
 * ddd4j.entity-id-registry.OrderId=com.example.domain.OrderIdFactory
 * ddd4j.entity-id-registry.CustomerId=com.example.domain.CustomerIdFactory
 * # 白名单：仅允许 com.example.domain.* 与 io.acme.identity.* 下的工厂类
 * ddd4j.entity-id-registry.factory-class-prefixes[0]=com.example.domain.
 * ddd4j.entity-id-registry.factory-class-prefixes[1]=io.acme.identity.
 * }</pre>
 *
 * <p>每个配置项的值必须是 {@code Function<String, EntityId>} 的实现类全限定名。
 *
 * <h3>GraalVM native-image</h3>
 * <p>工厂类在运行时经反射实例化（类名来自配置，build 时不可枚举），构建 native image
 * 前业务方需在 {@code application.properties} 注册反射（包名替换为工厂类所在包）：
 * <pre>{@code
 * quarkus.native.reflection.include-patterns=com.example.domain.*
 * }</pre>
 * 未注册时启动日志按条目报 {@code Factory class not found ...}（不阻断启动）。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 4.0.x
 */
@ConfigMapping(prefix = "ddd4j.entity-id-registry")
public interface EntityIdConfig {

    /**
     * EntityId 类型注册表（类型名 → 工厂类全限定名）。
     *
     * @return 注册表映射
     */
    Map<String, String> registry();

    /**
     * 是否启用自动注册（默认 true）。
     *
     * @return 启用时 {@code true}
     */
    @WithName("enabled")
    Optional<Boolean> enabled();

    /**
     * 工厂类白名单前缀列表。
     *
     * <p>仅当配置了至少一个前缀时启用白名单校验。任一前缀匹配即视为允许（OR 语义）。
     * 未配置时 {@link io.ddd4j.quarkus.ddd.EntityIdRegistryInitializer#onStart}
     * 仅做格式校验（{@link io.ddd4j.core.cqrs.eventstore.EventDeserializer#isValidClassName}），
     * 跳过白名单——与 ddd4j-core {@code EventDeserializer} 默认行为保持一致。
     *
     * @return 白名单前缀列表
     */
    @WithName("factory-class-prefixes")
    List<String> factoryClassPrefixes();
}
