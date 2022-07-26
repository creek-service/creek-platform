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

package org.creekservice.internal.platform.metadata;


import java.util.Arrays;
import org.creekservice.api.platform.metadata.ComponentDescriptor;

public final class Components {

    private Components() {}

    public static String defaultNaming(
            final ComponentDescriptor descriptor, final String... supportedPostFixes) {
        final String className = descriptor.getClass().getSimpleName();
        final String found =
                Arrays.stream(supportedPostFixes)
                        .filter(className::endsWith)
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new UnsupportedOperationException(
                                                "Non-standard class name: either override name() or use standard naming"));

        final String name =
                className
                        .substring(0, className.length() - found.length())
                        .replaceAll("([A-Z])", "-$1")
                        .toLowerCase();

        return name.indexOf("-") == 0 ? name.substring(1) : name;
    }
}
