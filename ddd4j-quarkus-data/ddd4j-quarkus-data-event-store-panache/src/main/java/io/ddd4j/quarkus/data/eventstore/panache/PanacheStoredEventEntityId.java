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

import java.io.Serializable;
import java.util.Objects;

/**
 * {@link PanacheStoredEventEntity} 的复合主键类。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 4.0.x
 */
public class PanacheStoredEventEntityId implements Serializable {

    private String aggregateId;
    private long version;

    public PanacheStoredEventEntityId() {
    }

    public PanacheStoredEventEntityId(String aggregateId, long version) {
        this.aggregateId = aggregateId;
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PanacheStoredEventEntityId that)) {
            return false;
        }
        return version == that.version && Objects.equals(aggregateId, that.aggregateId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aggregateId, version);
    }
}
