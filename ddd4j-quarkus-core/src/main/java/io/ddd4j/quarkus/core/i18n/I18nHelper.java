package io.ddd4j.quarkus.core.i18n;

import org.jboss.logging.Logger;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quarkus 原生国际化辅助：从 classpath 下 {@code i18n/message_{lang}.properties} 加载文案，
 * 用 {@link ResourceBundle} 解析，不依赖 Spring ThreadContext。
 * <p>
 * 对标 ddd4j-core 的 {@code I18nKit}（依赖 Spring ThreadContext 获取 Locale），
 * Quarkus 轨道采用语言参数显式传递方案。
 * </p>
 */
public class I18nHelper {

    private static final Logger logger = Logger.getLogger(I18nHelper.class);
    private static final String BUNDLE_PREFIX = "i18n/message";
    private static final Map<String, ResourceBundle> CACHE = new ConcurrentHashMap<>();
    private static volatile String defaultLang = "zh";

    /**
     * 设置默认语言（应用启动时调用）。
     *
     * @param lang 语言标识
     */
    public static void setDefaultLang(String lang) {
        defaultLang = lang;
    }

    /**
     * 获取默认语言。
     *
     * @return 语言标识
     */
    public static String getDefaultLang() {
        return defaultLang;
    }

    /**
     * 按语言做国际化翻译。
     *
     * @param lang       语言标识（如 zh、en、vi）
     * @param key        i18n 键（含点号，如 asset.type.1）
     * @param parameters 可选的 String.format 参数
     * @return 翻译后字符串；找不到键则原样返回 key
     */
    public static String i18n(String lang, String key, Object... parameters) {
        if (key == null || key.isEmpty()) return "";
        if (lang == null || lang.isEmpty()) lang = defaultLang;

        String result = resolve(lang, key);
        if (parameters != null && parameters.length > 0) {
            try {
                result = String.format(result, parameters);
            } catch (Exception e) {
                logger.warnf("Could not format i18n msg, lang=%s, key=%s", lang, key);
            }
        }
        return result;
    }

    private static String resolve(String lang, String key) {
        ResourceBundle bundle = CACHE.computeIfAbsent(lang, I18nHelper::loadBundle);
        if (bundle != null && bundle.containsKey(key)) {
            return bundle.getString(key);
        }
        // 回退：zh-TW → zh
        if (lang.contains("-")) {
            String fallback = lang.substring(0, lang.indexOf("-"));
            ResourceBundle fb = CACHE.computeIfAbsent(fallback, I18nHelper::loadBundle);
            if (fb != null && fb.containsKey(key)) {
                return fb.getString(key);
            }
        }
        return key;
    }

    private static ResourceBundle loadBundle(String lang) {
        try {
            Locale locale = Locale.forLanguageTag(lang);
            return ResourceBundle.getBundle(BUNDLE_PREFIX, locale);
        } catch (MissingResourceException e) {
            logger.debugf("i18n bundle not found for lang=%s", lang);
            return null;
        }
    }
}
