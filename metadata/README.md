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
(such as [creek-kafka][1]), expose their own interfaces, which extend the marker interfaces, and which can be used to 
define a resource the component uses, e.g. a Kafka Topic.

> ### NOTE
> Extensions expose resource _interfaces_ rather than _classes_ to keep code coupling to a minimum, minimising chances of 
> dependency hell, etc.

### Resource initialization

Resources can optionally be marked with **one** of the `ResourceInitialization` marker interfaces to control how
and when the resource is initialized:

#### Owned Resources

Resources tagged with the `OwnedResource` interface are conceptually owns by the service.

For example, service's generally _own_ their Kafka output topics, as this is the data they are responsible for generating
and managing. Therefore, the service's descriptor will define a descriptor for this resource, e.g. defining the
topic name, key & value types, partition count, etc.

When the service starts, Creek will automatically create the resource when initializing the service context.

> ### NOTE
> Future plans are to support a mode where owned resources are created by an initialization tool, prior to deployment.
> See https://github.com/creek-service/creek-service/issues/68.
 
#### Shared Resources

Resources tagged with the `SharedResource` interface are conceptually not owned by any service.

A shared resource's descriptor will normally be defined in a common library and referenced by all components
that wish to use it.

Shared resources are initialised via the [Init tool](https://github.com/creek-service/creek-platform/issues/7) before 
any service that requires them are deployed.

#### Unmanaged Resources

Resources tagged with the `UnmanagedResource` interface are deemed not to be initialized by Creek. Such resources
must be initialized some other way.

#### Unowned Resources

Any resource descriptor that does not implement one of the resource initialization marker interfaces is deemed to be 
an unowned resource, i.e. a resource owned by another component that this service is using.

For example, an upstream service defines an owned Kafka output topic that a service consumes. The service's descriptor
will import the upstream service's topic descriptor and obtain an _unowned_ input topic descriptor from it. 

[1]: https://github.com/creek-service/creek-kafka/tree/main/metadata