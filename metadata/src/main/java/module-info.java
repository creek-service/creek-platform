/** Dependency free module containing base interfaces for defining components */
import org.creekservice.api.platform.metadata.ComponentDescriptor;

/**
 * Platform Metadata Module.
 *
 * <p>A dependency-free module that defines the interfaces used to describe platform components and
 * resources.
 */
module creek.platform.metadata {
    exports org.creekservice.api.platform.metadata;

    uses ComponentDescriptor;
}
