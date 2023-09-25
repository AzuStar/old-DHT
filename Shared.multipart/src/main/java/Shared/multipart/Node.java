package Shared.multipart;

import java.io.Serializable;

// Caching and analytics purposes
public class Node implements Serializable {
    private static final long serialVersionUID = 34567348235L;
    public IChordNode node;
    public int myId;
    // More stuff?
    
    // Contrustor
    public Node(IChordNode node, int id) {
        this.node = node;
        this.myId = id;
    }

}