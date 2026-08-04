package io.ddd4j.quarkus.mq.core;

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
    public record InitCall(List<MQListener> listeners, MQProperties properties,
                           MQEventSerialization serialization, MQEventStorer storer) {
    }
}
