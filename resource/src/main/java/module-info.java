/** Module for working with Creek component resources */
module creek.platform.resource {
    requires transitive creek.platform.metadata;
    requires creek.base.type;
    requires creek.observability.logging;

    exports org.creekservice.api.platform.resource;
}
