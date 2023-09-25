package Shared.multipart;

import java.io.Serializable;
import java.util.Arrays;

/**
 * This class serves a chunk of a file. Chunk is sent to
 */
public class BinaryFileChunk implements Serializable {

    private static final long serialVersionUID = -671392793165328867L;

    /**
     * How many bytes every file holds
     */
    public static final int CHUNK_SIZE = 4092;

    private int fileId;
    private int chunkPart;
    private byte[] data;

    public int getFileId() {
        return fileId;
    }

    /**
     * This is what part of the file this chunk belongs to.
     * 
     * @return chunk #
     */
    public int getPart() {
        return chunkPart;
    }

    public byte[] getData() {
        return data;
    }

    private BinaryFileChunk(int fileId, int chunkPart, byte[] data) {
        this.fileId = fileId;
        this.chunkPart = chunkPart;
        this.data = data;
    }

    public static BinaryFileChunk[] computeFileChunks(BinaryFileMeta meta, byte[] fileBytes) {
        BinaryFileChunk[] chunks = new BinaryFileChunk[meta.getChunkParts()];
        for (int i = 0; i < meta.getChunkParts() - 1; i++)
            chunks[i] = new BinaryFileChunk(meta.getFileId(), i,
                    Arrays.copyOfRange(fileBytes, i * CHUNK_SIZE, (i + 1) * CHUNK_SIZE - 1));
        int lastIndex = meta.getChunkParts() - 1;
        chunks[lastIndex] = new BinaryFileChunk(meta.getFileId(), lastIndex,
                Arrays.copyOfRange(fileBytes, lastIndex * CHUNK_SIZE, (fileBytes.length - 1) % CHUNK_SIZE));
        return chunks;
    }
}