/*
 * Copyright 2023 Creek Contributors (https://github.com/creek-service)
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

/** Dependency free module containing base interfaces for defining components */
import org.creekservice.api.platform.metadata.ComponentDescriptor;

/**
 * Platform Metadata Module.
 *
 * <p>A dependency-free module that defines the interfaces used to describe platform components and
 * resources.
 */
module creek.platform.metadata {
    exports org.creekservice.api.platform.metadata;

    uses ComponentDescriptor;
}
