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

package org.creekservice.api.platform.resource;

import static org.creekservice.api.platform.resource.ComponentValidator.componentValidator;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesRegex;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.quality.Strictness.LENIENT;

import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.creekservice.api.platform.metadata.ComponentDescriptor;
import org.creekservice.api.platform.metadata.ResourceDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = LENIENT)
class ComponentValidatorTest {

    private static final Pattern CODE_LOCATION =
            Pattern.compile(".*\\(file:/.*creek-platform-metadata-.*\\.jar\\).*", Pattern.DOTALL);

    @Mock(name = "jane")
    private ComponentDescriptor component;

    private final ComponentValidator validator = componentValidator();

    @BeforeEach
    void setUp() {
        when(component.name()).thenReturn("bob");
    }

    @Test
    void shouldThrowOnNullComponentName() {
        // Given:
        when(component.name()).thenReturn(null);

        // When:
        final Exception e =
                assertThrows(RuntimeException.class, () -> validator.validate(component));

        // Then:
        assertThat(e.getMessage(), containsString("name can not be null or blank, component"));
        assertThat(e.getMessage(), containsString("jane"));
        assertThat(e.getMessage(), matchesRegex(CODE_LOCATION));
    }

    @Test
    void shouldThrowOnBlankComponentName() {
        // Given:
        when(component.name()).thenReturn(" \t");

        // When:
        final Exception e =
                assertThrows(RuntimeException.class, () -> validator.validate(component));

        // Then:
        assertThat(e.getMessage(), containsString("name can not be null or blank, component"));
        assertThat(e.getMessage(), containsString("jane"));
        assertThat(e.getMessage(), matchesRegex(CODE_LOCATION));
    }

    @Test
    void shouldThrowOnSpecialComponentName() {
        // Given:
        when(component.name()).thenReturn("bob\nbob");

        // When:
        final Exception e =
                assertThrows(RuntimeException.class, () -> validator.validate(component));

        // Then:
        assertThat(
                e.getMessage(),
                containsString("name can not contain control characters, component"));
        assertThat(e.getMessage(), containsString("bob\nbob"));
        assertThat(e.getMessage(), matchesRegex(CODE_LOCATION));
    }

    @Test
    void shouldThrowIfResourcesOverride() {
        // Given:
        component = new BadDescriptor();

        // When:
        final Exception e =
                assertThrows(RuntimeException.class, () -> validator.validate(component));

        // Then:
        assertThat(
                e.getMessage(),
                containsString("should not override resources() method, component"));
        assertThat(e.getMessage(), containsString(component.name()));
    }

    @Test
    void shouldThrowOnNullResource() {
        // Given:
        when(component.resources()).thenReturn(Stream.of((ResourceDescriptor) null));

        // When:
        final Exception e =
                assertThrows(RuntimeException.class, () -> validator.validate(component));

        // Then:
        assertThat(e.getMessage(), containsString("contains null resource, component"));
        assertThat(e.getMessage(), containsString(component.name()));
        assertThat(e.getMessage(), matchesRegex(CODE_LOCATION));
    }

    @Test
    void shouldNotThrowIfEverythingIsOK() {
        validator.validate(component);
    }

    private static final class BadDescriptor implements ComponentDescriptor {
        @Override
        public String name() {
            return "bad";
        }

        @Override
        public Stream<ResourceDescriptor> resources() {
            return ComponentDescriptor.super.resources();
        }
    }
}
