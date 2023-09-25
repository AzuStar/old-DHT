package DHT.multipart;

import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import Shared.multipart.BinaryFileChunk;
import Shared.multipart.BinaryFileMeta;
import Shared.multipart.IChordNode;
import Shared.multipart.Node;
import Shared.multipart.PassThroughLock;
import Shared.multipart.Task;
import Shared.multipart.Utils;

public class ChordNode extends UnicastRemoteObject implements IChordNode {
	/**
	 * Default version
	 */
	private static final long serialVersionUID = 12365235L;

	public static final int KEY_BITS = 8;

	Random rnd = new Random();

	// for each peer link that we have, we store a reference to the peer node plus a
	// "cached" copy of that node's key; this means that whenever we change e.g. our
	// successor reference we also set successorKey by doing successorKey =
	// successor.getKey()
	Node successor = null;

	Node predecessor = null;

	// my finger table; note that all "node" entries will initially be "null"; your
	// code should handle this
	Node finger[];
	int nextFingerFix;

	HashMap<Integer, BinaryFile> dataStore = new HashMap<Integer, BinaryFile>();

	HashMap<Integer, Task> fileTasks = new HashMap<>();
	Queue<Task> awaitingTasks = new LinkedList<>();
	PassThroughLock taskLock = new PassThroughLock();

	Queue<BinaryFile> moveDownFiles = new LinkedList<>();
	PassThroughLock moveDownLock = new PassThroughLock();

	// note: you should always use getKey() to get a node's key; this will make the
	// transition to RMI easier
	private int myId;
	private String myAddress;

	ChordNode(String myAddress) throws RemoteException {
		this.myAddress = myAddress;
		myId = Utils.Hash(myAddress, KEY_BITS);

		successor = new Node(this, myId);

		// initialise finger table (note all "node" links will be null!)
		finger = new Node[KEY_BITS];
		Arrays.fill(finger, new Node(null, 0));
	}

	// -- API functions --

	void put(String key, byte[] value) {
		// find the node that should hold this key and add the key and value to that
		// node's local store
	}

	byte[] get(String key) {
		// find the node that should hold this key, request the corresponding value from
		// that node's local store, and return it

		return null;
	}

	// -- state utilities --

	public int getId() {
		return myId;
	}

	// -- topology management functions --
	public void join(String network) throws RemoteException {
		try {
			IChordNode node = (IChordNode) Naming.lookup("rmi://localhost/" + network);
			try {
				LocateRegistry.createRegistry(1099);
			} catch (RemoteException e) {
			}
			try {
				Naming.bind(myAddress, this);
			} catch (AlreadyBoundException e) {
			}
			successor = node.findSuccessor(myId);
			Timer maintenance = new Timer();
			// this part of the code gets ugly, but, god, I don't belive I say that
			// but it's because java is rubbish
			// in C# I'd just cast the whole thing into a function and have bunch of
			// functions that do the thing
			// anyway...
			maintenance.schedule(new TimerTask() {
				@Override
				public void run() {
					// if (successor != null && predecessor != null)
					// System.out.println(predecessor.myId + "->" + myId + "->" + successor.myId);
					// Arrays.stream(finger).forEach((node) -> {System.out.print(node.myId+"|");});
					// System.out.println();

					// Maintenance of dht
					try {
						stabilise();
					} catch (Exception e) {
						System.out.println("Stabilization in this tick failed");
					}
					try {
						fixFingers();
					} catch (Exception e) {
						System.out.println("Fixing fingers in this tick failed");
					}
					try {
						checkLinks();
					} catch (Exception e) {
						System.out.println("Error when checking links");
					}

					// Data maintenance & movement tasks
					try {
						checkDataMoveDown();
					} catch (Exception e) {
						System.out.println("When moving data something happened...");
					}
					// moves data down the hierarchy
					if (moveDownLock.lock())
						new Thread(new Runnable() {
							@Override
							public void run() {
								BinaryFile file = moveDownFiles.poll();
								try {
									if (file != null)
										for (BinaryFileChunk chunk : file.getAllChunks())
											predecessor.node.transmitChunk(chunk);
								} catch (RemoteException e) {
									// dont drop it lol
									synchronized (dataStore) {
										dataStore.put(file.metadata.getFileId(), file);
									}
								} finally {
									// absolutely fkin certain
									// unlock movement
									moveDownLock.unlock();
								}
							}
						}).start();

					if (awaitingTasks.size() > 0)
						if (taskLock.lock())
							new Thread(new Runnable() {
								@Override
								public void run() {
									Task task = awaitingTasks.poll();
									try {
										BinaryFile file = dataStore.get(task.getFileId());
										byte[] data = new byte[(int) file.metadata.getFileSize()];
										for (int i = 0; i < file.metadata.getChunkParts(); i++) {
											int len = file.getChunk(i).getData().length;
											for (int j = 0; j < len; j++)
												data[i * BinaryFileChunk.CHUNK_SIZE + j] = file.getChunk(i)
														.getData()[j];
											task.doAllRegistredProcessing(new String(data));
										}
									} finally {
										// absolutely fkin certain
										// unlock movement
										taskLock.unlock();
									}
								}
							}).start();

				}
			}, 1000, 1000);
			// TimerTask DONE.
			// ----------------
		} catch (NotBoundException | java.rmi.ConnectException e) {
			try {
				LocateRegistry.createRegistry(1099);
				Naming.bind(network, this);
				// predecessor = new Node(this, myId);
				// Arrays.fill(finger, new Node(this, myId));
				join(network);
			} catch (MalformedURLException | AlreadyBoundException | RemoteException exp) {
				exp.printStackTrace();
				System.exit(-1);
			}
		} catch (MalformedURLException | RemoteException e1) {
			e1.printStackTrace();
		}
	}

