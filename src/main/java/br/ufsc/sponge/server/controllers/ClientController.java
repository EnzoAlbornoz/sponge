package br.ufsc.sponge.server.controllers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLConnection;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.net.httpserver.*;

import org.easygson.JsonEntity;
import org.tinylog.Logger;

import br.ufsc.sponge.server.repositories.FileRepository;
import br.ufsc.sponge.server.repositories.ReplicationRepository;
import br.ufsc.sponge.server.repositories.VirtualFile;

/**
 * ClientController
 */
public class ClientController {
    // Singleton Overhead
    private static Optional<ClientController> instance = Optional.empty();

    private ClientController() {
    }

    public static ClientController getInstance() {
        if (instance.isEmpty()) {
            instance = Optional.of(new ClientController());
        }
        return instance.get();
    }

    // Methods
    public void getAllFiles(HttpExchange ctx) throws IOException {
        Logger.info("[getAllFiles] Searching for files");
        // List files
        var files = FileRepository.getInstance().listFiles();
        // Print Log
        Logger.info("[getAllFiles] {} files found", files.size());
        // Serialize File Info
        Logger.info("[getAllFiles] Serializing response content");
        var jcontent = JsonEntity.emptyObject().createArray("files");
        for (VirtualFile file : files) {
            jcontent = jcontent.createObject().create("id", file.getId()).create("name", file.getName())
                    .create("size", file.getSize())
                    .create("date", ZonedDateTime.ofInstant(file.getDate().toInstant(), ZoneId.systemDefault())
                            .format(DateTimeFormatter.ISO_INSTANT))
                    .parent();
        }
        jcontent = jcontent.parent();
        var ffiledJson = jcontent.toString().getBytes();
        // Send Headers
        // ===============
        Logger.info("[getAllFiles] Sending response");
        var headers = ctx.getResponseHeaders();
        headers.add("content-type", "application/json");
        ctx.sendResponseHeaders(200, ffiledJson.length);
        // Send Response
        var response = ctx.getResponseBody();
        response.write(ffiledJson);
        response.close();
    }

    public void createFile(HttpExchange ctx) throws IOException {
        Logger.info("[createFile] Creating file");
        var headers = ctx.getRequestHeaders();
        var reqBody = ctx.getRequestBody();
        // Get Parameters
        String hFileName = headers.getFirst("FileName");
        // Get Binary Content
        byte[] bContent = reqBody.readAllBytes();
        // if (hFilename already exists) {
        // throw new IOException();
        // }
        Logger.info("[createFile] [Meta] Size: {} Name: {}", bContent.length, hFileName);
        var createdFile = FileRepository.getInstance().createFile(hFileName, Instant.now().toEpochMilli(), bContent);
        Logger.info("[createFile] File {} created", createdFile.toFileNameHash());
        // Replicate ===========================================================
        var success = ReplicationRepository.getInstance().replicateFileCreation(createdFile);
        // =====================================================================
        if (success) {
            // Serialize File Info
            Logger.info("[createFile] Serializing response content");
            var jcontent = JsonEntity.emptyObject().createObject("file").create("id", createdFile.getId())
                    .create("name", createdFile.getName()).create("size", createdFile.getSize())
                    .create("date", ZonedDateTime.ofInstant(createdFile.getDate().toInstant(), ZoneId.systemDefault())
                            .format(DateTimeFormatter.ISO_INSTANT))
                    .parent();
            var ffiledJson = jcontent.toString().getBytes();
            // Send Headers
            Logger.info("[createFile] Sending response");
            var resHeaders = ctx.getResponseHeaders();
            resHeaders.add("content-type", "application/json");
            ctx.sendResponseHeaders(200, ffiledJson.length);
            // Send Response
            var response = ctx.getResponseBody();
            response.write(ffiledJson);
            response.close();
        } else {
            // Rollback
            Logger.info("[createFile] Error on replication");
            Logger.info("[createFile] Rollbacking creation");
            FileRepository.getInstance().deleteFile(createdFile.getId());
            Logger.info("[createFile] Rollback complete");
            Logger.info("[createFile] Sending response");
            ctx.sendResponseHeaders(500, -1);
        }
    }

    public void getFile(HttpExchange ctx) throws IOException {
        // Make Regex over URL to get the searched file Id
        final Matcher idMatcher = Pattern.compile("^/(?<id>[a-zA-Z_\\-0-9]+)").matcher(ctx.getRequestURI().getPath());
        if (idMatcher.matches()) {
            // Valid URL
            var fId = idMatcher.group("id");
            Logger.info("[getFile] Searching for file {}", fId);
            // Fetch File
            var optVFile = FileRepository.getInstance().getFile(fId);
            if (optVFile.isPresent()) {
                Logger.info("[getFile] File found");
                var vFile = optVFile.get();
                // Guess mime type
                var vfMimeType = URLConnection.guessContentTypeFromName(vFile.getName());
                if (vfMimeType == null) {
                    vfMimeType = URLConnection
                            .guessContentTypeFromStream(new ByteArrayInputStream(vFile.getContent().get()));
                }
                // Define Response Headers
                Logger.info("[getFile] Sending response");
                var resHeaders = ctx.getResponseHeaders();
                resHeaders.add("content-type", vfMimeType == null ? "application/octet-stream" : vfMimeType);
                resHeaders.add("file-name", vFile.getName());
                ctx.sendResponseHeaders(200, vFile.getContent().get().length);
                // Send File to Requester
                var response = ctx.getResponseBody();
                response.write(vFile.getContent().get());
            } else {
                Logger.info("[getFile] File not found");
                Logger.info("[getFile] Sending response");
                // Send Response
                ctx.sendResponseHeaders(404, -1);
            }
        } else {
            Logger.info("[getFile] Received invalid Id");
            ctx.sendResponseHeaders(400, -1);
        }
    }

