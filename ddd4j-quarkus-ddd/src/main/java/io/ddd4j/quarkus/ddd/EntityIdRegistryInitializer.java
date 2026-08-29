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

import io.ddd4j.core.cqrs.eventstore.EventDeserializer;
import io.ddd4j.core.ddd.event.EntityId;
import io.ddd4j.core.ddd.event.EntityIdRegistry;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.function.Function;

/**
 * EntityIdRegistry 自动注册启动监听器。
 *
 * <p>在 Quarkus 启动时（{@link StartupEvent}），从 {@link EntityIdConfig} 读取配置，
 * 实例化工厂类并注册到 {@link EntityIdRegistry}。
 *
 * <p>工厂类必须是 {@code Function<String, EntityId>} 的实现，且提供 public 无参构造器。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 4.0.x
 */
@ApplicationScoped
public class EntityIdRegistryInitializer {

    private static final Logger LOG = Logger.getLogger(EntityIdRegistryInitializer.class);

    @Inject
    EntityIdConfig config;

    /**
     * 启动时注册自定义 EntityId 类型。
     *
     * @param event Quarkus 启动事件
     */
    void onStart(@Observes StartupEvent event) {
        if (!config.enabled().orElse(true)) {
            LOG.debug("EntityIdRegistry auto-registration is disabled");
            return;
        }

        Map<String, String> registry = config.registry();
        if (registry == null || registry.isEmpty()) {
            LOG.debug("No custom EntityId types configured for registration");
            return;
        }

        for (Map.Entry<String, String> entry : registry.entrySet()) {
            String typeName = entry.getKey();
            String factoryClassName = entry.getValue();
            try {
                Function<String, EntityId> factory = instantiateFactory(factoryClassName);
                EntityIdRegistry.register(typeName, factory);
                LOG.infof("Registered EntityId type '%s' with factory %s", typeName, factoryClassName);
            } catch (Exception e) {
                LOG.errorf(e, "Failed to register EntityId type '%s' with factory %s", typeName, factoryClassName);
            }
        }
    }

    /**
     * 实例化工厂类。
     *
     * <p>工厂类名来自外部配置，加载前先经 {@link EventDeserializer#isValidClassName}
     * 校验格式，防止异常输入触发意外类加载；仅允许 public 无参构造器
     * （不使用 setAccessible，避免绕过访问控制及模块系统限制）。
     *
     * @param factoryClassName 工厂类全限定名
     * @return 工厂实例
     * @throws Exception 实例化失败
     */
    @SuppressWarnings("unchecked")
    private Function<String, EntityId> instantiateFactory(String factoryClassName) throws Exception {
        if (!EventDeserializer.isValidClassName(factoryClassName)) {
            throw new IllegalArgumentException("Invalid factory class name format: " + factoryClassName);
        }
        Class<?> factoryClass = Class.forName(factoryClassName);
        if (!Function.class.isAssignableFrom(factoryClass)) {
            throw new IllegalArgumentException(
                    "Factory class must implement Function<String, EntityId>: " + factoryClassName);
        }
        Constructor<?> constructor = factoryClass.getConstructor();
        return (Function<String, EntityId>) constructor.newInstance();
    }
}
