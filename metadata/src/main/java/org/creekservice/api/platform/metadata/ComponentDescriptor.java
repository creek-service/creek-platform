/*
 * Copyright 2021-2022 Creek Contributors (https://github.com/creek-service)
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


import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * Defines metadata about a platform component
 *
 * <p>Creek system tests will look for components using {@link java.util.ServiceLoader} to load
 * instances of this type from the class & module paths. Therefore, to be loaded by Creek system
 * test the component must:
 *
 * <ul>
 *   <li>be listed in the {@code module-info.java} file as a {@code provider} of {@link
 *       ComponentDescriptor}, if using JPMS, or
 *   <li>have a suitable entry in the {@code META-INFO.services} directory, or
 *   <li>both of the above
 * </ul>
 */
public interface ComponentDescriptor {

    /** @return the unique name of the component within the platform */
    String name();

    /** @return the inputs to the component, e.g. Kafka topics it consumes. */
    default Collection<ComponentInput> inputs() {
        return List.of();
    }

    /** @return the internals to the component, e.g. changelog or repartition Kafka topics. */
    default Collection<ComponentInternal> internals() {
        return List.of();
    }

    /** @return the outputs from the component, e.g. the Kafka topics it outputs too */
    default Collection<ComponentOutput> outputs() {
        return List.of();
    }

    /**
     * @return {@link Stream} of component {@link #inputs}, {@link #internals} and {@link #outputs}
     *     resources.
     */
    default Stream<ResourceDescriptor> resources() {
        return Stream.concat(
                inputs().stream(), Stream.concat(internals().stream(), outputs().stream()));
    }
}
