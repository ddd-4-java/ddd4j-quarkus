package io.ddd4j.quarkus.sample.auth.satoken;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaTokenContext;
import cn.dev33.satoken.context.model.SaRequest;
import cn.dev33.satoken.context.model.SaResponse;
import cn.dev33.satoken.context.model.SaStorage;
import cn.dev33.satoken.context.model.SaTokenContextModelBox;
import io.ddd4j.core.auth.AuthPrincipal;
import io.ddd4j.core.subject.SubjectDataProvider;
import io.ddd4j.core.util.SubjectKit;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.vertx.core.http.Cookie;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 将真实 Vert.x 请求接入 Sa-Token，并提供演示账号的授权数据。
 * 存储绑定 RoutingContext，避免工作线程复用时污染相邻请求。
 */
@ApplicationScoped
public class SampleSaTokenRuntime implements SaTokenContext {

    private static final String CONTEXT_KEY = "sample.sa-token.context";

    @Inject
    CurrentVertxRequest currentRequest;

    private SaTokenContext previousContext;
    private SubjectDataProvider previousDataProvider;
    private final SubjectDataProvider sampleDataProvider = new SubjectDataProvider() {
        @Override
        public List<String> getRoleList(AuthPrincipal principal) {
            return List.of("user");
        }

        @Override
        public List<String> getPermissionList(AuthPrincipal principal) {
            return List.of("profile:read");
        }
    };

    void start(@Observes StartupEvent event) {
        previousContext = SaManager.getSaTokenContext();
        previousDataProvider = SubjectKit.getDataProvider();
        SaManager.setSaTokenContext(this);
        SubjectKit.setDataProvider(sampleDataProvider);
    }

    void stop(@Observes ShutdownEvent event) {
        SaManager.setSaTokenContext(previousContext);
        SubjectKit.setDataProvider(previousDataProvider);
    }

    /** 在当前请求中显式设置上下文，不创建线程全局状态。 */
    @Override
    public void setContext(SaRequest request, SaResponse response, SaStorage storage) {
        currentRequest.getCurrent().put(CONTEXT_KEY, new SaTokenContextModelBox(request, response, storage));
    }

    /** 清除当前请求中显式设置的上下文。 */
    @Override
    public void clearContext() {
        currentRequest.getCurrent().remove(CONTEXT_KEY);
    }

    /** 读取显式上下文，或从当前真实 HTTP 请求构建上下文。 */
    @Override
    public SaTokenContextModelBox getModelBox() {
        RoutingContext context = currentRequest.getCurrent();
        SaTokenContextModelBox box = context.get(CONTEXT_KEY);
        return Objects.nonNull(box) ? box
                : new SaTokenContextModelBox(new Request(context), new Response(context), new Storage(context));
    }

    /** 返回当前 HTTP 请求适配器。 */
    @Override
    public SaRequest getRequest() {
        return getModelBox().getRequest();
    }

    /** 返回当前 HTTP 响应适配器。 */
    @Override
    public SaResponse getResponse() {
        return getModelBox().getResponse();
    }

    /** 返回仅属于当前请求的临时存储。 */
    @Override
    public SaStorage getStorage() {
        return getModelBox().getStorage();
    }

    /** 只有真实 HTTP 请求激活时才允许读取会话。 */
    @Override
    public boolean isValid() {
        return Objects.nonNull(currentRequest.getCurrent());
    }

    /** Vert.x 请求到 Sa-Token 请求接口的适配。 */
    private record Request(RoutingContext context) implements SaRequest {
        @Override
        public Object getSource() { return context.request(); }
        @Override
        public String getParam(String name) { return context.request().getParam(name); }
        @Override
        public Collection<String> getParamNames() { return context.request().params().names(); }
        @Override
        public Map<String, String> getParamMap() {
            Map<String, String> params = new HashMap<>();
            context.request().params().forEach(entry -> params.put(entry.getKey(), entry.getValue()));
            return params;
        }
        @Override
        public String getHeader(String name) { return context.request().getHeader(name); }
        @Override
        public String getCookieValue(String name) {
            Cookie cookie = context.request().getCookie(name);
            return Objects.isNull(cookie) ? null : cookie.getValue();
        }
        @Override
        public String getCookieFirstValue(String name) { return getCookieValue(name); }
        @Override
        public String getCookieLastValue(String name) { return getCookieValue(name); }
        @Override
        public String getRequestPath() { return context.request().path(); }
        @Override
        public String getUrl() { return context.request().absoluteURI(); }
        @Override
        public String getMethod() { return context.request().method().name(); }
        @Override
        public String getHost() { return context.request().getHeader("Host"); }
        @Override
        public Object forward(String path) {
            context.reroute(path);
            return null;
        }
    }

    /** 将响应头与状态写入真实 Vert.x 响应。 */
    private record Response(RoutingContext context) implements SaResponse {
        @Override
        public Object getSource() { return context.response(); }
        @Override
        public SaResponse setStatus(int status) {
            context.response().setStatusCode(status);
            return this;
        }
        @Override
        public SaResponse setHeader(String name, String value) {
            context.response().putHeader(name, value);
            return this;
        }
        @Override
        public SaResponse addHeader(String name, String value) {
            context.response().headers().add(name, value);
            return this;
        }
        @Override
        public Object redirect(String url) {
            return context.response().setStatusCode(302).putHeader("Location", url).end();
        }
    }

    /** 使用独立键空间，避免与业务 RoutingContext 属性冲突。 */
    private record Storage(RoutingContext context) implements SaStorage {
        private static final String PREFIX = "sample.sa-token.";
        @Override
        public Object getSource() { return context; }
        @Override
        public Object get(String key) { return context.get(PREFIX + key); }
        @Override
        public SaStorage set(String key, Object value) {
            context.put(PREFIX + key, value);
            return this;
        }
        @Override
        public SaStorage delete(String key) {
            context.remove(PREFIX + key);
            return this;
        }
    }
}
