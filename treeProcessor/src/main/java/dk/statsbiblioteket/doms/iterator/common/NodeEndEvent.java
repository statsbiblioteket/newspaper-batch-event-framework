package dk.statsbiblioteket.doms.iterator.common;

/**
 * This event represents the iterator leaving a node. It is given when the iterator is finished processing all attributes
 * and subtrees from the current node, just before leaving it.
 */
public class NodeEndEvent extends Event {


    public NodeEndEvent(String localname) {
        super(localname, EventType.NodeEnd);
    }


}
