package dk.statsbiblioteket.doms.iterator.common;

/**
 * The event types.
 */
public enum EventType {
    /**
     * Node begin is given when we enter a fedora object / filesystem directory
     */
    NodeBegin,

    /**
     * Node end is given when we are about the leave a fedora object / filesystem directory
     */
    NodeEnd,
    /**
     * Attribute is given when we encounter a fedora datastream / filesystem file
     */
    Attribute
}
