package com.speedment.plugins.gson;

import com.speedment.code.JavaClassTranslator;
import com.speedment.code.Translator;
import com.speedment.code.TranslatorDecorator;
import com.speedment.codegen.model.Field;
import com.speedment.codegen.model.Interface;
import com.speedment.codegen.model.Method;
import com.speedment.config.db.Table;
import static com.speedment.internal.codegen.model.constant.DefaultType.STRING;

/**
 *
 * @author  Emil Forslund
 * @since   1.0.0
 */
public final class GeneratedManagerDecorator implements TranslatorDecorator<Table, Interface> {

    @Override
    public void apply(JavaClassTranslator<Table, Interface> translator) {
        translator.onMake((file, builder) -> {
            builder.forEveryTable(Translator.Phase.POST_MAKE, (clazz, table) -> {
                clazz.add(Method.of("fromJson", translator.getSupport().entityType())
                    .add(Field.of("json", STRING))
                );
            });
        });
    }
    
}
