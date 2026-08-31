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
package io.ddd4j.quarkus.data.eventstore.panache.arch;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * ddd4j-quarkus-data-event-store-panache 模块独立性自检。
 *
 * <p>确保 Quarkus Panache 实现不引入 Spring 或 JPA 直接依赖。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 4.0.x
 */
@AnalyzeClasses(packages = "io.ddd4j.quarkus.data.eventstore.panache", importOptions = {ImportOption.DoNotIncludeTests.class})
class EventStorePanacheModuleIndependenceTest {

    /**
     * Panache 模块不得直接依赖 Spring Framework。
     */
    @ArchTest
    static final ArchRule no_spring_in_panache_event_store =
            noClasses().that().resideInAPackage("io.ddd4j.quarkus.data.eventstore.panache..")
                    .should().dependOnClassesThat().resideInAPackage("org.springframework..");

    /**
     * Panache 模块不得直接依赖 Jakarta Persistence（JPA 映射归 Panache 处理）。
     */
    @ArchTest
    static final ArchRule no_jakarta_persistence_in_panache_event_store =
            noClasses().that().resideInAPackage("io.ddd4j.quarkus.data.eventstore.panache..")
                    .should().dependOnClassesThat().resideInAPackage("jakarta.persistence..");
}
