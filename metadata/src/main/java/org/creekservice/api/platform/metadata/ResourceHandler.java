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
     * Ensure the supplied {@code resources} exists.
     *
     * <p>Instructs an extension to ensure the resources described by the supplied descriptor exist
     * and are initialized.
     *
     * <p>Implementations should consider outputting a warning or failing if the resource already
     * exists, but does not match the expected configuration.
     *
     * @param resources the resource instances to ensure exists and are initialized. Resources
     *     passed will be {@link ResourceDescriptor#isCreatable creatable}.
     */
    void ensure(Collection<? extends T> resources);
}
