package com.speedment.plugins.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.speedment.code.JavaClassTranslator;
import com.speedment.code.Translator;
import com.speedment.codegen.model.Class;
import com.speedment.code.TranslatorDecorator;
import com.speedment.codegen.model.Field;
import com.speedment.codegen.model.Import;
import com.speedment.codegen.model.Method;
import com.speedment.codegen.model.Type;
import com.speedment.config.db.Table;
import static com.speedment.internal.codegen.model.constant.DefaultAnnotationUsage.OVERRIDE;
import static com.speedment.internal.codegen.model.constant.DefaultType.STRING;
import static com.speedment.internal.codegen.util.Formatting.indent;

/**
 *
 * @author  Emil Forslund
 * @since   1.0.0
 */
public final class GeneratedManagerImplDecorator implements TranslatorDecorator<Table, Class> {

    @Override
    public void apply(JavaClassTranslator<Table, Class> translator) {
        final String entityName = translator.getSupport().entityName();
        final String typeAdapterName = "Generated" + entityName + "TypeAdapter";
        
        translator.onMake((file, builder) -> {
            builder.forEveryTable(Translator.Phase.POST_MAKE, (clazz, table) -> {
                
                // Make sure GsonBuilder and the generated type adapter are imported.
                file.add(Import.of(Type.of(GsonBuilder.class)));
                file.add(Import.of(Type.of(translator.getSupport().basePackageName() + ".generated." + typeAdapterName)));
                
                // Add a Gson instance as a private member
                clazz.add(Field.of("gson", Type.of(Gson.class))
                    .private_().final_()
                );
                
                // Find the constructor and define gson in it
                clazz.getConstructors().forEach(constr -> {
                    constr.add(
                        "this.gson = new GsonBuilder()",
                        indent(".setDateFormat(\"" + GeneratedTypeAdapterTranslator.DATE_FORMAT + "\")"),
                        indent(".registerTypeAdapter(" + entityName + ".class, new " + typeAdapterName + "(this))"),
                        indent(".create();")
                    );
                });
                
                // Override the toJson()-method
                clazz.add(Method.of("toJson", STRING)
                    .public_().add(OVERRIDE)
                    .add(Field.of("entity", translator.getSupport().entityType()))
                    .add("return gson.toJson(entity, " + entityName + ".class);")
                );
                
                // Override the fromJson()-method
                clazz.add(Method.of("fromJson", translator.getSupport().entityType())
                    .public_().add(OVERRIDE)
                    .add(Field.of("json", STRING))
                    .add("return gson.fromJson(json, " + entityName + ".class);")
                );
            });
        });
    }
    
}
