/**
 * 分层架构示例主应用模块（Layered Module）。
 *
 * <p>本模块聚合分层示例的主要分层（client / app / domain / adapter / common），
 * 作为可运行的 Quarkus 应用入口，演示 DDD 分层架构在 Quarkus 下的组装方式；
 * infrastructure（Panache 持久化）按需引入，默认使用内存仓储。</p>
 *
 * <h3>目录结构：</h3>
 * <ul>
 *   <li><code>LayeredOrderApplication</code> - Quarkus 启动入口（{@code @QuarkusMain}）</li>
 *   <li><code>config/</code> - CDI 装配（{@link io.ddd4j.quarkus.sample.layered.config.LayeredSampleConfig}
 *       @Produces 内存版 OrderRepository）</li>
 * </ul>
 *
 * <p>JAX-RS 资源复用 {@code adapter} 模块的 {@code OrderResource}（{@code /api/orders}），
 * 依赖 jar 中的 {@code @Path} 资源由 Quarkus RESTEasy Reactive 自动发现，无需重复实现。</p>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
package io.ddd4j.quarkus.sample.layered;
