/*
 * Copyright 2022 Creek Contributors (https://github.com/creek-service)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.creekservice.api.platform.resource;


import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import org.creekservice.api.base.type.CodeLocation;
import org.creekservice.api.platform.metadata.ComponentDescriptor;
import org.creekservice.api.platform.metadata.ResourceDescriptor;

public final class ComponentValidator {

    private static final Pattern CTRL_CHAR = Pattern.compile("\\p{Cntrl}");

    public static ComponentValidator componentValidator() {
        return new ComponentValidator();
    }

    private ComponentValidator() {}

    public void validate(final ComponentDescriptor... components) {
        validate(List.of(components));
    }

    public void validate(final Collection<? extends ComponentDescriptor> components) {
        components.forEach(this::validateComponent);
    }

    private void validateComponent(final ComponentDescriptor component) {
        validateComponentName(component);
        validateComponentResources(component);
    }

    private void validateComponentName(final ComponentDescriptor component) {
        if (component.name() == null || component.name().isBlank()) {
            throw new InvalidDescriptorException("name can not be null or blank", component, false);
        }
        if (CTRL_CHAR.matcher(component.name()).find()) {
            throw new InvalidDescriptorException(
                    "name can not contain control characters", component);
        }
    }

    private void validateComponentResources(final ComponentDescriptor component) {
        validateResourcesMethod(component);
        component.resources().forEach(r -> validateResource(r, component));
    }

    private void validateResource(
            final ResourceDescriptor resource, final ComponentDescriptor component) {
        if (resource == null) {
            throw new InvalidDescriptorException("contains null resource", component);
        }
    }

    private void validateResourcesMethod(final ComponentDescriptor component) {
        try {
            final Method m = component.getClass().getMethod("resources");
            if (!m.getDeclaringClass().equals(ComponentDescriptor.class)) {
                throw new InvalidDescriptorException(
                        "should not override resources() method", component);
            }
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("no resources method", e);
        }
    }

    private static final class InvalidDescriptorException extends RuntimeException {
        InvalidDescriptorException(final String msg, final ComponentDescriptor component) {
            this(msg, component, true);
        }

        InvalidDescriptorException(
                final String msg,
                final ComponentDescriptor component,
                final boolean useComponentName) {
            super(
                    msg
                            + ", component ("
                            + CodeLocation.codeLocation(component)
                            + "): "
                            + (useComponentName ? component.name() : component.toString()));
        }
    }
}
