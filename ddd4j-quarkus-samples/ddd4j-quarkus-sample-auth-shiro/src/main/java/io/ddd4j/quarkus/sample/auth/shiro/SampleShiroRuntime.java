package io.ddd4j.quarkus.sample.auth.shiro;

import io.ddd4j.kit.lang.StrKit;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.interceptor.InvocationContext;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.realm.SimpleAccountRealm;
import org.apache.shiro.subject.Subject;

/**
 * 演示账号的真实 Shiro Realm 与内存 Session 管理器；仅供本地示例。
 * 生产系统应从账号服务验证凭证，并配置持久化 SessionDAO。
 */
@ApplicationScoped
public class SampleShiroRuntime {

    private DefaultSecurityManager securityManager;

    @PostConstruct
    void initialize() {
        securityManager = new DefaultSecurityManager(new SampleRealm());
    }

    /**
     * 用请求携带的原生 Session ID 恢复主体，并在方法结束时恢复线程上下文。
     *
     * @param invocation 当前资源方法调用
     * @param sessionId X-Session-Id 请求头中的 Shiro Session ID
     * @return 资源方法结果
     * @throws Exception 资源方法抛出的异常
     */
    public Object invoke(InvocationContext invocation, String sessionId) throws Exception {
        Subject.Builder builder = new Subject.Builder(securityManager);
        if (StrKit.isNotBlank(sessionId)) {
            builder.sessionId(sessionId);
        }
        Subject subject = builder.buildSubject();
        // Shiro execute 使用 SubjectThreadState，在异常路径也会恢复原线程状态。
        return subject.execute(invocation::proceed);
    }

    @PreDestroy
    void close() {
        securityManager.destroy();
    }

    /** 固定演示账号；使用 Shiro 自身的凭证匹配、角色和权限查询。 */
    private static final class SampleRealm extends SimpleAccountRealm {
        private SampleRealm() {
            // 示例沿用 userId 请求体；空凭证只用于固定演示账号，不代表生产认证。
            addAccount("alice", "", "user");
            addAccount("bob", "", "user");
            getUser("alice").addStringPermission("profile:read");
            getUser("bob").addStringPermission("profile:read");
        }
    }
}
