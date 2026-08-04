package io.ddd4j.quarkus.excel;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Excel 附件元数据（文件名、Content-Type、编码）。
 *
 * <p>纯 Java 迁移自 ddd4j-boot-extension-excel 的 {@code ExcelAttachment}（boot 侧位于
 * {@code io.ddd4j.boot.excel.web} 包）。封装 Web 下载时所需的 HTTP 响应头字段，
 * 统一处理中文文件名编码（RFC 5987）。
 *
 * <h3>典型用法</h3>
 * <pre>{@code
 * ExcelAttachment attachment = ExcelAttachment.xlsx("订单.xlsx");
 * Response response = Response.ok(bytes, attachment.contentTypeWithCharset())
 *         .header("Content-Disposition", attachment.contentDisposition())
 *         .build();
 * }</pre>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 3.3.x
 */
public final class ExcelAttachment {

    /**
     * xlsx 默认 Content-Type。
     */
    public static final String CONTENT_TYPE_XLSX =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    /**
     * xls 默认 Content-Type。
     */
    public static final String CONTENT_TYPE_XLS = "application/vnd.ms-excel";

    /**
     * csv 默认 Content-Type。
     */
    public static final String CONTENT_TYPE_CSV = "text/csv";

    private final String filename;
    private final String contentType;
    private final Charset charset;

    private ExcelAttachment(String filename, String contentType, Charset charset) {
        this.filename = filename;
        this.contentType = contentType;
        this.charset = charset;
    }

    public String getFilename() {
        return filename;
    }

    public String getContentType() {
        return contentType;
    }

    public Charset getCharset() {
        return charset;
    }

    /**
     * 构造 xlsx 附件（UTF-8 编码文件名）。
     *
     * @param filename 文件名（含扩展名）
     * @return {@link ExcelAttachment}
     */
    public static ExcelAttachment xlsx(String filename) {
        return new ExcelAttachment(filename, CONTENT_TYPE_XLSX, StandardCharsets.UTF_8);
    }

    /**
     * 构造 xls 附件。
     *
     * @param filename 文件名
     * @return {@link ExcelAttachment}
     */
    public static ExcelAttachment xls(String filename) {
        return new ExcelAttachment(filename, CONTENT_TYPE_XLS, StandardCharsets.UTF_8);
    }

    /**
     * 构造 csv 附件（指定字符集）。
     *
     * @param filename 文件名
     * @param charset  字符集（影响 Content-Type 中的 charset 参数）
     * @return {@link ExcelAttachment}
     */
    public static ExcelAttachment csv(String filename, Charset charset) {
        return new ExcelAttachment(filename, CONTENT_TYPE_CSV, charset);
    }

    /**
     * 生成 RFC 5987 编码的 Content-Disposition 头。
     *
     * <p>支持中文文件名，输出形如：
     * {@code attachment;filename*=utf-8''%E8%AE%A2%E5%8D%95.xlsx}
     *
     * @return Content-Disposition 头值
     */
    public String contentDisposition() {
        String encoded = URLEncoder.encode(filename, charset).replace("+", "%20");
        return "attachment;filename*=" + charset.name().toLowerCase() + "''" + encoded;
    }

    /**
     * 返回带 charset 参数的 Content-Type（CSV 场景）。
     *
     * @return Content-Type 头值
     */
    public String contentTypeWithCharset() {
        if (CONTENT_TYPE_CSV.equals(contentType)) {
            return contentType + ";charset=" + charset.name();
        }
        return contentType;
    }
}
