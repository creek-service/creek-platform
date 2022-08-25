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

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;
import static org.creekservice.api.platform.metadata.ResourceDescriptor.isOwned;
import static org.creekservice.api.platform.metadata.ResourceDescriptor.isShared;
import static org.creekservice.api.platform.metadata.ResourceDescriptor.isUnmanaged;
import static org.creekservice.api.platform.metadata.ResourceDescriptor.isUnowned;
import static org.creekservice.api.platform.resource.ComponentValidator.componentValidator;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.creekservice.api.base.annotation.VisibleForTesting;
import org.creekservice.api.base.type.CodeLocation;
import org.creekservice.api.platform.metadata.ComponentDescriptor;
import org.creekservice.api.platform.metadata.ResourceDescriptor;
import org.creekservice.api.platform.metadata.ResourceHandler;

/**
 * Initializer of resources.
 *
 * <p>Given one of more {@link ComponentDescriptor component descriptors}, the initializer can
 * determine which resources should be initialized at the different stages of initialization: {@link
 * #init}, {@link #service} and {@link #test}.
 *
 * <p>The initializer will also validate the supplied component and resource descriptors.
 *
 * <p>For more information on resource initialization, see
 * https://github.com/creek-service/creek-platform/tree/main/metadata#resource-initialization.
 */
public final class ResourceInitializer {

    private final ResourceHandlers handlers;
    private final ComponentValidator componentValidator;

    @FunctionalInterface
    public interface ResourceHandlers {
        <T extends ResourceDescriptor> ResourceHandler<T> get(Class<T> type);
    }

    /**
     * Create an initializer instance.
     *
     * @param handlers accessor to resource handlers, as exposed by Creek extensions.
     * @return an initializer instance.
     */
    public static ResourceInitializer resourceInitializer(final ResourceHandlers handlers) {
        return new ResourceInitializer(handlers, componentValidator());
    }

    @VisibleForTesting
    ResourceInitializer(
            final ResourceHandlers handlers, final ComponentValidator componentValidator) {
        this.handlers = requireNonNull(handlers, "handlers");
        this.componentValidator = requireNonNull(componentValidator, "componentValidator");
    }

    public void init(final Stream<ComponentDescriptor> components) {
        ensureResources(
                groupById(
                        components,
                        resourceGroup -> resourceGroup.anyMatch(ResourceDescriptor::isShared)));
    }

    public void service(final Stream<ComponentDescriptor> components) {
        ensureResources(
                groupById(
                        components,
                        resourceGroup -> resourceGroup.anyMatch(ResourceDescriptor::isOwned)));
    }

    public void test(
            final Stream<ComponentDescriptor> componentsUnderTest,
            final Stream<ComponentDescriptor> otherComponents) {

        final Map<URI, List<ResourceDescriptor>> unowned =
                groupById(
                                componentsUnderTest,
                                resourceGroup ->
                                        resourceGroup.noneMatch(ResourceDescriptor::isOwned))
                        .collect(Collectors.toMap(group -> group.get(0).id(), Function.identity()));

        groupById(
                        otherComponents,
                        resourceGroup -> resourceGroup.anyMatch(r -> unowned.containsKey(r.id())))
                .forEach(
                        resourceGroup ->
                                unowned.get(resourceGroup.get(0).id()).addAll(resourceGroup));

        final Stream<List<ResourceDescriptor>> stream = unowned.values().stream();
        ensureResources(stream);
    }

    private void ensureResources(final Stream<List<ResourceDescriptor>> resourceGroups) {
        resourceGroups
                .peek(this::validateResourceGroup)
                .map(this::creatableDescriptor)
                .collect(groupingBy(this::resourceHandler))
                .forEach(ResourceHandler::ensure);
    }

    private ResourceDescriptor creatableDescriptor(final List<ResourceDescriptor> resourceGroup) {
        return resourceGroup.stream()
                .filter(ResourceDescriptor::isCreatable)
                .findAny()
                .orElseThrow(() -> new UncreatableResourceException(resourceGroup));
    }

    private Stream<List<ResourceDescriptor>> groupById(
            final Stream<ComponentDescriptor> componentsUnderTest,
            final Predicate<Stream<ResourceDescriptor>> groupPredicate) {
        return componentsUnderTest
                .flatMap(this::getResources)
                .collect(groupingBy(ResourceDescriptor::id))
                .values()
                .stream()
                .filter(l -> groupPredicate.test(l.stream()));
    }

    private Stream<ResourceDescriptor> getResources(final ComponentDescriptor component) {
        componentValidator.validate(component);
        return component.resources();
    }

    /**
     * Validate a group of descriptors that describe the same resource, ensuring they are
     * consistent.
     *
     * <p>When dealing with multiple components, the same resource can be described by multiple
     * descriptors. All descriptors should agree on the details of the resource.
     *
     * @param resourceGroup the group of descriptors that describe the same resource.
     */
    private <T extends ResourceDescriptor> void validateResourceGroup(final List<T> resourceGroup) {
        if (isShared(resourceGroup.get(0))) {
            // if shared, all should be shared.
            if (resourceGroup.stream().anyMatch(r -> !isShared(r))) {
                throw new ResourceDescriptorMismatchInitializationException(
                        "shared", resourceGroup);
            }
        } else if (isUnmanaged(resourceGroup.get(0))) {
            // if unmanaged, all should be unmanaged.
            if (resourceGroup.stream().anyMatch(r -> !isUnmanaged(r))) {
                throw new ResourceDescriptorMismatchInitializationException(
                        "unmanaged", resourceGroup);
            }
        } else {
            // if owned/unowned mixes allowed.
            if (resourceGroup.stream().anyMatch(r -> !(isOwned(r) || isUnowned(r)))) {
                throw new ResourceDescriptorMismatchInitializationException(
                        "owned or unowned", resourceGroup);
            }
        }

        resourceHandler(resourceGroup.get(0)).validate(resourceGroup);
    }

    @SuppressWarnings("unchecked")
    private <T extends ResourceDescriptor> ResourceHandler<T> resourceHandler(final T resource) {
        return handlers.get((Class<T>) resource.getClass());
    }

    private static String formatResource(final List<? extends ResourceDescriptor> descriptors) {
        return descriptors.stream()
                .map(ResourceInitializer::formatResource)
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private static String formatResource(final ResourceDescriptor descriptor) {
        return "(" + CodeLocation.codeLocation(descriptor) + ") " + descriptor;
    }

    private static class ResourceDescriptorMismatchException extends RuntimeException {
        ResourceDescriptorMismatchException(
                final String msg, final List<? extends ResourceDescriptor> descriptors) {
            super(
                    msg
                            + ". resource: "
                            + descriptors.get(0).id()
                            + ", descriptors: "
                            + formatResource(descriptors));
        }
    }

    private static final class ResourceDescriptorMismatchInitializationException
            extends ResourceDescriptorMismatchException {
        ResourceDescriptorMismatchInitializationException(
                final String type, final List<? extends ResourceDescriptor> descriptors) {
            super(
                    "Resource descriptors for resource are tagged with incompatible resource initialization marker "
                            + "interfaces. First descriptor is marked as a "
                            + type
                            + " resource, "
                            + "but at least one subsequent descriptor was not "
                            + type,
                    descriptors);
        }
    }

    private static final class UncreatableResourceException extends RuntimeException {
        UncreatableResourceException(final List<? extends ResourceDescriptor> descriptors) {
            super(
                    "No component provided a creatable descriptor for resource id: "
                            + descriptors.get(0).id()
                            + ", descriptors: "
                            + formatResource(descriptors));
        }
    }
}
