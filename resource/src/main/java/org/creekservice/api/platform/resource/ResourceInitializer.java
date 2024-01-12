/*
 * Copyright 2022-2023 Creek Contributors (https://github.com/creek-service)
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
import static java.util.stream.Collectors.toList;
import static org.creekservice.api.platform.metadata.ResourceDescriptor.isUnmanaged;

import java.net.URI;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.creekservice.api.base.annotation.VisibleForTesting;
import org.creekservice.api.base.type.CodeLocation;
import org.creekservice.api.observability.logging.structured.StructuredLogger;
import org.creekservice.api.observability.logging.structured.StructuredLoggerFactory;
import org.creekservice.api.platform.metadata.ComponentDescriptor;
import org.creekservice.api.platform.metadata.CreatableResource;
import org.creekservice.api.platform.metadata.OwnedResource;
import org.creekservice.api.platform.metadata.ResourceCollection;
import org.creekservice.api.platform.metadata.ResourceDescriptor;
import org.creekservice.api.platform.metadata.SharedResource;
import org.creekservice.api.platform.metadata.UnownedResource;
import org.creekservice.internal.platform.resource.ComponentValidator;

/**
 * Initializer of resources.
 *
 * <p>Given one of more {@link ComponentDescriptor component descriptors}, the initializer can
 * determine which resources should be initialized at the different stages of initialization: {@link
 * #init}, {@link #service} and {@link #test}.
 *
 * <p>The initializer will also validate the supplied component and resource descriptors.
 *
 * <p>For more information on resource initialization, see <a
 * href="https://github.com/creek-service/creek-platform/tree/main/metadata#resource-initialization">the
 * docs</a>.
 */
public final class ResourceInitializer {

    private static final StructuredLogger LOGGER =
            StructuredLoggerFactory.internalLogger(ResourceInitializer.class);

    private final Callbacks callbacks;
    private final ComponentValidator componentValidator;

    /** Type for supplying callbacks to the resource initializer. */
    public interface Callbacks {

        /**
         * Callback to validate that all the supplied {@code resources} consistently represent the
         * same resource.
         *
         * <p>There is often multiple resource descriptors describing the same resource present in
         * the system. For example, both input and output descriptors for the same resource. Any
         * inconsistencies in the details of the resource between these descriptors can lead to
         * bugs.
         *
         * @param type the type of the {@code resources}.
         * @param resources the set of resources that all share the same {@link
         *     ResourceDescriptor#id()}.
         * @param <T> the resource descriptor type
         * @throws RuntimeException with the details of any inconsistencies.
         */
        <T extends ResourceDescriptor> void validate(Class<T> type, Collection<T> resources);

        /**
         * Callback to ensure the supplied {@code creatableResources} exist.
         *
         * <p>All {@code creatableResources} will be of the same type.
         *
         * @param type the type of the {@code creatableResources}.
         * @param creatableResources the resource instances to ensure exists and are initialized.
         * @param <T> the creatable resource descriptor type
         * @throws RuntimeException on unknown resource type
         */
        <T extends CreatableResource> void ensure(Class<T> type, Collection<T> creatableResources);
    }

    /**
     * Create an initializer instance.
     *
     * @param callbacks callbacks used to ensure external resources exist and are valid, as exposed
     *     by Creek extensions.
     * @return an initializer instance.
     */
    public static ResourceInitializer resourceInitializer(final Callbacks callbacks) {
        return new ResourceInitializer(callbacks, new ComponentValidator());
    }

    @VisibleForTesting
    ResourceInitializer(final Callbacks callbacks, final ComponentValidator componentValidator) {
        this.callbacks = requireNonNull(callbacks, "callbacks");
        this.componentValidator = requireNonNull(componentValidator, "componentValidator");
    }

    /**
     * Initialize resources that should be created during the init stage.
     *
     * <p>Only resource's that require initialisation at this stage will have their descriptors
     * validated.
     *
     * @param components components to search for resources.
     */
    public void init(final Collection<? extends ComponentDescriptor> components) {
        components.forEach(componentValidator::validate);

        LOGGER.debug(
                "Initializing resources",
                log -> log.with("stage", "init").with("components", componentNames(components)));

        ensureResources(
                groupById(
                        components,
                        resGroup -> resGroup.stream().anyMatch(SharedResource.class::isInstance),
                        false));
    }

    /**
     * Initialize resources that should be created during the service stage.
     *
     * <p>All resource's will have their descriptors validated.
     *
     * @param components components to search for resources.
     */
    public void service(final Collection<? extends ComponentDescriptor> components) {
        components.forEach(componentValidator::validate);

        LOGGER.debug(
                "Initializing resources",
                log -> log.with("stage", "service").with("components", componentNames(components)));

        ensureResources(
                groupById(
                        components,
                        resGroup -> resGroup.stream().anyMatch(OwnedResource.class::isInstance),
                        true));
    }

