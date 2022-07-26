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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ComponentDescriptorTest {

    private static final List<ComponentInput> INPUTS = List.of(mock(ComponentInput.class));
    private static final List<ComponentInternal> INTERNALS = List.of(mock(ComponentInternal.class));
    private static final List<ComponentOutput> OUTPUTS = List.of(mock(ComponentOutput.class));

    private final ComponentDescriptor descriptor = new TestDescriptor();

    @Test
    void shouldDefaultToEmptyResources() {
        // Given:
        final ComponentDescriptor emptyDescriptor = () -> "bob";

        // Then:
        assertThat(emptyDescriptor.inputs(), is(empty()));
        assertThat(emptyDescriptor.internals(), is(empty()));
        assertThat(emptyDescriptor.outputs(), is(empty()));
        assertThat(emptyDescriptor.resources().collect(Collectors.toList()), is(empty()));
    }

    @Test
    void shouldReturnResources() {
        assertThat(descriptor.inputs(), is(INPUTS));
        assertThat(descriptor.internals(), is(INTERNALS));
        assertThat(descriptor.outputs(), is(OUTPUTS));
    }

    @Test
    void shouldStreamResources() {
        assertThat(
                descriptor.resources().collect(Collectors.toList()),
                contains(INPUTS.get(0), INTERNALS.get(0), OUTPUTS.get(0)));
    }

    private static final class TestDescriptor implements ComponentDescriptor {

        @Override
        public String name() {
            return "bob";
        }

        @Override
        public Collection<ComponentInput> inputs() {
            return INPUTS;
        }

        @Override
        public Collection<ComponentInternal> internals() {
            return INTERNALS;
        }

        @Override
        public Collection<ComponentOutput> outputs() {
            return OUTPUTS;
        }
    }
}
