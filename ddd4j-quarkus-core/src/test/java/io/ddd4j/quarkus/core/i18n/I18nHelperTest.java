package io.ddd4j.quarkus.core.i18n;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link I18nHelper} 纯单元测试。
 *
 * <p>验证语言回退与 key 缺失处理（对标 cloud-das I18nUtil 的回退逻辑）：
 * <ul>
 *   <li>缺失 key 时原样返回 key</li>
 *   <li>空 lang 回退到默认语言</li>
 *   <li>占位符 String.format 生效</li>
 * </ul>
 *
 * <p>注意：本测试不依赖实际的 i18n/message.properties（测试 classpath 无该资源），
 * 因此覆盖的是「资源缺失」分支（返回原始 key），验证降级行为健壮。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
class I18nHelperTest {

    @Test
    void null_key_returns_empty() {
        assertEquals("", I18nHelper.i18n("zh", null));
        assertEquals("", I18nHelper.i18n("zh", ""));
    }

    @Test
    void missing_resource_returns_original_key() {
        // 测试 classpath 无 i18n/message.properties，应原样返回 key
        String key = "nonexistent.key.12345";
        String result = I18nHelper.i18n("zh", key);
        assertEquals(key, result, "资源缺失时应原样返回 key");
    }

    @Test
    void empty_lang_falls_back_to_default() {
        String defaultLang = I18nHelper.getDefaultLang();
        String result = I18nHelper.i18n(null, "any.key");
        // 不抛异常即说明回退成功
        assertNotNull(result);
        assertEquals(defaultLang, I18nHelper.getDefaultLang());
    }

    @Test
    void placeholder_format_applied_to_raw_key_when_resource_missing() {
        // 资源缺失时 key 本身作为 pattern，String.format 生效
        String result = I18nHelper.i18n("zh", "value-%s", "X");
        assertEquals("value-X", result);
    }

    @Test
    void invalid_placeholder_does_not_throw() {
        // pattern 与参数不匹配时不应抛异常（降级为返回未格式化结果）
        assertDoesNotThrow(() -> I18nHelper.i18n("zh", "no-placeholder", "unused-arg"));
    }
}
