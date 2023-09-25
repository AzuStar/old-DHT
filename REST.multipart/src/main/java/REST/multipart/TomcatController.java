package REST.multipart;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import Shared.multipart.BinaryFileChunk;
import Shared.multipart.BinaryFileMeta;
import Shared.multipart.Node;
import Shared.multipart.Task;
import Shared.multipart.Utils;

//My global tomcat spring controller
//If there needs to be more done for app
//Create other controller classes
@RestController
public class TomcatController {

    public static Logger logger = Logger.getLogger(TomcatController.class);

    @RequestMapping(value = { "/index", "/" })
    public ResponseEntity<String> catchIndex() {
        StringBuilder page = new StringBuilder(1000);
        page.append("<!DOCTYPE html><html><head><meta name=\"viewport\" content=\"width=device-width\">");
        page.append("<title>SIM index</title><link rel=\"stylesheet\" href=\"primary.css\"></head>");
        page.append("<body>");
        page.append("<div class=\"mainbox\">");
        page.append("<h1>Welcome to SIM!</h1><h3>A simple file manager powered by dht.</h3><hr />");
        page.append(
                "Visit <a href=\"" + App.address + "/submitFile\">file submission page</a> to submit a file.<br />");
        page.append("If you have submitted the file you can visit <a href=\"" + App.address
                + "/retrieve\">file retriever page</a> to get the file you've uploaded.");
        page.append("<p>There is also a page that lets you see processed information about the file <a href=\""
                + App.address + "/processing\">visit processing page</a> to get the file you've processed.</p>");
        page.append("</div>");
        page.append("</body>");
        page.append("</html>");
        return new ResponseEntity<String>(page.toString(), HttpStatus.OK);
    }

    @RequestMapping(value = "/files/{filename:.+}", method = RequestMethod.GET)
    public void downloadFile(HttpServletResponse resp, @PathVariable("filename") String filename) throws IOException {
        FileCache fc = App.getCachedFile(Utils.Hash(filename, Utils.KEY_BITS));
        resp.setContentType("application/octet-stream");
        resp.setHeader("Content-Disposition", String.format("inline; filename=\"" + filename + "\""));
        resp.setContentLength(fc.data.length);
        InputStream is = new ByteArrayInputStream(fc.data);
        FileCopyUtils.copy(is, resp.getOutputStream());
    }

