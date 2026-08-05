package io.ddd4j.quarkus.sample.domain.order.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 订单项值对象。
 *
 * <p>不可变值对象：以 {@code productId + skuId} 标识一个商品行，
 * 通过值相等性判断，无独立生命周期。</p>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
public final class OrderItem {

    /**
     * 商品 ID
     */
    private final String productId;

    /**
     * SKU ID
     */
    private final String skuId;

    /**
     * 购买数量
     */
    private final Integer quantity;

    /**
     * 单价
     */
    private final BigDecimal price;

    /**
     * 构造订单项。
     *
     * @param productId 商品 ID（非空）
     * @param skuId     SKU ID（非空）
     * @param quantity  购买数量（大于 0）
     * @param price     单价（非空且大于等于 0）
     */
    public OrderItem(String productId, String skuId, Integer quantity, BigDecimal price) {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("productId must not be blank");
        }
        if (skuId == null || skuId.isBlank()) {
            throw new IllegalArgumentException("skuId must not be blank");
        }
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than 0");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("price must not be null and must be >= 0");
        }
        this.productId = productId;
        this.skuId = skuId;
        this.quantity = quantity;
        this.price = price;
    }

    public String getProductId() {
        return productId;
    }

    public String getSkuId() {
        return skuId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    /**
     * 计算小计金额（单价 × 数量）。
     *
     * @return 小计金额
     */
    public BigDecimal subtotal() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof OrderItem that)) {
            return false;
        }
        return Objects.equals(productId, that.productId)
                && Objects.equals(skuId, that.skuId)
                && Objects.equals(quantity, that.quantity)
                && Objects.equals(price, that.price);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, skuId, quantity, price);
    }

    @Override
    public String toString() {
        return "OrderItem{productId='" + productId + "', skuId='" + skuId
                + "', quantity=" + quantity + ", price=" + price + '}';
    }
}
