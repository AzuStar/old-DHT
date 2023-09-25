package Shared.multipart;

import java.io.Serializable;

/**
 * Contains information on the file stored on the DHT.<br>
 * </br>
 * File is a metadata construct that inits file storage on the Chord.
 * <hr />
 * <b>Part of data contract</b>
 */
public final class BinaryFileMeta implements Serializable {

    private static final long serialVersionUID = 5557272500661472518L;

    // Meta info
    /**
     * hashed file key<br>
     * </br>
     * high collision due to algorithm!
     */
    public int getFileId() {
        return meta_id;
    }

    private int meta_id;

    /**
     * File name
     */
    public String getFileName() {
        return meta_name;
    }

    private String meta_name;

    /**
     * File extension
     */
    public String getFileExtension() {
        return meta_extension;
    }

    private String meta_extension;

    /**
     * Complete file size in bytes
     */
    public long getFileSize() {
        return meta_size;
    }

    private long meta_size;

    /**
     * How many parts in file
     */
    public int getChunkParts() {
        return meta_parts;
    }

    private int meta_parts;

    public BinaryFileMeta(String filename, long meta_size) {
        String[] str = filename.split("\\.");
        String name = "";
        if (str.length > 1) {
            this.meta_extension = str[str.length - 1];
            for (int i = 0; true; i++) {
                name += str[i];
                if (i == str.length - 2)
                    break;
                name += '.';
            }
        } else {
            this.meta_name = filename;
            this.meta_extension = "";
        }
        this.meta_id = Utils.Hash(filename, Utils.KEY_BITS);
        this.meta_name = name;
        this.meta_size = meta_size;
        // Calculate how manu chunks will be in every file chunk
        int modChunk = (int) (meta_size % BinaryFileChunk.CHUNK_SIZE);
        meta_parts = (int) (meta_size / BinaryFileChunk.CHUNK_SIZE);
        if (modChunk > 0)
            meta_parts++;
    }

}