    @RequestMapping(value = "/retrieve", method = RequestMethod.GET)
    public ResponseEntity<String> catchRetrieve(@RequestParam(value = "fileName", required = false) String fileName) {
        StringBuilder page = new StringBuilder(1000);
        page.append("<!DOCTYPE html><html><head><meta name=\"viewport\" content=\"width=device-width\">");
        if (fileName == null || fileName.equals("")) {
            page.append("<title>SIM retriever</title><link rel=\"stylesheet\" href=\"primary.css\"></head>");
            page.append("<body>");
            page.append("<div class=\"mainbox\">");
            page.append(
                    "<h2 class=\"txtcenter\">File Retriever</h2><p class=\"txtcenter\">Please enter your file name in to the box below and click on Submit button.</p><hr />");
            page.append(
                    "<form class=\"txtcenter\" style=\"margin-top: 30px;\" method=\"GET\" action=\"\"><input style=\"width: 180px; height: 14px\" placeholder=\"myfile.txt\" name=\"fileName\"><br /><input style=\"margin-top: 10px;\" type=\"submit\"></form>");
            page.append("</div>");
            page.append("</body>");
            page.append("</html>");
        } else {
            FileCache file = App.getCachedFile(Utils.Hash(fileName, Utils.KEY_BITS));
            if (file == null) {
                try {
                    Node nodeWithFile = App.wellknownNode.findSuccessor(Utils.Hash(fileName, Utils.KEY_BITS));
                    BinaryFileMeta meta = nodeWithFile.node.getFileMeta(fileName);
                    if (meta != null)
                        App.startCaching(meta);
                } catch (RemoteException e) {
                    page.append("<title>SIM retriever</title><link rel=\"stylesheet\" href=\"primary.css\"></head>");
                    page.append("<body>");
                    page.append("<div class=\"mainbox\">");
                    page.append(
                            "<h2 class=\"txtcenter\">Request has failed to reach host!</h2><hr /><p class=\"txtcenter\">Try your request again...</p>");
                    page.append("</div>");
                    page.append("</body>");
                    page.append("</html>");
                    return new ResponseEntity<String>(page.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
            file = App.getCachedFile(Utils.Hash(fileName, Utils.KEY_BITS));
            if (file == null) {
                page.append("<title>SIM retriever</title><link rel=\"stylesheet\" href=\"primary.css\"></head>");
                page.append("<body>");
                page.append("<div class=\"mainbox\">");
                page.append("<h2 class=\"txtcenter\">File " + fileName
                        + " does not exist on the network!</h2><hr /><p class=\"txtcenter\">This could be due slow communication between hosts or your file simply has not been published yet.</p>");
                page.append("</div>");
                page.append("</body>");
                page.append("</html>");
            } else if (file.data != null) {
                page.append("<title>SIM retriever</title><link rel=\"stylesheet\" href=\"primary.css\"></head>");
                page.append("<body>");
                page.append("<div class=\"mainbox txtcenter\">");
                page.append("<h2>File " + fileName
                        + " is available for download!</h2><p>You can download file by clicking the button below which will start file download.</p><hr />");
                page.append("<p>File ID: " + file.meta.getFileId() + "</p>");
                page.append("<p>File name: " + file.meta.getFileName() + "</p>");
                page.append("<p>File size: " + file.meta.getFileSize() + "</p>");
                page.append("<p>File extension: " + file.meta.getFileExtension() + "</p>");
                page.append("<p>File total parts: " + file.meta.getChunkParts() + "</p>");
                page.append("<hr /><button onclick=\"location.href='" + App.address + "/files/" + fileName
                        + "';\">Download " + fileName + "</button>");
                page.append("<p>This file will remain on the server for <b>" + file.timeRemain + "</b> seconds</p>");
                page.append("</div>");
                page.append("<script>setTimeout(function(){window.location.reload(1);}, 5000);</script>");
                page.append("</body>");
                page.append("</html>");
            } else if (file.meta != null) {
                page.append("<title>SIM retriever</title><link rel=\"stylesheet\" href=\"primary.css\"></head>");
                page.append("<body>");
                page.append("<div class=\"mainbox txtcenter\">");
                page.append("<h2 class=\"txtcenter\">File " + fileName
                        + " has metada avaliable!</h2><p class=\"txtcenter\">The file finally has metadata avaliable! Soon it will have chunks available for download!</p><hr />");
                page.append("<p>File ID: " + file.meta.getFileId() + "</p>");
                page.append("<p>File name: " + file.meta.getFileName() + "</p>");
                page.append("<p>File size: " + file.meta.getFileSize() + "</p>");
                page.append("<p>File extension: " + file.meta.getFileExtension() + "</p>");
                page.append("<p>File total parts: " + file.meta.getChunkParts() + "</p>");
                page.append("</div>");
                page.append("<script>setTimeout(function(){window.location.reload(1);}, 5000);</script>");
                page.append("</body>");
                page.append("</html>");
                if (App.lockCache(Utils.Hash(fileName, Utils.KEY_BITS))) {
                    BinaryFileMeta worker_meta = file.meta;
                    new Thread(new Runnable() {

                        @Override
                        public void run() {

                            try {
                                App.startCaching(worker_meta);
                                Node nodeWithFile = App.wellknownNode.findSuccessor(worker_meta.getFileId());
                                BinaryFileChunk[] chunks = new BinaryFileChunk[worker_meta.getChunkParts()];
                                for (int i = 0; i < worker_meta.getChunkParts(); i++)
                                    chunks[i] = nodeWithFile.node.getFileChunk(fileName, i);
                                App.addCacheData(worker_meta.getFileId(), chunks);
                            } catch (Exception e) {
                            }
                        }
                    }).start();
                }
            }
        }

        return new ResponseEntity<String>(page.toString(), HttpStatus.OK);

    }

    @RequestMapping(value = "/upload", method = RequestMethod.GET)
    public ResponseEntity<String> catchUpload(@RequestParam(value = "error", required = false) boolean error) {
        if (error) {
            StringBuilder page = new StringBuilder(1000);
            page.append("<!DOCTYPE html><html><head><meta name=\"viewport\" content=\"width=device-width\">");
            page.append("<title>SIM upload</title><link rel=\"stylesheet\" href=\"primary.css\"></head>");
            page.append("<body>");
            page.append("<div class=\"mainbox\">");
            page.append("<h2 class=\"txtcenter\">File upload failed!</h2><hr />");
            page.append("<p class=\"txtcenter\">This error happened for unknown reason. Try <a href=\"" + App.address
                    + "/submitFile\">upload</a> file again.</p>");
            page.append("</div>");
            page.append("</body>");
            page.append("</html>");
            return new ResponseEntity<String>(page.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        HttpHeaders hed = new HttpHeaders();
        hed.add("Location", App.address + "/submitFile");
        return new ResponseEntity<String>(hed, HttpStatus.FOUND);
    }

    @RequestMapping(value = "/com", method = RequestMethod.GET)
    public ResponseEntity<String> catchUpload(@RequestParam(value = "id") int id) {
        StringBuilder page = new StringBuilder(1000);
        page.append("<!DOCTYPE html><html><head><meta name=\"viewport\" content=\"width=device-width\">");
        page.append("<title>SIM COM</title><link rel=\"stylesheet\" href=\"primary.css\"></head>");
        page.append("<body>");
        page.append("<div class=\"mainbox\">");
        Node idNode;
        try {
            idNode = App.wellknownNode.findSuccessor(id);
            page.append("<h2 class=\"txtcenter\">ID of server closest to " + id + "</h2><hr />");
            page.append("<p class=\"txtcenter\">Has id of <b>" + idNode.myId + "</b></p>");
        } catch (RemoteException e) {
            page.append("<h1>COM with DHT failed!</h1>");
        }
        page.append("</div>");
        page.append("</body>");
        page.append("</html>");
        return new ResponseEntity<String>(page.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @RequestMapping(value = "/XML", method = RequestMethod.GET, produces = { MediaType.APPLICATION_XML_VALUE })
    public ResponseEntity<String> giveXML(@RequestParam("target") String target) {
        return new ResponseEntity<>(App.getCachedTask(Utils.Hash(target, Utils.KEY_BITS)).XML, HttpStatus.OK);
    }

    @RequestMapping(value = "/processing", method = RequestMethod.GET)
    public ResponseEntity<String> catchProcessRequest(
            @RequestParam(value = "fileName", required = false) String fileName) {
        StringBuilder page = new StringBuilder(1000);
        page.append("<!DOCTYPE html><html><head><meta name=\"viewport\" content=\"width=device-width\">");
        if (fileName == null || fileName.equals("")) {
            page.append("<title>SIM processing XML</title><link rel=\"stylesheet\" href=\"primary.css\"></head>");
            page.append("<body>");
            page.append("<div class=\"mainbox\">");
            page.append(
                    "<h2 class=\"txtcenter\">File Processed Information retriever</h2><p class=\"txtcenter\">Please enter your file name in to the box below and click on Submit button.</p><hr />");
            page.append(
                    "<form class=\"txtcenter\" style=\"margin-top: 30px;\" method=\"GET\" action=\"\"><input style=\"width: 180px; height: 14px\" placeholder=\"myfile.txt\" name=\"fileName\"><br /><input style=\"margin-top: 10px;\" type=\"submit\"></form>");
            page.append("</div>");
            page.append("</body>");
            page.append("</html>");
        } else {
            TaskCache file = App.getCachedTask(Utils.Hash(fileName, Utils.KEY_BITS));
            BinaryFileMeta meta = null;
            if (file == null) {
                try {
                    Node nodeWithFile = App.wellknownNode.findSuccessor(Utils.Hash(fileName, Utils.KEY_BITS));
                    meta = nodeWithFile.node.getFileMeta(fileName);
                    String xml = nodeWithFile.node.getProcessedXML(fileName);
                    if (xml != null)
                        App.startTaskCaching(Utils.Hash(fileName, Utils.KEY_BITS), xml);
                } catch (RemoteException e) {
                    page.append(
                            "<title>SIM processing XML</title><link rel=\"stylesheet\" href=\"primary.css\"></head>");
                    page.append("<body>");
                    page.append("<div class=\"mainbox\">");
                    page.append(
                            "<h2 class=\"txtcenter\">Request has failed to reach host!</h2><hr /><p class=\"txtcenter\">Try your request again...</p>");
                    page.append("</div>");
                    page.append("</body>");
                    page.append("</html>");
                    return new ResponseEntity<String>(page.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
            if (meta == null) {
                page.append("<title>SIM retriever</title><link rel=\"stylesheet\" href=\"primary.css\"></head>");
                page.append("<body>");
                page.append("<div class=\"mainbox\">");
                page.append("<h2 class=\"txtcenter\">File " + fileName
                        + " does not exist on the network!</h2><hr /><p class=\"txtcenter\">This could be due slow communication between hosts or your file simply has not been published yet.</p>");
                page.append("</div>");
                page.append("</body>");
                page.append("</html>");
                return new ResponseEntity<String>(page.toString(), HttpStatus.OK);
            }
            file = App.getCachedTask(Utils.Hash(fileName, Utils.KEY_BITS));
            if (file == null) {
                page.append("<title>SIM processing XML</title><link rel=\"stylesheet\" href=\"primary.css\"></head>");
                page.append("<body>");
                page.append("<div class=\"mainbox\">");
                page.append("<h2 class=\"txtcenter\">Task of file " + fileName
                        + " has not been processed yet!</h2><hr /><p class=\"txtcenter\">This is because there is a queue of tasks that needs to be processed.</p>");
                page.append("</div>");
                page.append("</body>");
                page.append("</html>");
            } else if (file.XML != null) {
                HttpHeaders hed = new HttpHeaders();
                hed.add("Location", App.address + "/XML?target=" + fileName);
                return new ResponseEntity<>(hed, HttpStatus.FOUND);
            }
        }

        return new ResponseEntity<String>(page.toString(), HttpStatus.OK);

    }

    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    public ResponseEntity<String> processFileSubmission(@RequestParam("target") MultipartFile file,
            @RequestParam("targetName") String filename, @RequestParam("process") boolean process) {
        if (!file.isEmpty()) {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        BinaryFileMeta meta = new BinaryFileMeta(filename, file.getSize());
                        byte[] bytes = file.getBytes();
                        Node correctNode = App.wellknownNode.findSuccessor(meta.getFileId());
                        correctNode.node.assumeFile(meta);
                        BinaryFileChunk[] chunks = BinaryFileChunk.computeFileChunks(meta, bytes);
                        for (BinaryFileChunk chunk : chunks) {
                            correctNode.node.transmitChunk(chunk); // more freedom for rest server
                        }
                        if (process) {
                            Task tsk = new Task(meta.getFileId(), true, true, true);
                            correctNode.node.processFile(tsk, filename);
                        }
                    } catch (Exception e) {
                    }
                }
            }).start();
            StringBuilder page = new StringBuilder(1000);
            page.append("<!DOCTYPE html><html><head><meta name=\"viewport\" content=\"width=device-width\">");
            page.append("<title>SIM upload</title><link rel=\"stylesheet\" href=\"primary.css\"></head>");
            page.append("<body>");
            page.append("<div class=\"mainbox txtcenter\">");
            page.append("<h2>File has been uploaded!</h2><hr />");
            page.append("<p>You will soon be redirected to file retrieval page...</p><p><a href=\"" + App.address
                    + "/retrieve?fileName=" + filename + "\">Click here to redirect now...</a></p>");
            page.append("</div>");
            page.append("<script>setTimeout(function(){window.location.href='" + App.address + "/retrieve?fileName="
                    + filename + "';}, 4000);</script>");
            page.append("</body>");
            page.append("</html>");
            return new ResponseEntity<String>(page.toString(), HttpStatus.OK);
        }
        HttpHeaders hed = new HttpHeaders();
        hed.add("Location", App.address + "/upload?error=true");
        return new ResponseEntity<String>(hed, HttpStatus.OK);
    }

    @RequestMapping(value = "/submitFile")
    public ResponseEntity<String> catchSubmission() {
        // may be prepare pages at some point
        // so you won't have to construct them on the go
        StringBuilder page = new StringBuilder(1000);
        page.append("<!DOCTYPE html><html><head><meta name=\"viewport\" content=\"width=device-width\">");
        page.append(
                "<title>SIM submission</title><link rel=\"stylesheet\" href=\"primary.css\"><link rel=\"stylesheet\" href=\"drop.css\"><script src=\"drop.js\"></script></head>");
        page.append("<body>");
        page.append(
                "<div class=\"mainbox txtcenter\" id=\"dropsite\" ondrop=\"dropHandler(event);\" ondragover=\"dragOverHandler(event);\" ondragleave=\"dragLeaveHandler(event);\">");
        page.append("<h2>File submission form</h2>");
        page.append(
                "<p>To submit a file select a file from below (or drag and drop it if your browser supports drag and drop).</p><hr />");
        page.append(
                "<button onClick=\"buttonSelectFile();\">Select a file</button><br /><p id=\"filenameText\">None</p>");
        page.append("<form method=\"post\" action=\"" + App.address
                + "/upload\" enctype=\"multipart/form-data\"><input class=\"secret\" type=\"text\" id=\"targetName\" name=\"targetName\"><input class=\"secret\" id=\"target\" type=\"file\" onchange=\"selectFileHandler();\" name=\"target\">");
        page.append("<input type=\"checkbox\" name=\"process\"> Process file<br /><input type=\"submit\"></form>");
        page.append("</div>");
        page.append("</body>");
        page.append("</html>");
        return new ResponseEntity<String>(page.toString(), HttpStatus.OK);
    }

    // f4k u spr1n9
    // @RequestMapping("/error")
    // public ResponseEntity<String> catchError() {
    // StringBuilder page = new StringBuilder(1000);
    // }

}