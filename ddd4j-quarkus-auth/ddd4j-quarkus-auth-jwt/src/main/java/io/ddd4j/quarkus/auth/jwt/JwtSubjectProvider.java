package io.ddd4j.quarkus.auth.jwt;

import io.ddd4j.core.subject.Subject;
import io.ddd4j.core.subject.SubjectProvider;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import org.jboss.logging.Logger;

/**
 * JWT 版 {@link SubjectProvider}：从 CDI 容器获取当前请求作用域的 {@link JwtSubject}。
 *
 * <p>由于 {@link SubjectKit#getSubject()} 是静态调用（框架无关），无法直接依赖 CDI 注入，
 * 本 Provider 作为静态门面与 CDI 请求作用域之间的桥梁：
 * <pre>
 *   SubjectKit.getSubject()  → SubjectProvider.getSubject()  → Arc.container().instance(JwtSubject)
 * </pre>
 *
 * <p>在非请求线程（如定时任务、MQ 消费）调用时，{@link JwtSubject} 的请求作用域不可用，
 * 此时返回一个匿名 JWT（principal 为空），业务层应判断 {@code getPrincipal() == null}。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
public class JwtSubjectProvider implements SubjectProvider {

    private static final Logger logger = Logger.getLogger(JwtSubjectProvider.class);

    @Override
    public Subject getSubject() {
        ArcContainer container = Arc.container();
        if (container == null) {
            logger.debug("Arc container not active, returning empty JwtSubject");
            return new JwtSubject(null, null);
        }
        try {
            JwtSubject instance = container.instance(JwtSubject.class).get();
            if (instance != null) {
                return instance;
            }
        } catch (Exception e) {
            logger.debugf(e, "JwtSubject not resolvable in current context (likely non-request thread)");
        }
        // 非请求线程兜底：返回空 Subject（principal 为 null）
        return new JwtSubject(null, null);
    }

    @Override
    public Subject getSubject(String realm) {
        // JWT 单一账号体系，忽略 realm
        return getSubject();
    }
}
