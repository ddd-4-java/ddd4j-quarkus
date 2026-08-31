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
package io.ddd4j.quarkus.data.eventstore.panache;

import io.ddd4j.core.cqrs.eventstore.EventStore;
import io.ddd4j.core.cqrs.eventstore.StoredEvent;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PanacheEventStore 集成测试（H2 内存库）。
 *
 * <p>验证 core {@link EventStore} SPI 的完整契约：乐观锁、顺序追加、全局事件流。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 4.0.x
 */
@QuarkusTest
class PanacheEventStoreIT {

    @Inject
    EventStore eventStore;

    @Test
    void appendThenReadShouldReturnEventsInVersionOrder() {
        String aggregateId = "panache-it-1";

        TestEvent first = new TestEvent("first");
        TestEvent second = new TestEvent("second");
        TestEvent third = new TestEvent("third");

        eventStore.append(aggregateId, List.of(first, second, third), 0L);

        List<StoredEvent> stored = eventStore.read(aggregateId);

        assertThat(stored).hasSize(3);
        assertThat(stored).extracting(StoredEvent::version).containsExactly(0L, 1L, 2L);
        assertThat(stored).extracting(StoredEvent::aggregateId).containsOnly(aggregateId);
    }

    @Test
    void appendWithWrongExpectedVersionShouldThrow() {
        String aggregateId = "panache-it-2";

        eventStore.append(aggregateId, List.of(new TestEvent("first")), 0L);

        assertThatThrownBy(() -> eventStore.append(aggregateId, List.of(new TestEvent("dup")), 0L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expected 0")
                .hasMessageContaining("was 1");
    }

    @Test
    void readAllShouldReturnEventsInPositionOrder() {
        String aggregateId1 = "panache-it-3";
        String aggregateId2 = "panache-it-4";

        eventStore.append(aggregateId1, List.of(new TestEvent("a1")), 0L);
        eventStore.append(aggregateId2, List.of(new TestEvent("a2")), 0L);
        eventStore.append(aggregateId1, List.of(new TestEvent("a3")), 1L);

        List<StoredEvent> stored = eventStore.readAll(0L, 100);

        assertThat(stored).hasSizeGreaterThanOrEqualTo(3);
        assertThat(stored).extracting(StoredEvent::position).isSorted();
    }

    @Test
    void eventPayloadShouldSurviveRoundTrip() {
        String aggregateId = "panache-it-5";

        eventStore.append(aggregateId, List.of(new TestEvent("payload")), 0L);

        List<StoredEvent> stored = eventStore.read(aggregateId);

        assertThat(stored).hasSize(1);
        Object event = stored.get(0).event();
        // EventDeserializer 按 event_type 还原强类型
        assertThat(event).isInstanceOf(TestEvent.class);
        assertThat(((TestEvent) event).getName()).isEqualTo("payload");
    }

    /**
     * 测试事件载荷（简单 POJO，core EventStore 的 payload 为 Object）。
     */
    public static class TestEvent {

        private String name;

        public TestEvent() {
        }

        public TestEvent(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