    /**
     * Initialize resources that should be created during the test stage.
     *
     * <p>Components under test will have all their resource descriptors validated.
     *
     * @param componentsUnderTest components that are being testing
     * @param otherComponents other components surrounding those being tested, e.g. upstream and
     *     downstream components. These components can contain {@link CreatableResource creatable}
     *     resource descriptors needed to know how to create edge resources.
     */
    public void test(
            final Collection<? extends ComponentDescriptor> componentsUnderTest,
            final Collection<? extends ComponentDescriptor> otherComponents) {
        componentsUnderTest.forEach(componentValidator::validate);
        otherComponents.forEach(componentValidator::validate);

        LOGGER.debug(
                "Initializing resources",
                log ->
                        log.with("stage", "test")
                                .with("components_under_test", componentNames(componentsUnderTest))
                                .with("other_components", componentNames(otherComponents)));

        final Map<URI, List<ResourceDescriptor>> unowned =
                groupById(
                                componentsUnderTest,
                                resGroup ->
                                        resGroup.stream()
                                                        .anyMatch(UnownedResource.class::isInstance)
                                                && resGroup.stream()
                                                        .noneMatch(OwnedResource.class::isInstance),
                                true)
                        .collect(Collectors.toMap(group -> group.get(0).id(), Function.identity()));

        groupById(
                        otherComponents,
                        resGroup -> resGroup.stream().anyMatch(r -> unowned.containsKey(r.id())),
                        false)
                .forEach(resGroup -> unowned.get(resGroup.get(0).id()).addAll(resGroup));

        ensureResources(unowned.values().stream());
    }

    private void ensureResources(final Stream<List<ResourceDescriptor>> resGroups) {
        resGroups
                .peek(this::validateResourceGroup)
                .map(this::creatableDescriptor)
                .collect(groupingBy(Object::getClass, LinkedHashMap::new, toList()))
                .values()
                .forEach(this::ensure);
    }

    @SuppressWarnings("unchecked")
    private <T extends CreatableResource> void ensure(final List<T> creatableResources) {
        final Class<T> type = (Class<T>) creatableResources.get(0).getClass();
        callbacks.ensure(type, creatableResources);
    }

    private CreatableResource creatableDescriptor(final List<ResourceDescriptor> resGroup) {
        return resGroup.stream()
                .filter(CreatableResource.class::isInstance)
                .map(CreatableResource.class::cast)
                .findAny()
                .orElseThrow(() -> new UncreatableResourceException(resGroup));
    }

    private Stream<List<ResourceDescriptor>> groupById(
            final Collection<? extends ComponentDescriptor> components,
            final Predicate<List<ResourceDescriptor>> groupPredicate,
            final boolean validateNonMatchingResGroups) {
        final Map<URI, List<ResourceDescriptor>> grouped =
                components.stream()
                        .flatMap(ResourceCollection::collectResources)
                        .collect(groupingBy(ResourceDescriptor::id, LinkedHashMap::new, toList()));

        final Map<Boolean, List<List<ResourceDescriptor>>> partitioned =
                grouped.values().stream().collect(groupingBy(groupPredicate::test));

        if (validateNonMatchingResGroups) {
            partitioned.getOrDefault(false, List.of()).forEach(this::validateResourceGroup);
        }

        return partitioned.getOrDefault(true, List.of()).stream();
    }

    /**
     * Validate a group of descriptors that describe the same resource, ensuring they are consistent
     * on how the resource is initialized.
     *
     * @param resourceGroup the group of descriptors that describe the same resource.
     */
    @SuppressWarnings("unchecked")
    private <T extends ResourceDescriptor> void validateResourceGroup(final List<T> resourceGroup) {
        final T first = resourceGroup.get(0);
        if (first instanceof SharedResource) {
            // if shared, all should be shared.
            if (resourceGroup.stream().anyMatch(r -> !(r instanceof SharedResource))) {
                throw new ResourceDescriptorMismatchInitializationException(
                        "shared", resourceGroup);
            }
        } else if (isUnmanaged(first)) {
            // if unmanaged, all should be unmanaged.
            if (resourceGroup.stream().anyMatch(r -> !isUnmanaged(r))) {
                throw new ResourceDescriptorMismatchInitializationException(
                        "unmanaged", resourceGroup);
            }
        } else {
            // if owned/unowned mixes allowed.
            if (resourceGroup.stream()
                    .anyMatch(r -> !(r instanceof OwnedResource || r instanceof UnownedResource))) {
                throw new ResourceDescriptorMismatchInitializationException(
                        "owned or unowned", resourceGroup);
            }
        }

        callbacks.validate((Class<T>) first.getClass(), resourceGroup);
    }

    private static String formatResource(final List<? extends ResourceDescriptor> descriptors) {
        return descriptors.stream()
                .map(ResourceInitializer::formatResource)
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private static String formatResource(final ResourceDescriptor descriptor) {
        return "(" + CodeLocation.codeLocation(descriptor) + ") " + descriptor;
    }

    private static List<String> componentNames(
            final Collection<? extends ComponentDescriptor> components) {
        return components.stream().map(ComponentDescriptor::name).collect(Collectors.toList());
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
                    "Resource descriptors for resource are tagged with incompatible resource"
                            + " initialization interfaces. First descriptor is marked as a "
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
                            + ", known_creatable_descriptors: "
                            + formatResource(descriptors));
        }
    }
}
