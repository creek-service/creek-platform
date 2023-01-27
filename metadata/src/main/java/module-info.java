/** Dependency free module containing base interfaces for defining components */
import org.creekservice.api.platform.metadata.ComponentDescriptor;

module creek.platform.metadata {
    exports org.creekservice.api.platform.metadata;

    uses ComponentDescriptor;
}
