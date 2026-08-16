package io.ddd4j.quarkus.jackson.ser;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;

import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * null 值默认序列化策略：按属性类型将 {@code null} 序列化为类型默认值而非省略字段。
 *
 * <p>对齐 boot 轨 {@code io.github.hiwepy:jackson-extension} 的
 * {@code MyBeanSerializerModifier} 语义（该上游 3.0.x 已迁移 Jackson 3 命名空间
 * {@code tools.jackson}，与 Quarkus 3.x 的 Jackson 2 不兼容，故本模块自包含实现）。
 *
 * <p>六类开关（{@code ddd4j.jackson.default-null-*-serializer}）：
 * <ul>
 *   <li>array —— 数组/集合 → {@code []}</li>
 *   <li>number —— 数值 → {@code 0}</li>
 *   <li>string —— 字符串 → {@code ""}</li>
 *   <li>date —— 日期/时间 → {@code ""}</li>
 *   <li>boolean —— 布尔 → {@code false}</li>
 *   <li>json-object —— 对象/Map → {@code {}}</li>
 * </ul>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 4.0.x
 */
public class NullTolerantBeanSerializerModifier extends BeanSerializerModifier {

    private final boolean nullArray;
    private final boolean nullNumber;
    private final boolean nullString;
    private final boolean nullDate;
    private final boolean nullBoolean;
    private final boolean nullJsonObject;

    public NullTolerantBeanSerializerModifier(boolean nullArray, boolean nullNumber, boolean nullString,
                                              boolean nullDate, boolean nullBoolean, boolean nullJsonObject) {
        this.nullArray = nullArray;
        this.nullNumber = nullNumber;
        this.nullString = nullString;
        this.nullDate = nullDate;
        this.nullBoolean = nullBoolean;
        this.nullJsonObject = nullJsonObject;
    }

    @Override
    public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc,
                                                     List<BeanPropertyWriter> beanProperties) {
        List<BeanPropertyWriter> modified = new ArrayList<>(beanProperties.size());
        for (BeanPropertyWriter writer : beanProperties) {
            modified.add(modify(writer));
        }
        return modified;
    }

    private BeanPropertyWriter modify(BeanPropertyWriter writer) {
        if (writer == null) {
            return null;
        }
        Class<?> type = writer.getPropertyType();
        if (type == null) {
            return writer;
        }
        if (type.isArray() || Collection.class.isAssignableFrom(type)) {
            return nullArray ? new NullValueBeanPropertyWriter(writer, "[]") : writer;
        }
        if (CharSequence.class.isAssignableFrom(type) || Character.class == type) {
            return nullString ? new NullValueBeanPropertyWriter(writer, "\"\"") : writer;
        }
        if (Number.class.isAssignableFrom(type) || type.isPrimitive()
                && type != boolean.class && type != void.class) {
            return nullNumber ? new NullValueBeanPropertyWriter(writer, "0") : writer;
        }
        if (Date.class.isAssignableFrom(type) || Temporal.class.isAssignableFrom(type)) {
            return nullDate ? new NullValueBeanPropertyWriter(writer, "\"\"") : writer;
        }
        if (boolean.class == type || Boolean.class == type) {
            return nullBoolean ? new NullValueBeanPropertyWriter(writer, "false") : writer;
        }
        if (Map.class.isAssignableFrom(type) || Object.class == type) {
            return nullJsonObject ? new NullValueBeanPropertyWriter(writer, "{}") : writer;
        }
        return nullJsonObject ? new NullValueBeanPropertyWriter(writer, "{}") : writer;
    }
}
