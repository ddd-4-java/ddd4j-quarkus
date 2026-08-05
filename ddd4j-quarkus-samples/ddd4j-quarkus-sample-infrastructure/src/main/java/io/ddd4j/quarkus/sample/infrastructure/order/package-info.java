/**
 * 基础设施层（Infrastructure Layer）—— 订单子域。
 *
 * <p>基础设施层实现领域层定义的仓储契约，依赖
 * {@code ddd4j-quarkus-sample-domain}，但依赖方向保持单向
 * （基础设施 → 领域），领域层不感知任何技术实现。</p>
 *
 * <h3>主要职责</h3>
 * <ul>
 *   <li><b>持久化</b>：{@code persistence/entity/OrderEntity}（Panache 实体）+
 *       {@code persistence/OrderRepositoryImpl}（实现 {@code OrderRepository}）</li>
 *   <li><b>装配</b>：{@code config/OrderInfrastructureConfig} 通过 CDI {@code @Produces}
 *       装配仓储并将其实例注册到
 *       {@link io.ddd4j.core.ddd.repository.RepositoryRegistry}</li>
 *   <li><b>领域事件发布</b>：仓储持久化成功后统一调用
 *       {@link io.ddd4j.core.ddd.event.DomainEvent#publish()} 发布聚合根注册的事件</li>
 * </ul>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
package io.ddd4j.quarkus.sample.infrastructure.order;
