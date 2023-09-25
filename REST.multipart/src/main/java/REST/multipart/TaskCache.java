package REST.multipart;

import java.util.Timer;
import java.util.TimerTask;

public class TaskCache {
    public int id;
    public String XML;
    public int timeRemain = 360;
    public Timer t;

    public TaskCache(int id, String xml) {
        this.id = id;
        this.XML = xml;
        t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                synchronized (this) {
                    if (--timeRemain == 0) {
                        App.removeCachedFile(id);
                        t.cancel();
                        t.purge();
                        return;
                    }
                }
            }
        }, 0, 1000);
    }

}