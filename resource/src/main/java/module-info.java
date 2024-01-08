/** Module for working with Creek component resources */
module creek.platform.resource {
    requires transitive creek.platform.metadata;
    requires creek.base.type;
    requires creek.observability.logging;
    requires com.github.spotbugs.annotations;

    exports org.creekservice.api.platform.resource;
}
