[![javadoc](https://javadoc.io/badge2/org.creekservice/creek-platform-metadata/javadoc.svg)](https://javadoc.io/doc/org.creekservice/creek-platform-metadata)

# Creek Platform Metadata
A dependency-free library that defines the types used to describe platform components.

A platform is make up of _services_ components and, optionally, services can be grouped into _aggregates_ components.
Each component has a _descriptor_: a java class that defines metadata about the component.
This metadata is used by Creek to provide its functionality and can be used by other components.
For example, the output of one component can be used to define the input of another.

## Descriptor types

There are two broad types of descriptors: [component](#component-descriptors) and [resource](#resource-descriptors) descriptors.

## Component Descriptors

Component descriptors define, among other things, a component's [inputs](#inputs), [internals](#internals) and [outputs](#outputs).
There are two types of component: [aggregate](#aggregate-descriptors) and [service](#service-descriptors) descriptors.

Authors of Creek based services will build implementations of both aggregate and service descriptors. 

### Inputs

The inputs of a component define any external resources the component consumes/reads.

All input resources implement the [`ComponentInput`](src/main/java/org/creekservice/api/platform/metadata/ComponentInput.java)
marker interface.

### Internals

The internals of a component define any external resources the component uses internally to perform its function.

All internal resources implement the [`ComponentInternal`](src/main/java/org/creekservice/api/platform/metadata/ComponentInternal.java)
marker interface.

### Outputs

The outputs of a component define any external resources the component produces/writes.

All output resources implement the [`ComponentOutput`](src/main/java/org/creekservice/api/platform/metadata/ComponentOutput.java)
marker interface.

### Service descriptors

Services should provide an implementation of [`ServiceDescriptor`](src/main/java/org/creekservice/api/platform/metadata/ServiceDescriptor.java)
to define key metadata needed by other components and the Creek system, including things like
[inputs](#inputs), [internals](#internals) and [outputs](#outputs), docker image name and any
environment variables to set during testing.

### Aggregate descriptors

Each aggregate should provide an implementation of [`AggregateDescriptor`](src/main/java/org/creekservice/api/platform/metadata/AggregateDescriptor.java)
to define their public [inputs](#inputs) and [outputs](#outputs).

## Resource Descriptors

Resource descriptors define the resources a component uses in its [inputs](#inputs), [internals](#internals) and [outputs](#outputs).
There are corresponding `ComponentInput`, `ComponentInternal` and `ComponentOutput` marker interfaces, respectively. 

Authors of creek services are not expected to implement the above marker interfaces. Creek extensions, 
(such as [creek-kafka][1]), expose their own interfaces, which extend these marker interfaces, and which can be used to 
define a resource the component uses, e.g. a Kafka Topic.

> ### NOTE
> The reason extensions expose resource _interfaces_ rather than _classes_ is to keep code coupling to a minimum, 
> thereby minimising chances of dependency hell. Extensions provide example code that can be cut & paste for
> creating the required implementations.

### Resource initialization

#### Resource ownership

Conceptually, resources, such as a Kafka topic or database, can be owned by a particular service, owned by another service, or shared.
This is encoded into the type of the resource descriptor, e.g. an `OwnedKafkaTopicInput` vs (unowned)`KafkaTopicInput`.

Creek supports the following resource descriptor ownership model:

* [_owned_](#owned-resources): a descriptor to a resource conceptually owned by the service.
* [_unowned_](#unowned-resources): a descriptor to a resource conceptually owned by another service.
* [_shared_](#shared-resources): a descriptor to a resource not conceptually owned by any service.
            (This should be a rare thing in a well architected platform).
* [_unmanaged_](#unmanaged-resources): a descriptor to a resource which Creek will not manage.

##### Owned Resources

Resources tagged with the `OwnedResource` interface are conceptually owns by the service.

For example, services generally _own_ their Kafka output topics, as this is the data they are responsible for generating
and managing. Therefore, the service's descriptor will define an _owned_ descriptor for this resource, e.g. defining the
topic name, key & value types, partition count, and any other parameters required to allow the service to create the topic.

When the service starts, Creek will automatically create the resource when initializing the service.

> ### NOTE
> Future plans are to support a mode where owned resources are created by an initialization tool, prior to deployment.
> See [issue-68][2].

The owned resource types provided by extensions will define helper methods to obtain an unowned descriptor from an
owned one. For example, the `OwnedKafkaTopicOutput` has a `toInput` method to obtain an unowned `KafkaTopicInput`.

##### Unowned Resources

Resources tagged with the `UnownedResource` interface are conceptually owns by another service.

For example, services generally consume Kafka the _owned_ output topics of upstream services. 
Therefore, the service's descriptor will define an _unowned_ descriptor for such resource, e.g. defining the
topic name, key & value types, partition count, etc. 

When the service starts, Creek will _not_ initialize unowned resources.

Unowned resource types provided by extensions should not normally be directly created. Instead, the unowned descriptor 
should be created by calling an appropriate helper method on the owned resource. For example, an unowned `KafkaTopicInput`
is obtained by calling `toInput` on the associated `OwnedKafkaTopicOutput`. 

##### Shared Resources

Resources tagged with the `SharedResource` interface are conceptually not owned by any service.

A shared resource's descriptor will normally be defined in a common library and referenced by all components
that wish to use it.

Shared resources are initialised via the [Init tool](https://github.com/creek-service/creek-platform/issues/7) before 
any service that requires them are deployed.

Shared resource types provided by extensions will implement the `ComponentInput`, `ComponentInternal` and/or `ComponentOutput` 
interfaces, as appropriate for the resource type.  This allows a shared single definition to be used directly as a component's
input, internal or output.

##### Unmanaged Resources

Any resource descriptor that does not implement one of the resource initialization marker interfaces are deemed not 
to be initialized by Creek. Such resources must be initialized some other way.

#### Resource deployment

Resources, for example Kafka topics or databases, can be initialized/created at different points in the deployment process.
Most resources are created by the service that [conceptually owns](#owned-resources) them when the service starts for the first time.
However, some resources may be to be created before a service starts, e.g. [shared resources](#shared-resources):
resources that are not owned by any one single service.

Creek supports initializing resources at different points in the process by defining the following stages of resource
initialization:

* **init**: Initialize an environment with any resources that should be created before services are started.
  This is generally [shared resources](#shared-resources), though, in the future Creek will also support
  [pre-initializing owned resources][2] as well.
* **service**: Initialize any resources that the service [conceptually owns](#owned-resources) and which are managed
  by Creek.
* **test**: A special stage used for [system testing][3]: for a given subset of components under test, initialize a test
  environment to ensure all unowned resources are created.

The [Init tool](https://github.com/creek-service/creek-platform/issues/7) can be used to manually initialize resources 
at these different stages.

[1]: https://www.creekservice.org/creek-kafka/#metadata-types
[2]: https://github.com/creek-service/creek-service/issues/68
[3]: https://github.com/creek-service/creek-system-test