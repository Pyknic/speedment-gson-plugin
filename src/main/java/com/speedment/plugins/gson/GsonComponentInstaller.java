package com.speedment.plugins.gson;

import com.speedment.Speedment;
import com.speedment.component.ComponentConstructor;

/**
 *
 * @author  Emil Forslund
 * @since   1.0.0
 */
public final class GsonComponentInstaller implements ComponentConstructor<GsonComponent> {

    @Override
    public GsonComponent create(Speedment speedment) {
        return new GsonComponent(speedment);
    }
}