/**
 * 领域层（Domain Layer）—— 订单子域。
 *
 * <p>领域层是 DDD 分层架构的核心层，包含业务逻辑与领域模型，
 * 不依赖任何技术设施（数据库 / 消息队列 / Web 框架等）。
 * 本模块对齐 ddd4j-boot 的 {@code ddd4j-boot-sample-domain}，采用
 * ddd4j-core 的充血模型基类 {@link io.ddd4j.core.ddd.model.AggregateRoot}。
 *
 * <h3>主要职责</h3>
 * <ul>
 *   <li><b>聚合根（Aggregate Root）</b>：{@code model/aggregate/Order}，
 *       维护聚合的一致性边界并封装业务规则（支付 / 发货 / 取消）</li>
 *   <li><b>值对象（Value Object）</b>：{@code model/OrderItem}、{@code model/OrderStatus}</li>
 *   <li><b>仓储接口（Repository Interface）</b>：{@code repository/OrderRepository}，
 *       继承 {@link io.ddd4j.core.ddd.repository.Repository}，仅定义契约不含实现</li>
 *   <li><b>领域事件（Domain Event）</b>：{@code event/} 下四个订单事件，
 *       继承 {@link io.ddd4j.core.ddd.event.DomainEvent}，通过 {@code publish()} 发布</li>
 * </ul>
 *
 * <h3>设计原则</h3>
 * <ul>
 *   <li>领域层技术无关：不引入 JPA / Panache / Quarkus 等任何框架注解</li>
 *   <li>业务逻辑内聚在聚合根，状态流转由 {@code OrderStatus} 约束</li>
 *   <li>领域事件在聚合根内注册（{@code registerEvent}），
 *       由基础设施层在持久化成功后统一发布</li>
 * </ul>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
package io.ddd4j.quarkus.sample.domain.order;
