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

package org.creekservice.internal.platform.resource;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.creekservice.api.base.type.CodeLocation;
import org.creekservice.api.platform.metadata.AggregateDescriptor;
import org.creekservice.api.platform.metadata.ComponentDescriptor;
import org.creekservice.api.platform.metadata.OwnedResource;
import org.creekservice.api.platform.metadata.ResourceDescriptor;
import org.creekservice.api.platform.metadata.ServiceDescriptor;
import org.creekservice.api.platform.metadata.SharedResource;
import org.creekservice.api.platform.metadata.UnownedResource;

/**
 * Validator of components.
 *
 * <p>The validate performs basic validate on components to ensure they are valid, e.g. methods
 * return valid values, i.e. non-null, not-empty, etc.
 */
public final class ComponentValidator {

    private static final Pattern CTRL_CHAR = Pattern.compile("\\p{Cntrl}");

    /**
     * Validate one or more components
     *
     * @param components the components to validate.
     */
    public void validate(final ComponentDescriptor... components) {
        Arrays.stream(components).forEach(this::validateComponent);
    }

    private void validateComponent(final ComponentDescriptor component) {
        validateComponentName(component);

        final boolean isAggregate = component instanceof AggregateDescriptor;
        final boolean isService = component instanceof ServiceDescriptor;
        if (isAggregate && isService) {
            throw new InvalidDescriptorException(
                    "descriptor is both aggregate and service descriptor", component);
        }
        if (!isAggregate && !isService) {
            throw new InvalidDescriptorException(
                    "descriptor is neither aggregate and service descriptor", component);
        }
        if (component instanceof AggregateDescriptor) {
            validateAggregate((AggregateDescriptor) component);
        } else {
            validateService((ServiceDescriptor) component);
        }

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

    private void validateAggregate(final AggregateDescriptor component) {
        if (!component.internals().isEmpty()) {
            throw new InvalidDescriptorException(
                    "Aggregate should not expose internal resources. internals: "
                            + component.internals(),
                    component);
        }

        final List<ResourceDescriptor> notOwned =
                component
                        .resources()
                        .filter(r -> !(r instanceof OwnedResource))
                        .collect(Collectors.toList());

        if (!notOwned.isEmpty()) {
            throw new InvalidDescriptorException(
                    "Aggregate should only expose OwnedResource. not_owned: " + notOwned,
                    component);
        }
    }

    private void validateService(final ServiceDescriptor component) {
        if (component.dockerImage() == null || component.dockerImage().isBlank()) {
            throw new InvalidDescriptorException("dockerImage can not be null or blank", component);
        }

        if (component.testEnvironment() == null) {
            throw new InvalidDescriptorException("testEnvironment can not be null", component);
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

    private void validateResource(
            final ResourceDescriptor resource, final ComponentDescriptor component) {
        if (resource == null) {
            throw new InvalidDescriptorException("contains null resource", component);
        }

        if (resource.id() == null) {
            throw new InvalidDescriptorException(
                    "null resource id, resource_type: " + resource.getClass().getSimpleName(),
                    component);
        }

        final List<String> initialisation =
                types(resource.getClass())
                        .filter(ComponentValidator::isResourceInitializationMarkerInterface)
                        .map(Class::getSimpleName)
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList());
        if (initialisation.size() > 1) {
            throw new InvalidDescriptorException(
                    "resource can implement at-most one ResourceInitialization marker interface, but was: "
                            + initialisation,
                    resource,
                    component);
        }
    }

    private static boolean isResourceInitializationMarkerInterface(final Class<?> type) {
        return type == OwnedResource.class
                || type == UnownedResource.class
                || type == SharedResource.class;
    }

    private static Stream<Class<?>> types(final Class<?> type) {
        final Class<?> superclass = type.getSuperclass();

        final Stream<Class<?>> types = superclass == null ? Stream.of() : types(superclass);

        final Stream<Class<?>> interfaces =
                Arrays.stream(type.getInterfaces()).flatMap(ComponentValidator::types);

        return Stream.concat(Stream.of(type), Stream.concat(types, interfaces));
    }

    private static final class InvalidDescriptorException extends RuntimeException {
        InvalidDescriptorException(final String msg, final ComponentDescriptor component) {
            this(msg, component, true);
        }

        InvalidDescriptorException(
                final String msg,
                final ResourceDescriptor resource,
                final ComponentDescriptor component) {
            this(msg + ", resource: " + resource.id(), component, true);
        }

        InvalidDescriptorException(
                final String msg,
                final ComponentDescriptor component,
                final boolean useComponentName) {
            super(
                    msg
                            + ", component: "
                            + (useComponentName ? component.name() : component.toString())
                            + " ("
                            + CodeLocation.codeLocation(component)
                            + ")");
        }
    }
}
