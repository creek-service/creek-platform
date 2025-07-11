/*
 * Copyright 2023-2025 Creek Contributors (https://github.com/creek-service)
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

package org.creekservice.api.platform.metadata;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

/** A collection of resources. */
public interface ResourceCollection {

    /**
     * The stream of the resources in the collection.
     *
     * <p>In general, callers should call {@link #collectResources(ResourceCollection)}, rather than
     * directly calling this method.
     *
     * @return the resources in the collection.
     */
    Stream<? extends ResourceDescriptor> resources();

    /**
     * Utility method to collect all resources from a collection, including any resources referenced
     * by any resources encountered.
     *
     * <p>Child resources are returned earlier in the stream than parents.
     *
     * @param collection the resource collection to retrieve all resources from.
     * @return complete stream of resources.
     */
    static Stream<ResourceDescriptor> collectResources(final ResourceCollection collection) {
        final Set<ResourceCollection> visited = new HashSet<>();
        visited.add(collection);
        return collection.resources().flatMap(child -> collectResources(child, visited));
    }

    /**
     * Utility method to collect all resources referenced by a {@code ResourceDescriptor}, including
     * any resources referenced by any resources encountered.
     *
     * <p>Child resources are returned earlier in the stream than parents.
     *
     * <p>In general, callers should call {@link #collectResources(ResourceCollection)}, rather than
     * directly calling this method.
     *
     * @param resource the resource descriptor to retrieve all resources from.
     * @param visited the set of resource containers already visited.
     * @return complete stream of resources.
     */
    static Stream<ResourceDescriptor> collectResources(
            final ResourceDescriptor resource, final Set<ResourceCollection> visited) {
        if (!visited.add(resource)) {
            // Already processed:
            return Stream.empty();
        }

        return Stream.concat(
                resource.resources().flatMap(child -> collectResources(child, visited)),
                Stream.of(resource));
    }
}
