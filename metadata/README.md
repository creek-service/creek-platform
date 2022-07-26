# Creek Platform Metadata
A dependency-free library that defines the types used to describe platform components.

A platform is make up of _services_ components and, optionally, services can be grouped into _aggregates_ components.
Each component has a _descriptor_: a java class that defines metadata about the component.
This metadata is used by Creek to provide its functionality and can be used by other components.
For example, the output of one component can be used to define the input of another.

## Descriptor types

### Service descriptors

Services should provide an implementation of [`ServiceDescriptor`](src/main/java/org/creekservice/api/platform/metadata/ServiceDescriptor.java)
to define key metadata needed by other components and the Creek system, including things like
[inputs](#inputs), [internals](#internals), [outputs](#outputs), docker image name and any
environment variables to set during testing.

### Aggregate descriptors

Each aggregate should provide an implementation of [`AggregateDescriptor`](src/main/java/org/creekservice/api/platform/metadata/AggregateDescriptor.java)
to define their public [inputs](#inputs) and [outputs](#outputs).

## Component Descriptors

Descriptors define, among other things, the components [inputs](#inputs), [internals](#internals), [outputs](#outputs):

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
