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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.net.URI;
import java.util.List;
import java.util.stream.Stream;
import org.creekservice.api.platform.metadata.ComponentDescriptor;
import org.creekservice.api.platform.metadata.OwnedResource;
import org.creekservice.api.platform.metadata.ResourceDescriptor;
import org.creekservice.api.platform.metadata.ResourceHandler;
import org.creekservice.api.platform.metadata.SharedResource;
import org.creekservice.api.platform.metadata.UnownedResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ResourceInitializerTest {

    private static final URI A1_ID = URI.create("a://1");

    @Mock private ComponentValidator validator;
    @Mock private ComponentDescriptor component0;
    @Mock private ComponentDescriptor component1;
    @Mock private ResourceInitializer.ResourceHandlers handlers;
    @Mock private ResourceHandler<ResourceDescriptor> handlerA;
    @Mock private ResourceHandler<ResourceDescriptor> handlerB;

    @Mock(extraInterfaces = SharedResource.class)
    private ResourceA sharedResource1;

    @Mock(extraInterfaces = UnownedResource.class)
    private ResourceA unownedResource1;

    @Mock(extraInterfaces = OwnedResource.class)
    private ResourceA ownedResource1;

    @Mock private ResourceA unmanagedResource1;

    private ResourceInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new ResourceInitializer(handlers, validator);

        when(handlers.get(any())).thenReturn(handlerA);

        when(sharedResource1.id()).thenReturn(A1_ID);
        when(ownedResource1.id()).thenReturn(A1_ID);
        when(unownedResource1.id()).thenReturn(A1_ID);
        when(unmanagedResource1.id()).thenReturn(A1_ID);
    }

    @Test
    void shouldValidateEachComponentOnInit() {
        // When:
        initializer.init(Stream.of(component0, component1));

        // Then:
        verify(validator).validate(component0);
        verify(validator).validate(component1);
    }

    @Test
    void shouldValidateEachComponentOnService() {
        // When:
        initializer.service(Stream.of(component0, component1));

        // Then:
        verify(validator).validate(component0);
        verify(validator).validate(component1);
    }

    @Test
    void shouldValidateEachComponentOnTest() {
        // When:
        initializer.test(Stream.of(component0), Stream.of(component1));

        // Then:
        verify(validator).validate(component0);
        verify(validator).validate(component1);
    }

    @Test
    void shouldThrowIfComponentValidationFails() {
        // Given:
        final RuntimeException expected = new RuntimeException("boom");
        doThrow(expected).when(validator).validate(any());

        // When:
        final Exception e =
                assertThrows(
                        RuntimeException.class,
                        () -> initializer.service(Stream.of(component0, component1)));

        // Then:
        assertThat(e, is(sameInstance(expected)));
    }

    @Test
    void shouldThrowIfResourceGroupContainsSharedAndNonShared() {
        // Given:
        when(component0.resources()).thenReturn(Stream.of(sharedResource1));
        when(component1.resources()).thenReturn(Stream.of(unownedResource1));

        // When:
        final Exception e =
                assertThrows(
                        RuntimeException.class,
                        () -> initializer.init(Stream.of(component0, component1)));

        // Then:
        assertThat(
                e.getMessage(),
                startsWith(
                        "Resource descriptors for resource are tagged with incompatible resource initialization "
                                + "marker interfaces. First descriptor is marked as a shared resource, "
                                + "but at least one subsequent descriptor was not shared. "
                                + "resource: a://1, descriptors: "));
        assertThat(e.getMessage(), containsString("sharedResource1"));
        assertThat(e.getMessage(), containsString("unownedResource1"));
    }

    @Test
    void shouldThrowIfResourceGroupContainsUnmanagedAndNonManaged() {
        // Given:
        when(component0.resources()).thenReturn(Stream.of(unmanagedResource1));
        when(component1.resources()).thenReturn(Stream.of(sharedResource1));

        // When:
        final Exception e =
                assertThrows(
                        RuntimeException.class,
                        () -> initializer.init(Stream.of(component0, component1)));

        // Then:
        assertThat(
                e.getMessage(),
                startsWith(
                        "Resource descriptors for resource are tagged with incompatible resource initialization "
                                + "marker interfaces. First descriptor is marked as a unmanaged resource, "
                                + "but at least one subsequent descriptor was not unmanaged. "
                                + "resource: a://1, descriptors: "));
        assertThat(e.getMessage(), containsString("unmanagedResource1"));
        assertThat(e.getMessage(), containsString("sharedResource1"));
    }

    @Test
    void shouldThrowIfResourceGroupContainsOwnedAndOther() {
        // Given:
        when(component0.resources()).thenReturn(Stream.of(ownedResource1));
        when(component1.resources()).thenReturn(Stream.of(sharedResource1));

        // When:
        final Exception e =
                assertThrows(
                        RuntimeException.class,
                        () -> initializer.init(Stream.of(component0, component1)));

        // Then:
        assertThat(
                e.getMessage(),
                startsWith(
                        "Resource descriptors for resource are tagged with incompatible resource initialization "
                                + "marker interfaces. First descriptor is marked as a owned or unowned resource, "
                                + "but at least one subsequent descriptor was not owned or unowned. "
                                + "resource: a://1, descriptors: "));
        assertThat(e.getMessage(), containsString("ownedResource1"));
        assertThat(e.getMessage(), containsString("sharedResource1"));
    }

    @Test
    void shouldThrowIfResourceGroupContainsUnownedAndOther() {
        // Given:
        when(component0.resources()).thenReturn(Stream.of(unownedResource1));
        when(component1.resources()).thenReturn(Stream.of(sharedResource1));

        // When:
        final Exception e =
                assertThrows(
                        RuntimeException.class,
                        () -> initializer.init(Stream.of(component0, component1)));

        // Then:
        assertThat(
                e.getMessage(),
                startsWith(
                        "Resource descriptors for resource are tagged with incompatible resource initialization "
                                + "marker interfaces. First descriptor is marked as a owned or unowned resource, "
                                + "but at least one subsequent descriptor was not owned or unowned. "
                                + "resource: a://1, descriptors: "));
        assertThat(e.getMessage(), containsString("unownedResource1"));
        assertThat(e.getMessage(), containsString("sharedResource1"));
    }

    @Test
    void shouldNotInitializeAnyResourceOnInitIfNoSharedResources() {
        // Given:
        when(component0.resources()).thenReturn(Stream.of(ownedResource1, unmanagedResource1));
        when(component1.resources()).thenReturn(Stream.of(unownedResource1));

        // When:
        initializer.init(Stream.of(component0, component1));

        // Then:
        verify(handlers, never()).get(any());
    }

    @Test
    void shouldNotInitializeAnyResourcesOnServiceIfNoOwnedResources() {
        // Given:
        when(component0.resources()).thenReturn(Stream.of(sharedResource1, unmanagedResource1));
        when(component1.resources()).thenReturn(Stream.of(unownedResource1));

        // When:
        initializer.service(Stream.of(component0, component1));

        // Then:
        verify(handlers, never()).get(any());
    }

    @Test
    void shouldNotInitializeAnyResourcesOnTestIfNoUnownedResources() {
        // Given:
        when(component0.resources())
                .thenReturn(
                        Stream.of(
                                sharedResource1,
                                unmanagedResource1,
                                ownedResource1,
                                unownedResource1));

        // When:
        initializer.test(Stream.of(component0), Stream.of(component1));

        // Then:
        verify(handlers, never()).get(any());
    }

    @Test
    void shouldNotInitializeUnmanagedGroups() {
        // Given:
        final ResourceDescriptor unmanagedResource1b = mock(ResourceDescriptor.class);
        when(unmanagedResource1b.id()).thenReturn(A1_ID);

        when(component0.resources()).thenReturn(Stream.of(unmanagedResource1));
        when(component1.resources()).thenReturn(Stream.of(unmanagedResource1b));

        // When:
        initializer.init(Stream.of(component0, component1));

        // Then:
        verify(handlers, never()).get(any());
    }

    @Test
    void shouldValidateSharedGroup() {
        // Given:
        final ResourceA sharedResource1b =
                mock(ResourceA.class, withSettings().extraInterfaces(SharedResource.class));
        when(sharedResource1b.id()).thenReturn(A1_ID);

        when(component0.resources()).thenReturn(Stream.of(sharedResource1));
        when(component1.resources()).thenReturn(Stream.of(sharedResource1b));

        // When:
        initializer.init(Stream.of(component0, component1));

        // Then:
        verify(handlerA).validate(List.of(sharedResource1, sharedResource1b));
    }

    @Test
    void shouldValidateOwnedAndUnownedGroup() {
        // Given:
        when(component0.resources()).thenReturn(Stream.of(unownedResource1));
        when(component1.resources()).thenReturn(Stream.of(ownedResource1));

        // When:
        initializer.test(Stream.of(component0), Stream.of(component1));

        // Then:
        verify(handlerA).validate(List.of(unownedResource1, ownedResource1));
    }

    @Test
    void shouldThrowIfResourceGroupValidationFails() {
        // Given:
        final RuntimeException expected = new RuntimeException("boom");
        doThrow(expected).when(handlerA).validate(any());
        when(component0.resources()).thenReturn(Stream.of(sharedResource1));

        // When:
        final Exception e =
                assertThrows(RuntimeException.class, () -> initializer.init(Stream.of(component0)));

        // Then:
        assertThat(e, is(sameInstance(expected)));
    }

    @Test
    void shouldThrowOnUncreatableResource() {
        // Given:
        when(component0.resources()).thenReturn(Stream.of(unownedResource1));
        when(component1.resources()).thenReturn(Stream.of()); // Missing owned resource descriptor

        // When:
        final Exception e =
                assertThrows(
                        RuntimeException.class,
                        () -> initializer.test(Stream.of(component0), Stream.of(component1)));

        // Then:
        assertThat(
                e.getMessage(),
                startsWith(
                        "No component provided a creatable descriptor for resource id: a://1, descriptors: "));
        assertThat(e.getMessage(), containsString("unownedResource1"));
    }

    @Test
    void shouldEnsureSharedResource() {
        // Given:
        final ResourceA sharedResource2 =
                mock(ResourceA.class, withSettings().extraInterfaces(SharedResource.class));
        when(sharedResource2.id()).thenReturn(URI.create("a://2"));

        when(component0.resources()).thenReturn(Stream.of(sharedResource1));
        when(component1.resources()).thenReturn(Stream.of(sharedResource2, sharedResource1));

        // When:
        initializer.init(Stream.of(component0, component1));

        // Then:
        verify(handlerA).ensure(List.of(sharedResource1, sharedResource2));
    }

    @Test
    void shouldEnsureOwnedResource() {
        // Given:
        final ResourceA ownedResource2 =
                mock(ResourceA.class, withSettings().extraInterfaces(OwnedResource.class));
        when(ownedResource2.id()).thenReturn(URI.create("a://2"));

        when(component0.resources()).thenReturn(Stream.of(ownedResource1));
        when(component1.resources()).thenReturn(Stream.of(ownedResource2, unownedResource1));

        // When:
        initializer.service(Stream.of(component0, component1));

        // Then:
        verify(handlerA).ensure(List.of(ownedResource1, ownedResource2));
    }

    @Test
    void shouldEnsureUnownedResource() {
        // Given:
        final ResourceA ownedResource2 =
                mock(ResourceA.class, withSettings().extraInterfaces(OwnedResource.class));
        when(ownedResource2.id()).thenReturn(URI.create("a://2"));

        final ResourceA unownedResource2 =
                mock(ResourceA.class, withSettings().extraInterfaces(UnownedResource.class));
        when(unownedResource2.id()).thenReturn(URI.create("a://2"));

        when(component0.resources()).thenReturn(Stream.of(unownedResource2));
        when(component1.resources()).thenReturn(Stream.of(ownedResource2, unownedResource1));

        // When:
        initializer.test(Stream.of(component0), Stream.of(component1));

        // Then:
        verify(handlerA).ensure(List.of(ownedResource2));
    }

    @Test
    void shouldThrowIfEnsureThrows() {
        // Given:
        final RuntimeException expected = new RuntimeException("boom");
        doThrow(expected).when(handlerA).ensure(any());
        when(component0.resources()).thenReturn(Stream.of(ownedResource1));

        // When:
        final Exception e =
                assertThrows(
                        RuntimeException.class, () -> initializer.service(Stream.of(component0)));

        // Then:
        assertThat(e, is(sameInstance(expected)));
    }

    @Test
    void shouldEnsureGroupingByHandler() {
        // Given
        when(handlers.get(any()))
                .thenAnswer(
                        inv ->
                                ResourceA.class.isAssignableFrom(inv.getArgument(0))
                                        ? handlerA
                                        : handlerB);

        final ResourceB ownedResourceB =
                mock(ResourceB.class, withSettings().extraInterfaces(OwnedResource.class));
        when(ownedResourceB.id()).thenReturn(URI.create("b://1"));

        when(component0.resources()).thenReturn(Stream.of(ownedResource1, ownedResourceB));

        // When:
        initializer.service(Stream.of(component0));

        // Then:
        verify(handlerA).ensure(List.of(ownedResource1));
        verify(handlerB).ensure(List.of(ownedResourceB));
    }

    private interface ResourceA extends ResourceDescriptor {}

    private interface ResourceB extends ResourceDescriptor {}
}
