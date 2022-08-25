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

package org.creekservice.api.platform.metadata;


import java.util.Collection;

/**
 * A Callback that Creek extensions implement to handler the resource types they support.
 *
 * @param <T> the specific resource type the handler handles.
 */
public interface ResourceHandler<T extends ResourceDescriptor> {

    /**
     * Validate a group of descriptors that should reference the same resource.
     *
     * <p>First the method performs a one time validation of each resource instance, called before
     * any other methods, to allow the extension to validate each descriptor instance. This is
     * necessary as resources are implemented by client code and therefore could be invalid.
     *
     * <p>Second, where the group contains more than one descriptor, the method validates that each
     * descriptor in the group agrees on the details of the resource. If descriptors differ on the
     * details it means something is out of whack in the system, and this needs resolving before
     * things can continue.
     *
     * @param resourceGroup a collection of resource descriptors that should all describe the same
     *     resource.
     * @throws RuntimeException implementations should throw a suitable exception type on any
     *     validation failures, providing enough information for users to resolve the issue.
     */
    void validate(Collection<T> resourceGroup);

    /**
     * Ensure the supplied {@code resources} exists.
     *
     * <p>Instructs an extension to ensure the resources described by the supplied descriptor exist
     * and are initialized.
     *
     * <p>Implementations should consider outputting a warning or failing if the resource alreay
     * exists, but does not match the expected configuration.
     *
     * @param resources the resource instances to ensure exists and are initialized. Resources must
     *     be {@link ResourceDescriptor#isCreatable creatable}.
     */
    void ensure(Collection<T> resources);
}
