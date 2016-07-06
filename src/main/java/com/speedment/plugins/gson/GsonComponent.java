package com.speedment.plugins.gson;

import com.speedment.Speedment;
import com.speedment.code.StandardTranslatorKey;
import com.speedment.component.CodeGenerationComponent;
import com.speedment.component.Component;
import com.speedment.config.db.Table;
import com.speedment.internal.core.platform.component.impl.AbstractComponent;
import com.speedment.internal.license.AbstractSoftware;
import static com.speedment.internal.license.OpenSourceLicense.APACHE_2;
import com.speedment.license.Software;

/**
 *
 * @author  Emil Forslund
 * @since   1.0.0
 */
public final class GsonComponent extends AbstractComponent {

    public GsonComponent(Speedment speedment) {
        super(speedment);
    }

    @Override
    public void onResolve() {
        final CodeGenerationComponent code = getSpeedment().getCodeGenerationComponent();
        code.put(Table.class, GeneratedTypeAdapterTranslator.KEY, GeneratedTypeAdapterTranslator::new);
        code.add(Table.class, StandardTranslatorKey.GENERATED_MANAGER, new GeneratedManagerDecorator());
        code.add(Table.class, StandardTranslatorKey.GENERATED_MANAGER_IMPL, new GeneratedManagerImplDecorator());
    }

    @Override
    public Class<GsonComponent> getComponentClass() {
        return GsonComponent.class;
    }

    @Override
    public Software asSoftware() {
        return AbstractSoftware.with("Gson Plugin", "1.0", APACHE_2);
    }

    @Override
    public Component defaultCopy(Speedment speedment) {
        return new GsonComponent(speedment);
    }
}