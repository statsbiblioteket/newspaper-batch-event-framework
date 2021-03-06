package dk.statsbiblioteket.newspaper.treenode;

/**
 * Represents a node in a tree including parent structure.
 */
public class TreeNode {
    private final String name;
    private final NodeType type;
    private final TreeNode parent;
    private final String location;

    /**
     * Constructor for this node.
     * @param name the name (label) of this node,
     * @param type the type of the node.
     * @param parent the parent of this node (may be null).
     * @param location the location of this node - either a doms-pid or a filepath.
     */
    public TreeNode(String name, NodeType type, TreeNode parent, String location) {
        this.name = name;
        this.type = type;
        this.parent = parent;
        this.location = location;
    }

    /**
     * Constructor for this node.
     * @param name the name (label) of this node,
     * @param type the type of the node.
     * @param parent the parent of this node (may be null).
     */
    public TreeNode(String name, NodeType type, TreeNode parent) {
        this(name, type, parent, null);
    }

    /**
     * The name of the node.
     */
    public String getName() {
        return name;
    }

    /**
     * The type as defined in the NodeType shortlist.
     */
    public NodeType getType() {
        return type;
    }

    /**
     * The parent node. Will always be non null, except for batch nodes.
     */
    public TreeNode getParent() {
        return parent;
    }

    /**
     * The location of the node as either doms pid or filepath.
     * @return
     */
    public String getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return "TreeNode{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", parent=" + parent +
                '}';
    }
}
