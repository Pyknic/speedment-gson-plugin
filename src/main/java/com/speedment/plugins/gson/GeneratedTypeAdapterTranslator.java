package com.speedment.plugins.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.speedment.Speedment;
import com.speedment.code.TranslatorKey;
import com.speedment.codegen.Generator;
import com.speedment.codegen.model.Class;
import com.speedment.codegen.model.Constructor;
import com.speedment.codegen.model.Field;
import com.speedment.codegen.model.File;
import com.speedment.codegen.model.Generic;
import com.speedment.codegen.model.Import;
import com.speedment.codegen.model.Method;
import com.speedment.codegen.model.Type;
import com.speedment.config.db.Column;
import com.speedment.config.db.Table;
import com.speedment.config.db.mapper.TypeMapper;
import static com.speedment.internal.codegen.model.constant.DefaultAnnotationUsage.OVERRIDE;
import static com.speedment.internal.codegen.model.constant.DefaultType.VOID;
import static com.speedment.internal.codegen.util.Formatting.indent;
import static com.speedment.internal.codegen.util.Formatting.nl;
import com.speedment.internal.core.code.DefaultJavaClassTranslator;
import com.speedment.internal.core.code.TranslatorKeyImpl;
import com.speedment.manager.Manager;
import static com.speedment.util.CollectorUtil.unmodifiableSetOf;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;
import java.util.Set;

/**
 *
 * @author  Emil Forslund
 * @since   1.0.0
 */
public final class GeneratedTypeAdapterTranslator extends DefaultJavaClassTranslator<Table, Class> {

    public final static TranslatorKey<Table, Class> KEY = new TranslatorKeyImpl<>("GeneratedTypeAdapter", Class.class);
    public final static String DATE_FORMAT = "yyyy-MM-dd";
    
    private final static Set<java.lang.Class<?>> BUILT_IN_TYPES = unmodifiableSetOf(
        Long.class, long.class,
        Integer.class, int.class,
        Short.class, short.class,
        Byte.class, byte.class,
        Double.class, double.class,
        Float.class, float.class,
        Boolean.class, boolean.class,
        String.class
    );
    
    public GeneratedTypeAdapterTranslator(Speedment speedment, Generator gen, Table table) {
        super(speedment, gen, table, Class::of);
    }

    @Override
    protected Class makeCodeGenModel(File file) {
        return newBuilder(file, getClassOrInterfaceName())
            .forEveryTable((clazz, table) -> {
                // Define some common types
                final Type entityType = getSupport().entityType();
                final Type managerType = Type.of(Manager.class).add(Generic.of().add(entityType));
                final boolean allBuiltInTypes = table.columns()
                    .map(Column::findTypeMapper)
                    .map(TypeMapper::getJavaType)
                    .allMatch(BUILT_IN_TYPES::contains);
                
                // Import the requireNonNull()-method statically.
                file.add(Import.of(Type.of(Objects.class)).static_().setStaticMember("requireNonNull"));
                
                // Generate a consructor that sets the manager.
                final Constructor constr = Constructor.of()
                    .public_()
                    .add(Field.of("manager", managerType))
                    .add("this.manager = requireNonNull(manager);");
                
                clazz.public_().final_()
                    
                    // Set supertype
                    .setSupertype(Type.of(TypeAdapter.class).add(Generic.of().add(entityType)))
                    
                    // Add manager and gson as private members
                    .add(Field.of("manager", managerType)
                        .private_().final_()
                    )
                    
                    // Create constructor
                    .add(constr)
                    
                    // Add 'write'- and 'read'-methods
                    .add(writeMethod(table))
                    .add(readMethod(file, table));
                
                
                // If one or more fields are of a non-built-in type, we need to generate
                // a wrapped Gson instance to handle them.
                if (!allBuiltInTypes) {
                    clazz.add(Field.of("gson", Type.of(Gson.class))
                        .private_().final_()
                    );
                    
                    file.add(Import.of(Type.of(GsonBuilder.class)));
                    
                    constr.add("this.gson = new GsonBuilder().setDateFormat(\"" + DATE_FORMAT + "\").create();");
                }
            }).build();
    }
    
