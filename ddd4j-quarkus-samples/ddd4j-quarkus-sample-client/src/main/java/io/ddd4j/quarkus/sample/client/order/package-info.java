/**
 * 订单客户端模块（Client Module）。
 *
 * <p>客户端模块是分层架构中的「接口契约层」，用于定义对外提供的服务接口与 DTO，
 * 对齐 ddd4j-boot 的 {@code ddd4j-boot-sample-client}。</p>
 *
 * <h3>主要职责：</h3>
 * <ul>
 *   <li><b>服务接口定义</b>：{@link io.ddd4j.quarkus.sample.client.order.OrderClientService}
 *       定义订单服务对外提供的客户端调用能力，由适配层（Adapter）实现</li>
 *   <li><b>响应对象</b>：{@link io.ddd4j.quarkus.sample.client.order.OrderClientDTO}
 *       定义服务返回给调用方的数据结构</li>
 * </ul>
 *
 * <h3>设计原则：</h3>
 * <ul>
 *   <li>客户端模块保持独立，不依赖应用层、领域层等任何内部模块</li>
 *   <li>使用独立的 DTO 对象，不暴露内部实现细节（如聚合根结构）</li>
 *   <li>接口定义清晰、稳定，便于版本管理与跨服务调用（REST/RPC 均可）</li>
 * </ul>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
package io.ddd4j.quarkus.sample.client.order;
