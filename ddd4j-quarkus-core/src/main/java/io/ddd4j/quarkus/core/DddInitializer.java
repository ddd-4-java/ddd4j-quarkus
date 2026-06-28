package io.ddd4j.quarkus.core;

import io.ddd4j.core.context.I18nKit;
import io.ddd4j.core.context.I18nProvider;
import io.ddd4j.core.subject.SubjectKit;
import io.ddd4j.core.subject.SubjectProvider;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * DDD 初始化器：在应用启动时注册 I18nProvider 和 SubjectProvider
 *
 * @author Loong Wan
 */
@ApplicationScoped
public class DddInitializer {

    private static final Logger logger = Logger.getLogger(DddInitializer.class);

    @Inject
    I18nProvider i18nProvider;

    @Inject
    SubjectProvider subjectProvider;

    void onStart(@Observes StartupEvent event) {
        // 注册 I18nProvider
        I18nKit.register(i18nProvider);
        logger.info("Registered I18nProvider for Quarkus CDI");

        // 注册 SubjectProvider
        SubjectKit.register(subjectProvider);
        logger.info("Registered SubjectProvider for Quarkus CDI");
    }
}
