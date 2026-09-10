package io.ddd4j.quarkus.sample.auth.satoken;

import io.ddd4j.core.auth.AuthPrincipal;
import io.ddd4j.core.util.SubjectKit;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 仅存在于测试源码集的认证上下文隔离探针。
 */
@Path("/auth/test/isolation")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class AuthIsolationProbeResource {

    private final ConcurrentMap<String, Observation> observations = new ConcurrentHashMap<>();

    @Inject
    CurrentVertxRequest currentRequest;

    /**
     * 在读取当前认证主体后主动失败，并记录同一个 Vert.x 请求的结束回调。
     *
     * @param requestId 测试请求标识
     * @param expectedLoginId 预期的当前登录标识
     */
    @GET
    @Path("/fail")
    public void fail(@QueryParam("requestId") String requestId,
                     @QueryParam("expectedLoginId") String expectedLoginId) {
        AuthPrincipal principal = SubjectKit.getPrincipal();
        String loginId = Objects.isNull(principal) ? null : String.valueOf(principal.getLoginId());
        if (!Objects.equals(expectedLoginId, loginId)) {
            throw new IllegalStateException("unexpected authenticated principal");
        }
        Observation observation = new Observation(loginId);
        observations.put(requestId, observation);
        currentRequest.getCurrent().addEndHandler(ignored -> observation.requestEnded.set(true));
        throw new IllegalStateException("sample auth request failed");
    }

    /**
     * 返回失败请求实际读取的主体、请求结束状态及当前探针请求的认证状态。
     *
     * @param requestId 测试请求标识
     * @return 隔离观察结果
     */
    @GET
    @Path("/observation")
    public Map<String, Object> observation(@QueryParam("requestId") String requestId) {
        Observation observation = observations.remove(requestId);
        if (Objects.isNull(observation)) {
            throw new NotFoundException();
        }
        return Map.of(
                "observedLoginId", observation.loginId,
                "requestEnded", observation.requestEnded.get(),
                "currentRequestAuthenticated", SubjectKit.isLogin());
    }

    private static final class Observation {
        private final String loginId;
        private final AtomicBoolean requestEnded = new AtomicBoolean();

        private Observation(String loginId) {
            this.loginId = loginId;
        }
    }
}
