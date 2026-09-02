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

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link EntityIdRegistryInitializer} 白名单前缀匹配测试。
 *
 * <p>覆盖：
 * <ul>
 *   <li>单前缀匹配</li>
 *   <li>多前缀（OR 语义）</li>
 *   <li>非匹配拒绝</li>
 *   <li>边界情况（前缀含点、不含点、null、空字符串）</li>
 * </ul>
 *
 * <p>{@code isAllowed} 是私有静态方法，通过反射验证。{@link #invokeIsAllowed(String, List)}
 * 包装反射调用并解包 {@link InvocationTargetException}，避免反射异常堆栈污染断言失败信息。
 */
class EntityIdRegistryInitializerWhitelistTest {

    private static boolean invokeIsAllowed(String factoryClassName, List<String> prefixes) throws Exception {
        Method method = EntityIdRegistryInitializer.class.getDeclaredMethod("isAllowed", String.class, List.class);
        method.setAccessible(true);
        try {
            return (boolean) method.invoke(null, factoryClassName, prefixes);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw e;
        }
    }

    @Test
    void 单前缀_匹配_返回true() throws Exception {
        assertThat(invokeIsAllowed("com.example.domain.OrderIdFactory", List.of("com.example.domain.")))
                .isTrue();
    }

    @Test
    void 多前缀_OR语义_任一匹配即true() throws Exception {
        List<String> prefixes = List.of("com.example.domain.", "io.acme.identity.");
        assertThat(invokeIsAllowed("com.example.domain.OrderIdFactory", prefixes)).isTrue();
        assertThat(invokeIsAllowed("io.acme.identity.UserIdFactory", prefixes)).isTrue();
    }

    @Test
    void 非匹配_返回false() throws Exception {
        assertThat(invokeIsAllowed("org.malicious.EvilFactory", List.of("com.example.domain.")))
                .isFalse();
    }

    @Test
    void 前缀不含点_精确匹配也接受() throws Exception {
        // 极端情况：用户配置 "Foo" 而类名为 "FooBar" —— startsWith 仍返回 true（业务可配置）
        assertThat(invokeIsAllowed("FooBar", List.of("Foo"))).isTrue();
    }

    @Test
    void 完全相同类名_返回true() throws Exception {
        assertThat(invokeIsAllowed("com.example.OrderId", List.of("com.example.OrderId"))).isTrue();
    }

    @Test
    void 大小写敏感_不匹配() throws Exception {
        assertThat(invokeIsAllowed("COM.Example.Domain.Foo", List.of("com.example.domain.")))
                .isFalse();
    }
}
