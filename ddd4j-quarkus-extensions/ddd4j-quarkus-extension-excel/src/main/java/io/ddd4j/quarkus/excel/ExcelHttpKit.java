package io.ddd4j.quarkus.excel;

import com.alibaba.excel.read.listener.ReadListener;
import io.ddd4j.core.exception.BizRuntimeException;
import io.ddd4j.extension.excel.ExcelKit;
import io.ddd4j.extension.excel.importer.ImportResult;
import jakarta.ws.rs.core.Response;

import java.io.InputStream;
import java.util.List;

/**
 * Excel Web 工具集（下载 / 上传）—— Quarkus / JAX-RS 版。
 *
 * <p>迁移自 ddd4j-boot-extension-excel 的 {@code ExcelHttpKit}（boot 侧依赖 Spring Servlet API：
 * {@code HttpServletResponse} 下载 + {@code MultipartFile} 上传），Quarkus 版改用
 * {@link jakarta.ws.rs.core.Response}（JAX-RS 等价物）承载下载能力；上传侧不绑定任何
 * Web 框架类型，由调用方提供 {@link InputStream} 与文件名 / 大小元数据
 * （RESTEasy Reactive 的 {@code FileUpload} 可直接适配）。
 *
 * <h3>静态调用（下载）</h3>
 * <pre>{@code
 * // 一行下载：资源方法直接返回 Response
 * @GET @Path("/download")
 * public Response download() {
 *     return ExcelHttpKit.download("订单.xlsx", ExcelKit.export(OrderVO.class, orderService.listAll()));
 * }
 * }</pre>
 *
 * <h3>Bean 注入（推荐，可与 ExcelConfig 联动）</h3>
 * <pre>{@code
 * @Inject
 * ExcelHttpKit excelHttpKit;
 *
 * excelHttpKit.write("订单.xlsx", OrderVO.class, data); // 返回 Response
 * ImportResult<OrderVO> result = excelHttpKit.read(in, "订单.xlsx", fileSize, OrderVO.class);
 * }</pre>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
public class ExcelHttpKit {

    private final ExcelConfig properties;

    /**
     * 默认构造（使用默认 {@link ExcelConfig} 的等价默认值）。
     */
    public ExcelHttpKit() {
        this(null);
    }

    /**
     * 注入配置构造（由 {@link Ddd4jExcelCdiProducer} 装配时使用）。
     *
     * @param properties Excel 配置（提供 maxUploadMB / charset 等参数）
     */
    public ExcelHttpKit(ExcelConfig properties) {
        this.properties = properties;
    }

    // ───────────────────── 下载 ─────────────────────

    /**
     * 下载 xlsx 字节数组为 JAX-RS {@link Response}。
     *
     * @param filename 文件名（含 .xlsx 扩展名）
     * @param bytes    xlsx 字节
     * @return 携带附件头的响应
     */
    public static Response download(String filename, byte[] bytes) {
        return download(ExcelAttachment.xlsx(filename), bytes);
    }

    /**
     * 下载自定义附件类型的字节数组为 JAX-RS {@link Response}。
     *
     * @param attachment 附件元数据
     * @param bytes      xlsx 字节
     * @return 携带附件头的响应
     */
    public static Response download(ExcelAttachment attachment, byte[] bytes) {
        return Response.ok(bytes, attachment.contentTypeWithCharset())
                .header("Content-Disposition", attachment.contentDisposition())
                .header("Access-Control-Expose-Headers", "Content-Disposition")
                .header("Content-Length", bytes.length)
                .build();
    }

    /**
     * 一行下载：导出 → 字节 → Response（实例方法，便于注入 Bean 后调用）。
     *
     * @param filename 文件名
     * @param head     表头类
     * @param data     数据
     * @return 携带附件头的响应
     */
    public Response write(String filename, Class<?> head, List<?> data) {
        return download(filename, ExcelKit.export(head, data));
    }

    /**
     * 直接写字节到 Response（实例方法）。
     *
     * @param filename 文件名
     * @param bytes    xlsx 字节
     * @return 携带附件头的响应
     */
    public Response write(String filename, byte[] bytes) {
        return download(filename, bytes);
    }

    // ───────────────────── 上传 ─────────────────────

    /**
     * 解析输入流为导入结果（默认 ErrorCollectingReadListener）。
     *
     * @param in   输入流（调用方负责关闭）
     * @param head 表头类
     * @param <T>  数据类型
     * @return 导入结果
     */
    public static <T> ImportResult<T> upload(InputStream in, Class<T> head) {
        try (InputStream stream = in) {
            return ExcelKit.importExcel(stream, head);
        } catch (Exception e) {
            return ImportResult.empty();
        }
    }

    /**
     * 用自定义 listener 解析输入流。
     *
     * @param in       输入流（调用方负责关闭）
     * @param head     表头类
     * @param listener 监听器
     * @param <T>      数据类型
     * @return 导入结果
     */
    public static <T> ImportResult<T> upload(InputStream in, Class<T> head, ReadListener<T> listener) {
        try (InputStream stream = in) {
            return ExcelKit.importExcel(stream, head, listener);
        } catch (Exception e) {
            return ImportResult.empty();
        }
    }

    /**
     * 实例方法上传（带 {@link ExcelConfig#maxUploadMB()} 大小与扩展名校验）。
     *
     * @param in       输入流
     * @param filename 原始文件名（用于扩展名校验）
     * @param size     文件大小（字节）
     * @param head     表头类
     * @param <T>      数据类型
     * @return 导入结果
     */
    public <T> ImportResult<T> read(InputStream in, String filename, long size, Class<T> head) {
        validate(filename, size);
        return upload(in, head);
    }

    /**
     * 校验上传文件大小与扩展名（使用 {@link ExcelConfig#maxUploadMB()} 配置上限）。
     *
     * @param filename 原始文件名
     * @param size     文件大小（字节）
     */
    public void validate(String filename, long size) {
        validate(filename, size, maxUploadMB(), List.of(".xlsx", ".xls"));
    }

    /**
     * 校验上传文件大小与扩展名。
     *
     * @param filename   原始文件名
     * @param size       文件大小（字节）
     * @param maxMB      最大体积（MB）
     * @param extensions 允许的扩展名（如 {@code List.of(".xlsx", ".xls")}）
     * @throws BizRuntimeException 校验失败
     */
    public static void validate(String filename, long size, int maxMB, List<String> extensions) {
        if (size <= 0) {
            throw new BizRuntimeException(400, "excel.upload.empty");
        }
        long maxBytes = (long) maxMB * 1024 * 1024;
        if (size > maxBytes) {
            throw new BizRuntimeException(400, "excel.upload.too.large", size, maxBytes);
        }
        if (filename == null) {
            throw new BizRuntimeException(400, "excel.upload.no.filename");
        }
        String lower = filename.toLowerCase();
        boolean ok = extensions == null || extensions.isEmpty()
                || extensions.stream().anyMatch(lower::endsWith);
        if (!ok) {
            throw new BizRuntimeException(400, "excel.upload.invalid.extension", filename);
        }
    }

    private int maxUploadMB() {
        return properties == null ? 50 : properties.maxUploadMB();
    }
}
