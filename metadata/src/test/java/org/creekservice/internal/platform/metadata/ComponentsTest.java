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

package org.creekservice.internal.platform.metadata;

import static org.creekservice.internal.platform.metadata.Components.defaultNaming;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.creekservice.api.platform.metadata.ComponentDescriptor;
import org.junit.jupiter.api.Test;

class ComponentsTest {

    @Test
    void shouldCreateDefaultName() {
        assertThat(defaultNaming(new TestServiceDescriptor(), "Descriptor"), is("test-service"));
    }

    @Test
    void shouldHandleFirstCharNoCapital() {
        assertThat(
                defaultNaming(new lowerCaseServiceDescriptor(), "ServiceDescriptor"),
                is("lower-case"));
    }

    @Test
    void shouldReportFirstMatchedPostfix() {
        assertThat(
                defaultNaming(new TestServiceDescriptor(), "ServiceDescriptor", "Descriptor"),
                is("test"));
    }

    @Test
    void shouldSupportNamesWithoutAnyMatchingPostfix() {}

    private static final class TestServiceDescriptor implements ComponentDescriptor {
        @Override
        public String name() {
            return null;
        }
    }

    @SuppressWarnings("checkstyle:TypeName")
    private static final class lowerCaseServiceDescriptor implements ComponentDescriptor {
        @Override
        public String name() {
            return null;
        }
    }
}
