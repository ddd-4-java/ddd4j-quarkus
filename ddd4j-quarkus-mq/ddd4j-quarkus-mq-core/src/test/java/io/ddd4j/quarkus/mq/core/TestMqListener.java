package io.ddd4j.quarkus.mq.core;

import io.ddd4j.mq.annotation.MQEventListener;
import io.ddd4j.mq.event.MQEvent;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * 测试夹具：标注了 {@link MQEventListener} 方法的 CDI Bean，
 * 供 {@link QuarkusMQListenerRegistrar#scanListeners()} 扫描（topic=TEST, tags=A）。
 */
@ApplicationScoped
public class TestMqListener {

    @MQEventListener(topic = "TEST", tags = "A")
    public void onTestEvent(MQEvent event) {
        // 测试用监听方法，不会被真正消费
    }
}
