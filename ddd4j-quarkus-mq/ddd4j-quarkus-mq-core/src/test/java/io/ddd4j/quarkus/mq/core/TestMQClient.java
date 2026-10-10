package io.ddd4j.quarkus.mq.core;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Objects;

import io.ddd4j.mq.MQClient;
import io.ddd4j.mq.MQProperties;
import io.ddd4j.mq.event.MQEvent;
import io.ddd4j.mq.event.MQEventSerialization;
import io.ddd4j.mq.event.MQEventStorer;
import io.ddd4j.mq.listener.MQListener;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 测试夹具：{@link ApplicationScoped} 的伪 {@link MQClient}。
 *
 * <p>{@code impl()} 固定返回 {@code "disruptor"}，用于验证
 * {@link QuarkusMQListenerRegistrar#findActiveClient()} 在配置 broker=disruptor 时能选中本 Bean。
 * 记录每次 {@link #init(List, MQProperties, MQEventSerialization, MQEventStorer)} 调用的入参，
 * 供测试断言「监听器被扫描到并委托给活跃 client」。
 */
@ApplicationScoped
public class TestMQClient implements MQClient {

    private final List<InitCall> initCalls = new ArrayList<>();

    @Override
    public String impl() {
        return "disruptor";
    }

    @Override
    public void init(List<MQListener> listeners, MQProperties properties,
                     MQEventSerialization serialization, MQEventStorer storer) {
        synchronized (this) {
            initCalls.add(new InitCall(List.copyOf(listeners), properties, serialization, storer));
        }
    }

    @Override
    public Consumer<MQEvent> initProducer(MQProperties properties) {
        return event -> {
        };
    }

    @Override
    public boolean initConsumer(MQListener listener, MQProperties properties) {
        return true;
    }

    public synchronized void reset() {
        initCalls.clear();
    }

    public synchronized List<InitCall> initCalls() {
        return List.copyOf(initCalls);
    }

    public synchronized int initCount() {
        return initCalls.size();
    }

    /**
     * 一次 {@link MQClient#init} 调用的入参快照。
     */
    public final static class InitCall {

        private static final long serialVersionUID = 0L;

        private final List<MQListener> listeners;

        private final MQProperties properties;

        private final MQEventSerialization serialization;

        private final MQEventStorer storer;

        @JsonCreator()
        public InitCall(@JsonProperty("listeners") List<MQListener> listeners, @JsonProperty("properties") MQProperties properties, @JsonProperty("serialization") MQEventSerialization serialization, @JsonProperty("storer") MQEventStorer storer) {
            this.listeners = listeners;
            this.properties = properties;
            this.serialization = serialization;
            this.storer = storer;
        }

        @JsonProperty("listeners")
        public List<MQListener> listeners() {
            return listeners;
        }

        @JsonProperty("properties")
        public MQProperties properties() {
            return properties;
        }

        @JsonProperty("serialization")
        public MQEventSerialization serialization() {
            return serialization;
        }

        @JsonProperty("storer")
        public MQEventStorer storer() {
            return storer;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (Objects.isNull(obj) || getClass() != obj.getClass()) {
                return false;
            }
            InitCall other = (InitCall) obj;
            return Objects.equals(this.listeners, other.listeners) && Objects.equals(this.properties, other.properties) && Objects.equals(this.serialization, other.serialization) && Objects.equals(this.storer, other.storer);
        }

        @Override
        public int hashCode() {
            int result = 0;
            result = 31 * result + Objects.hashCode(listeners);
            result = 31 * result + Objects.hashCode(properties);
            result = 31 * result + Objects.hashCode(serialization);
            result = 31 * result + Objects.hashCode(storer);
            return result;
        }

        @Override
        public String toString() {
            return "InitCall[listeners=" + listeners + ", properties=" + properties + ", serialization=" + serialization + ", storer=" + storer + "]";
        }
    }
}