    private Method writeMethod(Table table) {
        final Type entityType = getSupport().entityType();
        
        final Method method = Method.of("write", VOID)
            .public_().add(OVERRIDE)
            .add(Type.of(IOException.class))
            .add(Field.of("out", Type.of(JsonWriter.class)))
            .add(Field.of("entity", entityType));
        
        method.add("out.beginObject();");
        
        table.columns().forEachOrdered(col -> {
            final java.lang.Class<?> type = col.findTypeMapper().getJavaType();
            final boolean isBoolean = type == Boolean.class 
                                   || type == boolean.class;
            final boolean isBuiltIn = BUILT_IN_TYPES.contains(type);
            
            final String getter = new StringBuilder("entity.")
                .append(isBoolean ? "is" : "get")
                .append(getNamer().javaTypeName(col.getJavaName()))
                .append("()")
                .toString();
            
            final StringBuilder setter = new StringBuilder();
            
            if (col.isNullable()) {
                setter.append("if (")
                    .append(getter)
                    .append(".isPresent()) ");
            }
            
            setter.append("out.name(\"")
                .append(getNamer().javaVariableName(col.getJavaName()))
                .append("\").")
                .append(isBuiltIn
                    ? "value("
                    : "jsonValue(gson.toJson("
                );
            
            setter.append(getter);
            
            if (col.isNullable()) {
                setter.append(".get()");
            }

            setter.append(isBuiltIn ? ");" : "));");

            method.add(setter.toString());
        });
        
        method.add("out.endObject();");
        return method;
    }
    
    private Method readMethod(File file, Table table) {
        file.add(Import.of(Type.of(JsonToken.class)));
        final Type entityType = getSupport().entityType();
        
        final Method method = Method.of("read", entityType)
            .public_().add(OVERRIDE)
            .add(Type.of(IOException.class))
            .add(Field.of("in", Type.of(JsonReader.class)))
            .add(
                "in.beginObject();",
                "final " + getSupport().typeName() + " inst = manager.newEmptyEntity();",
                "",
                "while (in.hasNext()) {",
                indent("switch (in.nextName()) {")
            );
        
        table.columns().forEachOrdered(col -> {
            final String varName = getNamer().javaVariableName(col.getJavaName());
        
            method.add(indent(
                "case \"" + varName + "\" : " + (
                    col.isNullable() ? (nl() + indent(
                        "if (in.peek() == JsonToken.NULL) in.skipValue();" + nl() +
                        "else " + next(file, col)
                    )) : next(file, col)
                ), 2
            ));
        });
        
        method.add(
            indent("}"),
            "}",
            "",
            "in.endObject();",
            "return inst;"
        );
        
        return method;
    }
    
    private String next(File file, Column col) {
        final String typeName = getNamer().javaTypeName(col.getJavaName());
        final java.lang.Class<?> type = col.findTypeMapper().getJavaType();
        final StringBuilder body = new StringBuilder()
            .append("inst.set")
            .append(typeName)
            .append("(");
        
        if (type == Long.class || type == long.class) {
            body.append("in.nextLong()");
        } else if (type == Boolean.class || type == boolean.class) {
            body.append("in.nextBoolean()");
        } else if (type == Integer.class || type == int.class
                || type == Short.class || type == short.class
                || type == Byte.class || type == byte.class
                || type == BigInteger.class) {
            body.append("in.nextInt()");
        } else if (type == Double.class || type == double.class
                || type == Float.class || type == float.class
                || type == BigDecimal.class) {
            body.append("in.nextDouble()");
        } else if (type == String.class) {
            body.append("in.nextString()");
        } else {
            file.add(Import.of(Type.of(type)));
            body.append("gson.fromJson(in.nextString(), ")
                .append(type.getSimpleName())
                .append(".class)");
        }
        
        return body.append("); break;").toString();
    }
    
    @Override
    protected String getClassOrInterfaceName() {
        return "Generated" + getSupport().typeName() + "TypeAdapter";
    }

    @Override
    protected String getJavadocRepresentText() {
        return "A Gson Type Adapter";
    }

    @Override
    public boolean isInGeneratedPackage() {
        return true;
    }
}