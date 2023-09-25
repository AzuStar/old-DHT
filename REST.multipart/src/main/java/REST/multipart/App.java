package REST.multipart;

import java.rmi.Naming;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.MultipartConfigFactory;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;

import Shared.multipart.BinaryFileChunk;
import Shared.multipart.BinaryFileMeta;
import Shared.multipart.IChordNode;
import Shared.multipart.Task;

@SpringBootApplication
public class App extends SpringBootServletInitializer {

    public static String address;

    public static IChordNode wellknownNode;

    private static HashMap<Integer, FileCache> fileCache = new HashMap<>();
    private static ReentrantReadWriteLock fileLock = new ReentrantReadWriteLock();

    public static HashMap<Integer, TaskCache> taskCache = new HashMap<>();
    private static ReentrantReadWriteLock taskLock = new ReentrantReadWriteLock();

    public static void startCaching(BinaryFileMeta meta) {
        fileLock.writeLock().lock();
        try {
            fileCache.put(meta.getFileId(), new FileCache(meta));
        } finally {
            fileLock.writeLock().unlock();
        }
    }

    public static boolean lockCache(int id) {
        fileLock.readLock().lock();
        try {
            return fileCache.containsKey(id);
        } finally {
            fileLock.readLock().unlock();
        }
    }

    public static void addCacheData(int id, BinaryFileChunk[] chunks) {
        fileLock.writeLock().lock();
        try {
            fileCache.get(id).makeDownloadable(chunks);
        } finally {
            fileLock.writeLock().unlock();
        }
    }

    /**
     * Read-only purposes pls
     * 
     * @param id
     * @return
     */
    public static FileCache getCachedFile(int id) {
        fileLock.readLock().lock();
        try {
            return fileCache.get(id);
        } finally {
            fileLock.readLock().unlock();
        }
    }

    public static void removeCachedFile(int id) {
        fileLock.writeLock().lock();
        try {
            fileCache.remove(id);
        } finally {
            fileLock.writeLock().unlock();
        }
    }

    // PROCESSING OF TASK

    public static void startTaskCaching(int id, String xml) {
        taskLock.writeLock().lock();
        try {
            taskCache.put(id, new TaskCache(id, xml));
        } finally {
            taskLock.writeLock().unlock();
        }
    }

    public static boolean lockTaskCache(int id) {
        taskLock.readLock().lock();
        try {
            return taskCache.containsKey(id);
        } finally {
            taskLock.readLock().unlock();
        }
    }

    /**
     * Read-only purposes pls
     * 
     * @param id
     * @return
     */
    public static TaskCache getCachedTask(int id) {
        taskLock.readLock().lock();
        try {
            return taskCache.get(id);
        } finally {
            taskLock.readLock().unlock();
        }
    }

    public static void removeCachedTask(int id) {
        taskLock.writeLock().lock();
        try {
            taskCache.remove(id);
        } finally {
            taskLock.writeLock().unlock();
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    // :anger:
    @Bean
    MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize("20971520");
        factory.setMaxRequestSize("20971520");
        return factory.createMultipartConfig();
    }

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        super.onStartup(servletContext);
        address = servletContext.getContextPath();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(1000);
                        wellknownNode = (IChordNode) Naming.lookup("rmi://localhost/GLOBAL-DHT");
                        break;
                    } catch (Exception e) {
                    }
                }
            }
        }).start();

    }

}