package DHT.multipart;

import java.io.Serializable;
import java.util.Arrays;

import Shared.multipart.BinaryFileChunk;
import Shared.multipart.BinaryFileMeta;

public class BinaryFile implements Serializable {

    private static final long serialVersionUID = 1527773050508145253L;
    // file uploaded and 100% available
    public boolean complete = false;
    // counter for how many chunks on server already
    public int chunkCount = 0;

    public BinaryFileMeta metadata;
    private BinaryFileChunk[] chunks;

    public BinaryFile(BinaryFileMeta meta) {
        metadata = meta;
        chunks = new BinaryFileChunk[meta.getChunkParts()];
    }

    public BinaryFileChunk[] getAllChunks() {
        return chunks;
    }

    public void putChunk(BinaryFileChunk chunk) {
        chunks[chunk.getPart()] = chunk;
        if (++chunkCount >= metadata.getChunkParts()) {
            for (int i = 0; i < metadata.getChunkParts(); i++)
                if (chunks[i] == null)
                    return;
            complete = true;
        }

    }

    public BinaryFileChunk getChunk(int index) {
        return chunks[index];
    }

}