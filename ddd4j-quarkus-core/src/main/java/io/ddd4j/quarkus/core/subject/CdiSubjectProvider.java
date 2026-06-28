package io.ddd4j.quarkus.core.subject;

import io.ddd4j.core.subject.Subject;
import io.ddd4j.core.subject.SubjectProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * CDI 实现的 Subject 提供者
 * <p>
 * 通过 CDI 注入 Subject 实现，替代 Spring 的 ApplicationContext.getBean()。
 *
 * @author Loong Wan
 */
@ApplicationScoped
public class CdiSubjectProvider implements SubjectProvider {

    private static final Logger logger = Logger.getLogger(CdiSubjectProvider.class);

    @Inject
    Instance<Subject> subjectInstance;

    @Override
    public Subject getSubject() {
        if (subjectInstance.isUnsatisfied()) {
            logger.debug("No Subject implementation found in CDI container");
            return null;
        }
        if (subjectInstance.isAmbiguous()) {
            logger.warn("Multiple Subject implementations found, using first one");
        }
        return subjectInstance.get();
    }
}
