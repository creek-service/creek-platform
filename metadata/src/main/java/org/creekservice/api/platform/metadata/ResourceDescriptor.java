/*
 * Copyright 2021-2024 Creek Contributors (https://github.com/creek-service)
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

import java.net.URI;
import java.util.stream.Stream;

/** Base type for describing resource. */
public interface ResourceDescriptor extends ResourceCollection {

    /**
     * A unique identifier for this resource.
     *
     * <p>To avoid URI clashes between resources from different extensions, the URI scheme will
     * normally be prefixed with the name of the extension that managed the resource. For example,
     * the Kafka extension's topic resource's scheme is {@code kafka-topic}.
     *
     * <p>The form of the rest of the URI is up the extension implementer.
     *
     * <p>The core Creek system will use the id to determine if two descriptors refer to the same
     * resource. It will not inspect or use the parts of the URI.
     *
     * <p>{@link ComponentInput Input}, {@link ComponentInternal internal} and {@link
     * ComponentOutput output} resource descriptors that refer to the same underlying resource must
     * return the same URI.
     *
     * @return unique identifier for the resource.
     */
    URI id();

    /**
     * Additional resources this resource requires.
     *
     * @return stream of resources this resource depends on.
     */
    default Stream<? extends ResourceDescriptor> resources() {
        return Stream.of();
    }

    /**
     * Determine if a resource descriptor is unmanaged.
     *
     * @param r the r descriptor to check.
     * @return {@code true} if unmanaged, {@code false} otherwise.
     */
    static boolean isUnmanaged(final ResourceDescriptor r) {
        return !(r instanceof SharedResource
                || r instanceof OwnedResource
                || r instanceof UnownedResource);
    }
}
