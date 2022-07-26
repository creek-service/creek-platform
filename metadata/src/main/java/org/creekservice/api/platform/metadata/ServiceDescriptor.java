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

import static org.creekservice.internal.platform.metadata.Components.defaultNaming;

import java.util.Map;

/** Type defining metadata data about a service. */
public interface ServiceDescriptor extends ComponentDescriptor {

    @Override
    default String name() {
        return defaultNaming(this, "Descriptor");
    }

    /**
     * The name of the docker image that contains the service, without version info.
     *
     * <p>For example, given a full image name of {@code confluentinc/cp-kafka:6.1.2}, this method
     * should return {@code confluentinc/cp-kafka}.
     *
     * @return the service's docker image name.
     */
    String dockerImage();

    /**
     * Allows customisation of the environment variables available to the service during system
     * testing.
     */
    default Map<String, String> testEnvironment() {
        return Map.of();
    }
}
