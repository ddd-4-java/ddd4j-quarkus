package io.ddd4j.quarkus.excel;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * {@link Ddd4jExcelCdiProducer} Quarkus 集成测试：验证 Excel 导出 Kit 的
 * CDI 装配与真实导出（EasyExcel 写 xlsx 字节）。
 */
@QuarkusTest
class ExcelQuarkusTest {

    @Inject
    ExcelHttpKit excelHttpKit;

    @Inject
    ExcelConfig excelConfig;

    @Test
    void beansShouldBeInjectable() {
        assertNotNull(excelHttpKit);
        assertNotNull(excelConfig);
    }

    @Test
    void shouldWriteExcelResponse() {
        Response response = excelHttpKit.write(
                "orders.xlsx",
                OrderRow.class,
                List.of(new OrderRow("ORD-001", "张三")));
        assertNotNull(response);
        assertEquals(200, response.getStatus());
        byte[] bytes = (byte[]) response.getEntity();
        assertNotNull(bytes);
        // xlsx magic header: PK (zip)
        assertEquals('P', bytes[0]);
        assertEquals('K', bytes[1]);
    }

    @Test
    void shouldBuildDownloadResponse() {
        Response response = ExcelHttpKit.download("data.xlsx", new byte[]{1, 2, 3});
        assertNotNull(response);
        assertEquals(200, response.getStatus());
    }

    /**
     * 导出表头 POJO（无注解，EasyExcel 以字段名作为列头）。
     */
    public static class OrderRow {

        private String orderNo;
        private String buyerName;

        public OrderRow() {
        }

        public OrderRow(String orderNo, String buyerName) {
            this.orderNo = orderNo;
            this.buyerName = buyerName;
        }

        public String getOrderNo() {
            return orderNo;
        }

        public void setOrderNo(String orderNo) {
            this.orderNo = orderNo;
        }

        public String getBuyerName() {
            return buyerName;
        }

        public void setBuyerName(String buyerName) {
            this.buyerName = buyerName;
        }
    }
}
