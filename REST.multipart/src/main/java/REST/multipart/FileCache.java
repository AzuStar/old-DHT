package REST.multipart;

import java.util.Timer;
import java.util.TimerTask;

import Shared.multipart.BinaryFileChunk;
import Shared.multipart.BinaryFileMeta;

public class FileCache {
    public BinaryFileMeta meta;
    public byte[] data;
    public int timeRemain = 3600;
    public Timer t;

    public FileCache(BinaryFileMeta meta) {
        this.meta = meta;
        t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                synchronized (this) {
                    if (--timeRemain == 0) {
                        App.removeCachedFile(meta.getFileId());
                        t.cancel();
                        t.purge();
                        return;
                    }
                }
            }
        }, 0, 1000);
    }

    public void makeDownloadable(BinaryFileChunk[] chunks) {
        data = new byte[(int) meta.getFileSize()];
        for (int i = 0; i < meta.getChunkParts(); i++) {
            int len = chunks[i].getData().length;
            for (int j = 0; j < len; j++)
                data[i * BinaryFileChunk.CHUNK_SIZE + j] = chunks[i].getData()[j];
            synchronized (this) {
                timeRemain = 3600;
            }
        }
    }
}