    public void updateFile(HttpExchange ctx) throws IOException {
        // Make Regex over URL to get the searched file Id
        final Matcher idMatcher = Pattern.compile("^/(?<id>[a-zA-Z_\\-0-9]+)").matcher(ctx.getRequestURI().getPath());
        if (idMatcher.matches()) {
            // Valid URL
            var fId = idMatcher.group("id");
            Logger.info("[updateFile] Searching for file {}", fId);
            // Fetch File
            var optVFile = FileRepository.getInstance().getFile(fId);
            if (optVFile.isPresent()) {
                var vFile = optVFile.get();
                var oFile = vFile.clone(vFile);
                Logger.info("[updateFile] File found");
                // Fetch Context Data
                var headers = ctx.getRequestHeaders();
                var reqBody = ctx.getRequestBody();
                // Update Steps ==============
                // ==== FileName
                var hFileName = headers.getFirst("FileName");
                if (hFileName != null) {
                    Logger.info("[updateFile] Updating file name");
                    vFile.setName(hFileName);
                }
                // ====
                // Get Binary Content
                var contentLength = Integer.parseInt(headers.getFirst("Content-length"));
                if (contentLength > 0) {
                    Logger.info("[updateFile] Updating file content");
                    byte[] bContent = reqBody.readAllBytes();
                    vFile.setContent(bContent);
                }
                // Save Updates
                Logger.info("[updateFile] Saving file changes");
                FileRepository.getInstance().updateFile(oFile, vFile);
                Logger.info("[updateFile] File saved");
                // Replicate ===========================================================
                Logger.info("[updateFile] Replicating update");
                var success = ReplicationRepository.getInstance().replicateFileUpdate(vFile);
                // =====================================================================
                if (success) {
                    // Serialize File Info
                    Logger.info("[updateFile] Serializing response content");
                    var jcontent = JsonEntity.emptyObject().createObject("file").create("id", vFile.getId())
                            .create("name", vFile.getName()).create("size", vFile.getSize())
                            .create("date", ZonedDateTime.ofInstant(vFile.getDate().toInstant(), ZoneId.systemDefault())
                                    .format(DateTimeFormatter.ISO_INSTANT))
                            .parent();
                    var ffiledJson = jcontent.toString().getBytes();
                    // Send Headers
                    Logger.info("[updateFile] Sending response");
                    var resHeaders = ctx.getResponseHeaders();
                    resHeaders.add("content-type", "application/json");
                    ctx.sendResponseHeaders(200, ffiledJson.length);
                    // Send Response
                    var response = ctx.getResponseBody();
                    response.write(ffiledJson);
                    response.close();
                } else {
                    // Rollback
                    Logger.info("[updateFile] Error on replication");
                    Logger.info("[updateFile] Rollbacking update");
                    FileRepository.getInstance().updateFile(vFile, oFile);
                    Logger.info("[updateFile] Rollback complete");
                    Logger.info("[updateFile] Sending response");
                    ctx.sendResponseHeaders(500, -1);
                }
            } else {
                Logger.info("[getFile] File not found");
                Logger.info("[getFile] Sending response");
                // Send Response
                ctx.sendResponseHeaders(404, -1);
            }
        } else {
            Logger.info("[updateFile] Received invalid Id");
            ctx.sendResponseHeaders(400, -1);
        }
    }

    public void deleteFile(HttpExchange ctx) throws UnsupportedEncodingException, IOException {
        final Matcher idMatcher = Pattern
            .compile("^/(?<id>[a-zA-Z_\\-0-9]+)")
            .matcher(ctx.getRequestURI().getPath());
        if (idMatcher.matches()) {
            // Valid URL
            var fId = idMatcher.group("id");
            Logger.info("[deleteFile] Trying to delete file {}", fId);
            var optVFile = FileRepository.getInstance().deleteFile(fId);
            if (optVFile.isPresent()) {
                // Removed File
                Logger.info("[deleteFile] File {} removed", fId);
                // Replicate ===========================================================
                var success = ReplicationRepository.getInstance().replicateFileDeletion(optVFile.get());
                // =====================================================================
                if (success) {
                    Logger.info("[deleteFile] Sending response");
                    ctx.sendResponseHeaders(200, -1);
                } else {
                    // Rollback
                    Logger.info("[deleteFile] Error on replication");
                    Logger.info("[deleteFile] Rollbacking delete");
                    FileRepository.getInstance().createFileSlave(optVFile.get());
                    Logger.info("[deleteFile] Rollback complete");
                    Logger.info("[deleteFile] Sending response");
                    ctx.sendResponseHeaders(500, -1);
                }
            } else {
                // File not found
                Logger.info("[deleteFile] File does not exists", fId);
                ctx.sendResponseHeaders(404, -1);
            }
        } else {
            Logger.info("[deleteFile] Invalid Id");
            ctx.sendResponseHeaders(404, -1);
        }
    }
}