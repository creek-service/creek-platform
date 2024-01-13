/*
 * Copyright 2022-2024 Creek Contributors (https://github.com/creek-service)
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

/**
 * Base type for an owned resources.
 *
 * <p>A resource can conceptually be either:
 *
 * <ul>
 *   <li>{@link OwnedResource owned}: owned by the service.
 *   <li>{@link UnownedResource unowned}: owned by another service.
 *   <li>{@link SharedResource shared}: shared, i.e. owned by no specific service (rare).
 *   <li>{@code unmanaged}: if not marked by one of the above it is considered not managed by Creek.
 * </ul>
 *
 * <p>Resources should inherit <b>at most</b> one of the above interfaces.
 *
 * <p>For more information on resource initialization, see <a
 * href="https://github.com/creek-service/creek-platform/tree/main/metadata#resource-initialization">resource-initialization</a>.
 */
public interface OwnedResource extends CreatableResource {}
