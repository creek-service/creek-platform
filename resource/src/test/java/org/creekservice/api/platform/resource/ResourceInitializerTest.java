/*
 * Copyright 2022-2023 Creek Contributors (https://github.com/creek-service)
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
import static org.mockito.Mockito.inOrder;
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
import org.creekservice.api.platform.metadata.SharedResource;
import org.creekservice.api.platform.metadata.UnownedResource;
import org.creekservice.internal.platform.resource.ComponentValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockSettings;
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
    @Mock private ResourceInitializer.Callbacks callbacks;

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
        initializer = new ResourceInitializer(callbacks, validator);

        when(sharedResource1.id()).thenReturn(A1_ID);
        when(ownedResource1.id()).thenReturn(A1_ID);
        when(unownedResource1.id()).thenReturn(A1_ID);
        when(unmanagedResource1.id()).thenReturn(A1_ID);
    }

    @Test
    void shouldValidateEachComponentOnInit() {
        // When:
        initializer.init(List.of(component0, component1));

        // Then:
        verify(validator).validate(component0);
        verify(validator).validate(component1);
    }

    @Test
    void shouldValidateEachComponentOnService() {
        // When:
        initializer.service(List.of(component0, component1));

        // Then:
        verify(validator).validate(component0);
        verify(validator).validate(component1);
    }

    @Test
    void shouldValidateEachComponentOnTest() {
        // When:
        initializer.test(List.of(component0), List.of(component1));

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
                        () -> initializer.service(List.of(component0, component1)));

        // Then:
        assertThat(e, is(sameInstance(expected)));
    }

    @Test
    void shouldThrowIfResourceGroupContainsSharedAndNonShared() {
        // Given:
        when(component0.resources()).thenAnswer(inv -> Stream.of(sharedResource1));
        when(component1.resources()).thenAnswer(inv -> Stream.of(unownedResource1));

        // When:
        final Exception e =
                assertThrows(
                        RuntimeException.class,
                        () -> initializer.init(List.of(component0, component1)));

        // Then:
        assertThat(
                e.getMessage(),
                startsWith(
                        "Resource descriptors for resource are tagged with incompatible resource"
                                + " initialization interfaces. First descriptor is marked as a"
                                + " shared resource, but at least one subsequent descriptor was not"
                                + " shared. resource: a://1, descriptors: "));
        assertThat(e.getMessage(), containsString("sharedResource1"));
        assertThat(e.getMessage(), containsString("unownedResource1"));
    }

    @Test
    void shouldThrowIfResourceGroupContainsUnmanagedAndNonManaged() {
        // Given:
        when(component0.resources()).thenAnswer(inv -> Stream.of(unmanagedResource1));
        when(component1.resources()).thenAnswer(inv -> Stream.of(sharedResource1));

        // When:
        final Exception e =
                assertThrows(
                        RuntimeException.class,
                        () -> initializer.init(List.of(component0, component1)));

        // Then:
        assertThat(
                e.getMessage(),
                startsWith(
                        "Resource descriptors for resource are tagged with incompatible resource"
                            + " initialization interfaces. First descriptor is marked as a"
                            + " unmanaged resource, but at least one subsequent descriptor was not"
                            + " unmanaged. resource: a://1, descriptors: "));
        assertThat(e.getMessage(), containsString("unmanagedResource1"));
        assertThat(e.getMessage(), containsString("sharedResource1"));
    }

    @Test
    void shouldThrowIfResourceGroupContainsOwnedAndOther() {
        // Given:
        when(component0.resources()).thenAnswer(inv -> Stream.of(ownedResource1));
        when(component1.resources()).thenAnswer(inv -> Stream.of(sharedResource1));

        // When:
        final Exception e =
                assertThrows(
                        RuntimeException.class,
                        () -> initializer.init(List.of(component0, component1)));

        // Then:
        assertThat(
                e.getMessage(),
                startsWith(
                        "Resource descriptors for resource are tagged with incompatible resource"
                            + " initialization interfaces. First descriptor is marked as a owned or"
                            + " unowned resource, but at least one subsequent descriptor was not"
                            + " owned or unowned. resource: a://1, descriptors: "));
        assertThat(e.getMessage(), containsString("ownedResource1"));
        assertThat(e.getMessage(), containsString("sharedResource1"));
    }

    @Test
    void shouldThrowIfResourceGroupContainsUnownedAndOther() {
        // Given:
        when(component0.resources()).thenAnswer(inv -> Stream.of(unownedResource1));
        when(component1.resources()).thenAnswer(inv -> Stream.of(sharedResource1));

        // When:
        final Exception e =
                assertThrows(
                        RuntimeException.class,
                        () -> initializer.init(List.of(component0, component1)));

        // Then:
        assertThat(
                e.getMessage(),
                startsWith(
                        "Resource descriptors for resource are tagged with incompatible resource"
                            + " initialization interfaces. First descriptor is marked as a owned or"
                            + " unowned resource, but at least one subsequent descriptor was not"
                            + " owned or unowned. resource: a://1, descriptors: "));
        assertThat(e.getMessage(), containsString("unownedResource1"));
        assertThat(e.getMessage(), containsString("sharedResource1"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldCallbackValidateEachResGroupOnInit() {
        // Given:
        final ResourceA sharedResource2 = resourceA(1, SharedResource.class);
        when(component0.resources()).thenAnswer(inv -> Stream.of(sharedResource1));
        when(component1.resources()).thenAnswer(inv -> Stream.of(sharedResource2));

        // When:
        initializer.init(List.of(component0, component1));

        // Then:
        verify(callbacks)
                .validate(
                        (Class<ResourceA>) sharedResource1.getClass(),
                        List.of(sharedResource1, sharedResource2));
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldCallbackValidateEachResGroupOnService() {
        // Given:
        when(unmanagedResource1.id()).thenReturn(URI.create("a://diff"));
        when(component0.resources())
                .thenAnswer(inv -> Stream.of(ownedResource1, unmanagedResource1));
        when(component1.resources()).thenAnswer(inv -> Stream.of(unownedResource1));

        // When:
        initializer.service(List.of(component0, component1));

        // Then:
        verify(callbacks)
                .validate(
                        (Class<ResourceA>) unmanagedResource1.getClass(),
                        List.of(unmanagedResource1));

        verify(callbacks)
                .validate(
                        (Class<ResourceA>) ownedResource1.getClass(),
                        List.of(ownedResource1, unownedResource1));
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldCallbackValidateEachResGroupOnTest() {
        // Given:
        when(unmanagedResource1.id()).thenReturn(URI.create("a://diff"));
        when(component0.resources())
                .thenAnswer(inv -> Stream.of(unownedResource1, unmanagedResource1));
        when(component1.resources()).thenAnswer(inv -> Stream.of(ownedResource1));

        // When:
        initializer.test(List.of(component0), List.of(component1));

        // Then:
        verify(callbacks)
                .validate(
                        (Class<ResourceA>) unmanagedResource1.getClass(),
                        List.of(unmanagedResource1));

        verify(callbacks)
                .validate(
                        (Class<ResourceA>) unownedResource1.getClass(),
                        List.of(unownedResource1, ownedResource1));
    }

    @Test
    void shouldThrowIfValidateCallbackThrows() {
        // Given:
        when(component0.resources()).thenAnswer(inv -> Stream.of(ownedResource1));
        when(component1.resources()).thenAnswer(inv -> Stream.of(unownedResource1));
        final RuntimeException expected = new RuntimeException("BIG BADA BOOM");
        doThrow(expected).when(callbacks).validate(any(), any());

        // When:
        final Exception e =
                assertThrows(
                        RuntimeException.class,
                        () -> initializer.service(List.of(component0, component1)));

        // Then:
        assertThat(e, is(expected));
    }

    @Test
    void shouldNotInitializeAnyResourceOnInitIfNoSharedResources() {
        // Given:
        when(component0.resources())
                .thenAnswer(inv -> Stream.of(ownedResource1, unmanagedResource1));
        when(component1.resources()).thenAnswer(inv -> Stream.of(unownedResource1));

        // When:
        initializer.init(List.of(component0, component1));

        // Then:
        verify(callbacks, never()).ensure(any(), any());
    }

    @Test
    void shouldNotInitializeAnyResourcesOnServiceIfNoOwnedResources() {
        // Given:
        final ResourceA shared = resourceA(2, SharedResource.class);
        final ResourceA unowned = resourceA(3, UnownedResource.class);
        when(component0.resources()).thenAnswer(inv -> Stream.of(shared, unmanagedResource1));
        when(component1.resources()).thenAnswer(inv -> Stream.of(unowned));

        // When:
        initializer.service(List.of(component0, component1));

        // Then:
        verify(callbacks, never()).ensure(any(), any());
    }

    @Test
    void shouldNotInitializeAnyResourcesOnTestIfNoUnownedResources() {
        // Given:
        final ResourceA shared = resourceA(2, SharedResource.class);
        final ResourceA unowned = resourceA(3, UnownedResource.class);
        final ResourceA owned = resourceA(4, OwnedResource.class);
        when(component0.resources()).thenAnswer(inv -> Stream.of(unmanagedResource1, shared));
        when(component1.resources())
                .thenAnswer(inv -> Stream.of(unmanagedResource1, shared, unowned, owned));

        // When:
        initializer.test(List.of(component0), List.of(component1));

        // Then:
        verify(callbacks, never()).ensure(any(), any());
    }

    @Test
    void
            shouldNotInitializeAnyResourcesOnTestIfUnownedResourcesHaveOwnedDescriptorInComponentsUnderTest() {
        // Given:
        when(component0.resources()).thenAnswer(inv -> Stream.of(unownedResource1));
        when(component1.resources()).thenAnswer(inv -> Stream.of(ownedResource1));

        // When:
        initializer.test(List.of(component0, component1), List.of());

        // Then:
        verify(callbacks, never()).ensure(any(), any());
    }

    @Test
    void shouldNotInitializeUnmanagedGroups() {
        // Given:
        final ResourceDescriptor unmanagedResource1b = mock(ResourceDescriptor.class);
        when(unmanagedResource1b.id()).thenReturn(A1_ID);

        when(component0.resources()).thenAnswer(inv -> Stream.of(unmanagedResource1));
        when(component1.resources()).thenAnswer(inv -> Stream.of(unmanagedResource1b));

        // When:
        initializer.init(List.of(component0, component1));

        // Then:
        verify(callbacks, never()).ensure(any(), any());
    }

    @Test
    void shouldThrowOnUncreatableResource() {
        // Given:
        when(component0.resources()).thenAnswer(inv -> Stream.of(unownedResource1));
        when(component1.resources())
                .thenAnswer(inv -> Stream.of()); // Missing owned resource descriptor

        // When:
        final Exception e =
                assertThrows(
                        RuntimeException.class,
                        () -> initializer.test(List.of(component0), List.of(component1)));

        // Then:
        assertThat(
                e.getMessage(),
                startsWith(
                        "No component provided a creatable descriptor for resource id: a://1,"
                                + " known_creatable_descriptors: "));
        assertThat(e.getMessage(), containsString("unownedResource1"));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void shouldEnsureSharedResource() {
        // Given:
        final ResourceA sharedResource2 = resourceA(2, SharedResource.class);
        when(component0.resources()).thenAnswer(inv -> Stream.of(sharedResource1));
        when(component1.resources()).thenAnswer(inv -> Stream.of(sharedResource2, sharedResource1));

        // When:
        initializer.init(List.of(component0, component1));

        // Then:
        verify(callbacks)
                .ensure(
                        (Class) sharedResource1.getClass(),
                        (List) List.of(sharedResource1, sharedResource2));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void shouldEnsureOwnedResource() {
        // Given:
        final ResourceA ownedResource2 = resourceA(2, OwnedResource.class);
        when(component0.resources()).thenAnswer(inv -> Stream.of(ownedResource1));
        when(component1.resources()).thenAnswer(inv -> Stream.of(ownedResource2, unownedResource1));

        // When:
        initializer.service(List.of(component0, component1));

        // Then:
        verify(callbacks)
                .ensure(
                        (Class) ownedResource1.getClass(),
                        (List) List.of(ownedResource1, ownedResource2));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void shouldEnsureUnownedResource() {
        // Given:
        final ResourceA ownedResource2 = resourceA(2, OwnedResource.class);
        final ResourceA unownedResource2 = resourceA(2, UnownedResource.class);
        when(component0.resources()).thenAnswer(inv -> Stream.of(unownedResource2));
        when(component1.resources()).thenAnswer(inv -> Stream.of(ownedResource2, unownedResource1));

        // When:
        initializer.test(List.of(component0), List.of(component1));

        // Then:
        verify(callbacks).ensure((Class) ownedResource2.getClass(), (List) List.of(ownedResource2));
    }

    @Test
    void shouldThrowIfEnsureThrows() {
        // Given:
        final RuntimeException expected = new RuntimeException("boom");
        doThrow(expected).when(callbacks).ensure(any(), any());
        when(component0.resources()).thenAnswer(inv -> Stream.of(ownedResource1));

        // When:
        final Exception e =
                assertThrows(
                        RuntimeException.class, () -> initializer.service(List.of(component0)));

        // Then:
        assertThat(e, is(sameInstance(expected)));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void shouldEnsureGroupingByHandler() {
        // Given
        final ResourceB ownedResourceB = resourceB(OwnedResource.class);
        when(component0.resources()).thenAnswer(inv -> Stream.of(ownedResource1, ownedResourceB));

        // When:
        initializer.service(List.of(component0));

        // Then:
        verify(callbacks).ensure((Class) ownedResource1.getClass(), (List) List.of(ownedResource1));
        verify(callbacks).ensure((Class) ownedResourceB.getClass(), (List) List.of(ownedResourceB));
    }

    @Test
    void shouldThrowOnUnknownResourceType() {
        // Given:
        final NullPointerException expected = new NullPointerException("unknown");
        doThrow(expected).when(callbacks).ensure(any(), any());
        when(component0.resources()).thenAnswer(inv -> Stream.of(sharedResource1));

        // When:
        final Exception e =
                assertThrows(
                        NullPointerException.class, () -> initializer.init(List.of(component0)));

        // Then:
        assertThat(e, is(sameInstance(expected)));
    }

    @Test
    void shouldThrowOnInvalidComponentUsingActualValidator() {
        // Given:
        initializer = ResourceInitializer.resourceInitializer(callbacks);

        // Then:
        assertThrows(RuntimeException.class, () -> initializer.init(List.of(component0)));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void shouldEnsureChildResourcesBeforeParents() {
        // Given
        final ResourceB child = resourceB(OwnedResource.class);
        when(ownedResource1.resources()).thenAnswer(inv -> Stream.of(child));
        when(component0.resources()).thenAnswer(inv -> Stream.of(ownedResource1));

        // When:
        initializer.service(List.of(component0));

        // Then:
        final InOrder inOrder = inOrder(callbacks);
        inOrder.verify(callbacks).ensure((Class) child.getClass(), (List) List.of(child));
        inOrder.verify(callbacks)
                .ensure((Class) ownedResource1.getClass(), (List) List.of(ownedResource1));
    }

    @Test
    void shouldHandleCircularResourceReferences() {
        // Given
        when(ownedResource1.resources()).thenAnswer(inv -> Stream.of(unownedResource1));
        when(component0.resources()).thenAnswer(inv -> Stream.of(ownedResource1, unownedResource1));

        // When:
        initializer.service(List.of(component0));

        // Then: did not throw, and:
        verify(callbacks).ensure(any(), any());
    }

    private static ResourceA resourceA(final int id, final Class<?> extraInterface) {
        return resourceA(id, withSettings().extraInterfaces(extraInterface));
    }

    private static ResourceA resourceA(final int id, final MockSettings settings) {
        final ResourceA resource = mock(ResourceA.class, settings);
        when(resource.id()).thenReturn(URI.create("a://" + id));
        return resource;
    }

    @SuppressWarnings("SameParameterValue")
    private static ResourceB resourceB(final Class<?> extraInterface) {
        final ResourceB resource =
                mock(ResourceB.class, withSettings().extraInterfaces(extraInterface));
        when(resource.id()).thenReturn(URI.create("b://1"));
        return resource;
    }

    private interface ResourceA extends ResourceDescriptor {}

    private interface ResourceB extends ResourceDescriptor {}
}
