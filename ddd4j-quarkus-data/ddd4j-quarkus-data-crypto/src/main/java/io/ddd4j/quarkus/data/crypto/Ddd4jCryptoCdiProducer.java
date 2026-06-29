package io.ddd4j.quarkus.data.crypto;

import io.ddd4j.data.crypto.CryptoProperties;
import io.ddd4j.data.crypto.strategy.CryptoStrategy;
import io.ddd4j.data.crypto.strategy.DefaultCryptoStrategy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

/**
 * ddd4j-quarkus 加解密 CDI 生产者。
 *
 * <p>通过 CDI {@code @Produces} 暴露 ddd4j-data-crypto 的纯 Java 加解密策略到 Quarkus 容器，
 * 替代 Spring Boot 的 auto-config。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
public class Ddd4jCryptoCdiProducer {

    @Produces
    @ApplicationScoped
    public CryptoProperties cryptoProperties() {
        return new CryptoProperties();
    }

    @Produces
    @ApplicationScoped
    public CryptoStrategy defaultCryptoStrategy() {
        return new DefaultCryptoStrategy(null);
    }

}
