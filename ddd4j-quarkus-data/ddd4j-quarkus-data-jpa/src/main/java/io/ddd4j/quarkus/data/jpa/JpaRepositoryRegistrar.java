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
package io.ddd4j.quarkus.data.jpa;

import io.ddd4j.core.ddd.repository.Repository;
import io.ddd4j.core.ddd.repository.RepositoryRegistry;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * JPA Repository 注册器。
 *
 * <p>在 Quarkus 启动时，扫描所有 CDI Bean 中的 {@link Repository} 实现，
 * 注册到 {@link RepositoryRegistry}。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x（backport from 4.0.x）
 */
@ApplicationScoped
public class JpaRepositoryRegistrar {

    private static final Logger LOG = Logger.getLogger(JpaRepositoryRegistrar.class);

    @Inject
    Instance<Repository<?, ?>> repositories;

    /**
     * 启动时注册所有 Repository Bean。
     *
     * @param event Quarkus 启动事件
     */
    void onStart(@Observes StartupEvent event) {
        int count = 0;
        for (Repository<?, ?> repository : repositories) {
            try {
                registerUnchecked(repository);
                LOG.infof("Registered repository: %s", repository.getClass().getName());
                count++;
            } catch (Exception e) {
                LOG.errorf(e, "Failed to register repository: %s", repository.getClass().getName());
            }
        }
        LOG.infof("Total registered repositories: %d", count);
    }

    /**
     * 泛型捕获辅助：绕过 {@code Repository<?, ?>} 通配符与
     * {@code RepositoryRegistry.register(Class<M>, Repository<M>)} 的类型推断不兼容问题。
     */
    @SuppressWarnings("unchecked")
    private static void registerUnchecked(Repository<?, ?> repository) {
        RepositoryRegistry.register(
                (Class) repository.getClass(),
                (Repository) repository
        );
    }
}
