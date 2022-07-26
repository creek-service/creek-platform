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
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;

import org.junit.jupiter.api.Test;

class AggregateDescriptorTest {

    @Test
    void shouldReturnStandardAggregateName() {
        assertThat(new StandardAggregateDescriptor().name(), is("standard"));
        assertThat(new SupportedDescriptor().name(), is("supported"));
    }

    @Test
    void shouldThrowOnNonStandardAggregateClassName() {
        // Given:
        final AggregateDescriptor descriptor = new NonStandard();

        // When:
        final Exception e = assertThrows(UnsupportedOperationException.class, descriptor::name);

        // Then:
        assertThat(
                e.getMessage(),
                is("Non-standard class name: either override name() or use standard naming"));
    }

    private static final class StandardAggregateDescriptor implements AggregateDescriptor {}

    private static final class SupportedDescriptor implements AggregateDescriptor {}

    private static final class NonStandard implements AggregateDescriptor {}
}
