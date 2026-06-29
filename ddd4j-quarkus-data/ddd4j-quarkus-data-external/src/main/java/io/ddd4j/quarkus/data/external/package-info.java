/**
 * ddd4j-quarkus 外部服务 CDI 桥接占位包。
 *
 * <p>ddd4j-data-external 的 Template 深度依赖 Spring RestClient / RedisOperationTemplate，
 * Quarkus 端需自行通过 Vert.x HttpClient / Quarkus Redis 适配。
 * 本包预留，业务项目按需创建 CDI Producer。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 */
package io.ddd4j.quarkus.data.external;
