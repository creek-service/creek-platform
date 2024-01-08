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

package org.creekservice.api.platform.metadata;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResourceCollectionTest {

    @Mock(extraInterfaces = {ResourceDescriptor.class})
    private ResourceCollection collection;

    @Mock private ResourceDescriptor res0;
    @Mock private ResourceDescriptor res1;
    @Mock private ResourceDescriptor res2;

    @Test
    void shouldCollectUniqueResources() {
        // Given:
        when(collection.resources()).thenAnswer(inv -> Stream.of(res0, res1, res0, res1));
        when(res0.resources()).thenAnswer(inv -> Stream.of(res1));

        // When:
        final List<ResourceDescriptor> result =
                ResourceCollection.collectResources(collection).collect(toList());

        // Then:
        assertThat(result, containsInAnyOrder(res0, res1));
    }

    @Test
    void shouldIgnoreSelfReferences() {
        // Given:
        when(collection.resources()).thenAnswer(inv -> Stream.of(res0, res1, collection));
        when(res0.resources()).thenAnswer(inv -> Stream.of(collection));

        // When:
        final List<ResourceDescriptor> result =
                ResourceCollection.collectResources(collection).collect(toList());

        // Then:
        assertThat(result, containsInAnyOrder(res0, res1));
    }

    @Test
    void shouldNotFollowCircularReferences() {
        // Given:
        when(collection.resources()).thenAnswer(inv -> Stream.of(res0));
        when(res0.resources()).thenAnswer(inv -> Stream.of(res1));
        when(res1.resources()).thenAnswer(inv -> Stream.of(res0, collection));

        // When:
        final List<ResourceDescriptor> result =
                ResourceCollection.collectResources(collection).collect(toList());

        // Then:
        assertThat(result, containsInAnyOrder(res0, res1));
    }

    @Test
    void shouldNotFollowingSelfCircularReferences() {
        // Given:
        when(collection.resources()).thenAnswer(inv -> Stream.of(res0, res1, collection));
        when(res0.resources()).thenAnswer(inv -> Stream.of(res0));

        // When:
        final List<ResourceDescriptor> result =
                ResourceCollection.collectResources(collection).collect(toList());

        // Then:
        assertThat(result, containsInAnyOrder(res0, res1));
    }

    @Test
    void shouldReturnChildResourcesBeforeParent() {
        // Given:
        when(collection.resources()).thenAnswer(inv -> Stream.of(res0));
        when(res0.resources()).thenAnswer(inv -> Stream.of(res2, res1));
        when(res1.resources()).thenAnswer(inv -> Stream.of(res2));

        // When:
        final List<ResourceDescriptor> result =
                ResourceCollection.collectResources(collection).collect(toList());

        // Then:
        assertThat(result, is(List.of(res2, res1, res0)));
    }
}
