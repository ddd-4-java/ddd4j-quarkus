package io.ddd4j.quarkus.jackson.ser;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;



/**
 * {@code null} 属性写为固定 JSON 字面量（如 {@code []} / {@code ""} / {@code 0} / {@code {}}）的
 * {@link BeanPropertyWriter} 装饰器。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 4.0.x
 */
class NullValueBeanPropertyWriter extends BeanPropertyWriter {

    private static final long serialVersionUID = 1L;

    private final BeanPropertyWriter delegate;
    private final String nullLiteral;

    NullValueBeanPropertyWriter(BeanPropertyWriter delegate, String nullLiteral) {
        super(delegate);
        this.delegate = delegate;
        this.nullLiteral = nullLiteral;
    }

    @Override
    public void serializeAsField(Object bean, JsonGenerator gen, SerializerProvider prov) throws Exception {
        Object value = get(bean);
        if (value == null) {
            gen.writeFieldName(delegate.getName());
            gen.writeRawValue(nullLiteral);
            return;
        }
        delegate.serializeAsField(bean, gen, prov);
    }
}
