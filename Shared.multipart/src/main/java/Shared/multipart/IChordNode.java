package Shared.multipart;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IChordNode extends Remote {

    /**
     * Joins the network at specified address. If address is not taken, the node
     * assumes its place.
     * 
     * @param node
     * @throws RemoteException
     */
    public void join(String node) throws RemoteException;

    /**
     * Searches for successor and returns the node that is supposed to be a
     * successor to the calling node.
     */
    public Node findSuccessor(int id) throws RemoteException;

    /**
     * Simple method to return key.
     */
    public int getId() throws RemoteException;

    /**
     * Gets closest to <i>id</i> node that is known to this node.
     */
    public Node closestPrecedingNode(int id) throws RemoteException;

    /**
     * Retrieves node's predecessor.
     * 
     * @return
     * @throws RemoteException
     */
    public Node getPredecessor() throws RemoteException;

    /**
     * Retrieves node's succesor.
     * 
     * @return
     * @throws RemoteException
     */
    public Node getSuccesor() throws RemoteException;

    /**
     * Tell node there might be a new, better, predecessor.
     * 
     * @param node
     * @throws RemoteException
     */
    public void notifyNode(Node node) throws RemoteException;

    // *****************
    // *File operations*
    // *****************

    /**
     * Get file's metadata by id, it returns null if file does not exist.
     * 
     * @param id
     * @return
     * @throws RemoteException
     */
    public BinaryFileMeta getFileMeta(String fileName) throws RemoteException;

    /**
     * Returns data chunk of file <b>fileName</b>, part <b>part</b>.
     * @param fileName
     * @param part
     * @return
     * @throws RemoteException
     */
    public BinaryFileChunk getFileChunk(String fileName, int part) throws RemoteException;

    /**
     * ...
     * 
     * @throws RemoteException
     */
    public void test() throws RemoteException;

    /**
     * Assumes there must be a file on this node.
     * 
     * @param metedata
     * @throws RemoteException if somehow id does not match
     */
    public void assumeFile(BinaryFileMeta metedata) throws RemoteException;

    /**
     * Sends chunk of file to the server. Server will resolve everything else.
     * @param data
     * @throws RemoteException
     */
    public void transmitChunk(BinaryFileChunk data) throws RemoteException;

    /**
     * File needs to be on the server prior
     * @param filename
     * @throws RemoteException
     */
    public void processFile(Task definition, String filename) throws RemoteException;

    public String getProcessedXML(String filename) throws RemoteException;
}