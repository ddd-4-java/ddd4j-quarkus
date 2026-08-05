package io.ddd4j.quarkus.akka;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link AkkaCdiProducer} Quarkus 集成测试：验证 ActorSystem 的 CDI 装配
 * 与 actor 创建能力。
 */
@QuarkusTest
class AkkaQuarkusTest {

    @Inject
    ActorSystem actorSystem;

    @Test
    void shouldInjectActorSystem() {
        assertNotNull(actorSystem);
        assertTrue(actorSystem.name().startsWith("ddd4j"));
    }

    @Test
    void shouldCreateActorFromProps() {
        ActorRef ref = actorSystem.actorOf(Props.create(EchoActor.class), "echo-actor");
        assertNotNull(ref);
        assertTrue(ref.path().name().startsWith("echo-actor"));
    }

    /**
     * 最小 echo actor：验证 Props 创建链路。
     */
    public static class EchoActor extends AbstractActor {

        @Override
        public Receive createReceive() {
            return receiveBuilder().build();
        }
    }
}
