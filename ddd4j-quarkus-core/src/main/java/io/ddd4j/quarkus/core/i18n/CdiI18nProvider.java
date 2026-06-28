package io.ddd4j.quarkus.core.i18n;

import io.ddd4j.core.context.I18nProvider;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * CDI 实现的国际化提供者
 * <p>
 * 基于 {@link I18nHelper} 实现，使用 ResourceBundle 加载 i18n 资源。
 * 替代 Spring 的 MessageSource。
 *
 * @author Loong Wan
 */
@ApplicationScoped
public class CdiI18nProvider implements I18nProvider {

    @Override
    public String getMessage(String key, Object... args) {
        return I18nHelper.i18n(I18nHelper.getDefaultLang(), key, args);
    }
}
