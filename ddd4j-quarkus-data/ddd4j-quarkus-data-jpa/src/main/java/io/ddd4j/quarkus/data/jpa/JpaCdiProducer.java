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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * JPA CDI Producer。
 *
 * <p>暴露 {@link EntityManager} 为 CDI Bean，供 ddd4j 数据层组件注入。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 4.0.x
 */
@ApplicationScoped
public class JpaCdiProducer {

    @PersistenceContext
    EntityManager entityManager;

    /**
     * 暴露 EntityManager 为 CDI Bean。
     *
     * @return EntityManager 实例
     */
    @Produces
    @ApplicationScoped
    public EntityManager entityManager() {
        return entityManager;
    }
}