	public Node getPredecessor() throws RemoteException {
		return predecessor;
	}

	public Node getSuccesor() throws RemoteException {
		return successor;
	}

	// -- utility functions --
	public Node findSuccessor(int id) throws RemoteException {
		if (successor.node == this || isInHalfOpenRangeR(id, myId, successor.myId))
			return successor;
		Node node = closestPrecedingNode(id);
		if (node.node == this)
			return new Node(this, myId);
		return node.node.findSuccessor(id);
	}

	public Node closestPrecedingNode(int id) throws RemoteException {
		for (int i = KEY_BITS - 1; i >= 0; i--)
			if (isInClosedRange(finger[i].myId, this.myId, id))
				return finger[i];
		return new Node(this, myId);
	}

	// -- range check functions; they deal with the added complexity of range wraps
	// --
	/** x is in [a,b] ? */
	boolean isInOpenRange(int key, int a, int b) {
		if (b > a)
			return key >= a && key <= b;
		else
			return key >= a || key <= b;
	}

	/** x is in (a,b) ? */
	boolean isInClosedRange(int key, int a, int b) {
		if (b > a)
			return key > a && key < b;
		else
			return key > a || key < b;
	}

	/** x is in [a,b) ? */
	boolean isInHalfOpenRangeL(int key, int a, int b) {
		if (b > a)
			return key >= a && key < b;
		else
			return key >= a || key < b;
	}

	/** x is in (a,b] ? */
	boolean isInHalfOpenRangeR(int key, int a, int b) {
		if (b > a)
			return key > a && key <= b;
		else
			return key > a || key <= b;
	}

	// -- maintenance --
	public void notifyNode(Node potentialPredecessor) {
		if (predecessor == null || isInClosedRange(potentialPredecessor.myId, predecessor.myId, myId))
			predecessor = potentialPredecessor;
	}

	void stabilise() throws RemoteException {
		Node node = successor.node.getPredecessor();
		if (node != null)
			if (isInClosedRange(node.myId, myId, successor.myId))
				successor = node;
		successor.node.notifyNode(new Node(this, myId));
	}

	public void fixFingers() throws RemoteException {
		nextFingerFix++;
		if (nextFingerFix >= KEY_BITS)
			nextFingerFix = 0;
		finger[nextFingerFix] = findSuccessor(myId + (int) Math.pow(2, nextFingerFix));
	}

	void checkLinks() throws RemoteException {
		try {
			predecessor.node.getId();
		} catch (Exception e) {
			predecessor = null;
		}
		try {
			successor.node.getId();
		} catch (Exception e) {
			successor = new Node(this, myId);
			successor = findSuccessor(myId);
		}
	}

	@Override
	public void assumeFile(BinaryFileMeta metadata) throws RemoteException {
		synchronized (dataStore) {
			if (dataStore.containsKey(metadata.getFileId()))
				dataStore.remove(metadata.getFileId());
			dataStore.put(metadata.getFileId(), new BinaryFile(metadata));
		}
		System.out.println("LUMBERJACK: File meta received for file " + metadata.getFileName());
	}

	@Override
	public BinaryFileMeta getFileMeta(String fileName) throws RemoteException {
		BinaryFile file;
		synchronized (dataStore) {
			file = dataStore.get(Utils.Hash(fileName, KEY_BITS));
		}
		if (file == null)
			return null;
		return file.metadata;
	}

	@Override
	public BinaryFileChunk getFileChunk(String fileName, int part) throws RemoteException {
		synchronized (dataStore) {
			return dataStore.get(Utils.Hash(fileName, KEY_BITS)).getChunk(part);
		}
	}

	@Override
	public void transmitChunk(BinaryFileChunk data) throws RemoteException {
		synchronized (dataStore) {
			BinaryFile file = dataStore.get(data.getFileId());
			if (file.complete)
				return;
			file.putChunk(data);
		}
		System.out.println("LUMBERJACK: Chunk transmitted, first 10 bytes: "
				+ new String(Arrays.copyOfRange(data.getData(), 0, 10)));
	}

	void checkDataMoveDown() throws RemoteException {
		ArrayList<BinaryFileMeta> file_ids = new ArrayList<>();
		synchronized (dataStore) {
			for (int file_id : dataStore.keySet())
				if (dataStore.get(file_id).complete)
					if (predecessor.myId != myId)
						if (file_id <= predecessor.myId) {
							file_ids.add(dataStore.get(file_id).metadata);
							System.out.println("LUMBERJACK: Moving boys");
						}
			for (BinaryFileMeta meta : file_ids) {
				synchronized (dataStore) {
					dataStore.remove(meta.getFileId());
				}
				moveDownFiles.add(dataStore.get(meta.getFileId()));
			}

		}
	}

	public void test() throws RemoteException {
	}

	public static void main(String args[]) throws Exception {

		ChordNode node = new ChordNode(args[0]);
		node.join("GLOBAL-DHT");
		System.out.println("Server in action...\nFootprint:" + node.myAddress + ":" + node.myId);

	}

	@Override
	public void processFile(Task definition, String filename) throws RemoteException {
		synchronized (fileTasks) {
			fileTasks.put(Utils.Hash(filename, Utils.KEY_BITS), definition);
		}
		synchronized (awaitingTasks) {
			awaitingTasks.add(definition);
		}
	}

	@Override
	public String getProcessedXML(String filename) {
		synchronized (fileTasks) {
			Task task = fileTasks.get(Utils.Hash(filename, Utils.KEY_BITS));
			if (task != null)
				if (task.isComplete())
					return task.getXML();
		}
		return null;
	}

}