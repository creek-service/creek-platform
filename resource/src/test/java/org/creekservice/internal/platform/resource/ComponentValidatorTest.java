/*
 * Copyright 2022-2025 Creek Contributors (https://github.com/creek-service)
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

package org.creekservice.internal.platform.resource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesRegex;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mockito.quality.Strictness.LENIENT;

import java.net.URI;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.creekservice.api.platform.metadata.AggregateDescriptor;
import org.creekservice.api.platform.metadata.ComponentDescriptor;
import org.creekservice.api.platform.metadata.ComponentInput;
import org.creekservice.api.platform.metadata.ComponentInternal;
import org.creekservice.api.platform.metadata.CreatableResource;
import org.creekservice.api.platform.metadata.OwnedResource;
import org.creekservice.api.platform.metadata.ResourceDescriptor;
import org.creekservice.api.platform.metadata.ServiceDescriptor;
import org.creekservice.api.platform.metadata.SharedResource;
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

    @Mock(name = "bob-text")
    private ServiceDescriptor service;

    @Mock(name = "jane-text")
    private AggregateDescriptor aggregate;

    private final ComponentValidator validator = new ComponentValidator();

    @BeforeEach
    void setUp() {
        when(service.name()).thenReturn("bob");
        when(aggregate.name()).thenReturn("jane");

        when(service.dockerImage()).thenReturn("image");
    }

    @Test
    void shouldThrowOnNullComponentName() {
        // Given:
        when(service.name()).thenReturn(null);

        // When:
        final Exception e = assertThrows(RuntimeException.class, () -> validator.validate(service));

        // Then:
        assertThat(
                e.getMessage(),
                containsString("name can not be null or blank, component: bob-text"));
        assertThat(e.getMessage(), matchesRegex(CODE_LOCATION));
    }

    @Test
    void shouldThrowOnBlankComponentName() {
        // Given:
        when(service.name()).thenReturn(" \t");

        // When:
        final Exception e = assertThrows(RuntimeException.class, () -> validator.validate(service));

        // Then:
        assertThat(
                e.getMessage(),
                containsString("name can not be null or blank, component: bob-text"));
        assertThat(e.getMessage(), matchesRegex(CODE_LOCATION));
    }

    @Test
    void shouldThrowOnSpecialComponentName() {
        // Given:
        when(service.name()).thenReturn("bob\nbob");

        // When:
        final Exception e = assertThrows(RuntimeException.class, () -> validator.validate(service));

        // Then:
        assertThat(
                e.getMessage(),
                containsString("name can not contain control characters, component: bob\nbob"));
        assertThat(e.getMessage(), matchesRegex(CODE_LOCATION));
    }

    @Test
    void shouldThrowIfBothServiceAndAggregateDescriptor() {
        // Given:
        final ComponentDescriptor descriptor = new PolyDescriptor();

        // When:
        final Exception e =
                assertThrows(RuntimeException.class, () -> validator.validate(descriptor));

        // Then:
        assertThat(
                e.getMessage(),
                containsString(
                        "descriptor is both aggregate and service descriptor, component: bad"));
    }

    @Test
    void shouldThrowIfNeitherServiceOrAggregateDescriptor() {
        // Given:
        final ComponentDescriptor descriptor = mock(ComponentDescriptor.class);
        when(descriptor.name()).thenReturn("bad");

        // When:
        final Exception e =
                assertThrows(RuntimeException.class, () -> validator.validate(descriptor));

        // Then:
        assertThat(
                e.getMessage(),
                containsString(
                        "descriptor is neither aggregate and service descriptor, component: bad"));
    }

    @Test
    void shouldThrowIfResourcesOverride() {
        // Given:
        service = new OverridingServiceDescriptor();

        // When:
        final Exception e = assertThrows(RuntimeException.class, () -> validator.validate(service));

        // Then:
        assertThat(
                e.getMessage(),
                containsString("should not override resources() method, component: bad"));
    }

    @Test
    void shouldThrowOnNullResource() {
        // Given:
        when(service.resources()).thenAnswer(inv -> Stream.of((ResourceDescriptor) null));

        // When:
        final Exception e = assertThrows(RuntimeException.class, () -> validator.validate(service));

        // Then:
        assertThat(e.getMessage(), containsString("contains null resource or resource stream,"));
        assertThat(e.getMessage(), containsString(", component: bob"));
        assertThat(e.getMessage(), matchesRegex(CODE_LOCATION));
    }

    @Test
    void shouldThrowOnResourceWithNullId() {
        // Given:
        final ComponentInput resource = mock(ComponentInput.class, withSettings().name("unowned"));
        when(resource.id()).thenReturn(null);
        when(service.resources()).thenAnswer(inv -> Stream.of(resource));

        // When:
        final Exception e = assertThrows(RuntimeException.class, () -> validator.validate(service));

        // Then:
        assertThat(
                e.getMessage(), containsString("null resource id, resource_type: ComponentInput$"));
        assertThat(e.getMessage(), not(containsString(", resource:")));
        assertThat(e.getMessage(), containsString("component: bob"));
        assertThat(e.getMessage(), matchesRegex(CODE_LOCATION));
    }

    @Test
    void shouldThrowOnIssuesWithNestedResources() {
        // Given:
        final ComponentInput bad = mock(ComponentInput.class, withSettings().name("bad"));
        when(bad.id()).thenReturn(null);

        final ComponentInput indirect = mock(ComponentInput.class, withSettings().name("indirect"));
        when(indirect.id()).thenReturn(URI.create("res:indirect"));
        when(indirect.resources()).thenAnswer(inv -> Stream.of(bad));

        final ComponentInput direct = mock(ComponentInput.class, withSettings().name("direct"));
        when(direct.id()).thenReturn(URI.create("res:direct"));
        when(direct.resources()).thenAnswer(inv -> Stream.of(indirect));

        when(service.resources()).thenAnswer(inv -> Stream.of(direct));

        // When:
        final Exception e = assertThrows(RuntimeException.class, () -> validator.validate(service));

        // Then:
        assertThat(
                e.getMessage(), containsString("null resource id, resource_type: ComponentInput$"));
        assertThat(e.getMessage(), containsString(", component: bob "));
        assertThat(e.getMessage(), matchesRegex(CODE_LOCATION));
    }

    @Test
    void shouldThrowOnIssuesWithResourceReturningNullResourceStream() {
        // Given:
        final ComponentInput bad = mock(ComponentInput.class, withSettings().name("bad"));
        when(bad.id()).thenReturn(URI.create("not-null"));
        when(bad.resources()).thenReturn(null);

        when(service.resources()).thenAnswer(inv -> Stream.of(bad));

        // When:
        final Exception e = assertThrows(RuntimeException.class, () -> validator.validate(service));

        // Then:
        assertThat(e.getMessage(), containsString("contains null resource or resource stream,"));
        assertThat(e.getMessage(), containsString(", component: bob"));
        assertThat(e.getMessage(), matchesRegex(CODE_LOCATION));
    }

    @Test
    void shouldThrowIfAggregateExposesInternals() {
        // Given:
        when(aggregate.internals())
                .thenReturn(
                        List.of(mock(ComponentInternal.class, withSettings().name("internal"))));

        // When:
        final Exception e =
                assertThrows(RuntimeException.class, () -> validator.validate(aggregate));

        // Then:
        assertThat(
                e.getMessage(),
                containsString(
                        "Aggregate should not expose internal resources. internals: [internal]"));
        assertThat(e.getMessage(), containsString(", component: jane"));
        assertThat(e.getMessage(), matchesRegex(CODE_LOCATION));
    }

    @Test
    void shouldThrowIfAggregateExposesNonOwnedResources() {
        // Given:
        final ComponentInput unowned = mock(ComponentInput.class, withSettings().name("unowned"));
        when(unowned.id()).thenReturn(URI.create("not-null"));

        when(aggregate.resources()).thenAnswer(inv -> Stream.of(unowned));

        // When:
        final Exception e =
                assertThrows(RuntimeException.class, () -> validator.validate(aggregate));

        // Then:
        assertThat(e.getMessage(), containsString("Aggregate should only expose OwnedResource."));
        assertThat(e.getMessage(), containsString(". not_owned: [unowned]"));
        assertThat(e.getMessage(), containsString(", component: jane"));
        assertThat(e.getMessage(), matchesRegex(CODE_LOCATION));
    }

    @Test
    void shouldThrowIfAggregateExposesNestedNonOwnedResources() {
        // Given:
        final ComponentInput unowned = mock(ComponentInput.class, withSettings().name("unowned"));
        when(unowned.id()).thenReturn(URI.create("res:unowned"));

        final ComponentInput indirect =
                mock(
                        ComponentInput.class,
                        withSettings().extraInterfaces(OwnedResource.class).name("indirect"));
        when(indirect.id()).thenReturn(URI.create("res:indirect"));
        when(indirect.resources()).thenAnswer(inv -> Stream.of(unowned));

        final ComponentInput direct =
                mock(
                        ComponentInput.class,
                        withSettings().extraInterfaces(OwnedResource.class).name("direct"));
        when(direct.id()).thenReturn(URI.create("res:direct"));
        when(direct.resources()).thenAnswer(inv -> Stream.of(indirect));

        when(aggregate.resources()).thenAnswer(inv -> Stream.of(direct));

        // When:
        final Exception e =
                assertThrows(RuntimeException.class, () -> validator.validate(aggregate));

        // Then:
        assertThat(e.getMessage(), containsString("Aggregate should only expose OwnedResource."));
        assertThat(e.getMessage(), containsString(". not_owned: [unowned]"));
        assertThat(e.getMessage(), containsString(", component: jane"));
        assertThat(e.getMessage(), matchesRegex(CODE_LOCATION));
    }

    @Test
    void shouldThrowIfResourceImplementsMultipleInitializationInterfaces() {
        // Given:
        final ComponentInput bad =
                mock(
                        ComponentInput.class,
                        withSettings()
                                .extraInterfaces(SharedResource.class, OwnedResource.class)
                                .name("bad"));
        when(bad.id()).thenReturn(URI.create("bad:resource"));
        when(service.resources()).thenAnswer(inv -> Stream.of(bad));

        // When:
        final Exception e = assertThrows(RuntimeException.class, () -> validator.validate(service));

        // Then:
        assertThat(
                e.getMessage(),
                containsString(
                        "resource can implement at-most one resource initialization interface"
                                + ", but was: [OwnedResource, SharedResource]"));
        assertThat(e.getMessage(), containsString(", resource: bad:resource"));
        assertThat(e.getMessage(), containsString(", component: bob"));
        assertThat(e.getMessage(), matchesRegex(CODE_LOCATION));
    }

    @Test
    void shouldThrowIfResourceImplementsCreatableButNotOwnedOrShared() {
        // Given:
        final ComponentInput bad =
                mock(
                        ComponentInput.class,
                        withSettings().extraInterfaces(CreatableResource.class).name("bad"));
        when(bad.id()).thenReturn(URI.create("bad:resource"));
        when(service.resources()).thenAnswer(inv -> Stream.of(bad));

        // When:
        final Exception e = assertThrows(RuntimeException.class, () -> validator.validate(service));

        // Then:
        assertThat(
                e.getMessage(),
                containsString(
                        "creatable resource must implement either the SharedResource or the"
                                + " OwnedResource interface,"));
        assertThat(e.getMessage(), containsString(", resource: bad:resource"));
        assertThat(e.getMessage(), containsString(", component: bob"));
        assertThat(e.getMessage(), matchesRegex(CODE_LOCATION));
    }

    @Test
    void shouldThrowOnNullDockerImage() {
        // Given:
        when(service.dockerImage()).thenReturn(null);

        // When:
        final Exception e = assertThrows(RuntimeException.class, () -> validator.validate(service));

        // Then:
        assertThat(
                e.getMessage(),
                containsString("dockerImage can not be null or blank, component: bob"));
        assertThat(e.getMessage(), matchesRegex(CODE_LOCATION));
    }

    @Test
    void shouldThrowOnBlankDockerImage() {
        // Given:
        when(service.dockerImage()).thenReturn("\t");

        // When:
        final Exception e = assertThrows(RuntimeException.class, () -> validator.validate(service));

        // Then:
        assertThat(
                e.getMessage(),
                containsString("dockerImage can not be null or blank, component: bob"));
        assertThat(e.getMessage(), matchesRegex(CODE_LOCATION));
    }

    @Test
    void shouldThrowOnNullTestEnvironment() {
        // Given:
        when(service.testEnvironment()).thenReturn(null);

        // When:
        final Exception e = assertThrows(RuntimeException.class, () -> validator.validate(service));

        // Then:
        assertThat(
                e.getMessage(), containsString("testEnvironment can not be null, component: bob"));
        assertThat(e.getMessage(), matchesRegex(CODE_LOCATION));
    }

    @Test
    void shouldNotThrowIfEverythingIsOK() {
        validator.validate(aggregate);
        validator.validate(service);
    }

    @Test
    void shouldWorkWithCircularResourceReferences() {
        // Given:
        final ComponentInput indirect = mock(ComponentInput.class, withSettings().name("indirect"));
        when(indirect.id()).thenReturn(URI.create("res:indirect"));

        final ComponentInput direct = mock(ComponentInput.class, withSettings().name("direct"));
        when(direct.id()).thenReturn(URI.create("res:direct"));

        when(indirect.resources()).thenAnswer(inv -> Stream.of(direct));
        when(direct.resources()).thenAnswer(inv -> Stream.of(indirect));

        when(service.resources()).thenAnswer(inv -> Stream.of(direct));

        // When:
        validator.validate(service);

        // Then: did not throw
    }

    private static final class OverridingServiceDescriptor implements ServiceDescriptor {
        @Override
        public String name() {
            return "bad";
        }

        @Override
        public String dockerImage() {
            return "image";
        }

        @Override
        public Stream<? extends ResourceDescriptor> resources() {
            return ServiceDescriptor.super.resources();
        }
    }

    private static final class PolyDescriptor implements ServiceDescriptor, AggregateDescriptor {
        @Override
        public String name() {
            return "bad";
        }

        @Override
        public String dockerImage() {
            return "image";
        }
    }
}
