package br.ufsc.sponge.server.controllers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.net.httpserver.*;

import org.apache.commons.lang3.ArrayUtils;
import org.easygson.JsonEntity;

import br.ufsc.sponge.server.repositories.FileRepository;
import br.ufsc.sponge.server.repositories.VirtualFile;

/**
 * ClientController
 */
public class ClientController {
    // Singleton Overhead
    private static Optional<ClientController> instance = Optional.empty();

    private ClientController() {}

    public static ClientController getInstance() {
        if (instance.isEmpty()) { instance = Optional.of(new ClientController()); }
        return instance.get();
    }

    // Methods
    public void getAllFiles(HttpExchange ctx) throws IOException {
        // List files
        var files = FileRepository.getInstance().listFiles();
        // Print Log
        // files.forEach((file) -> { System.out.println(file.toString()); });
        // Serialize File Info
        var jcontent = JsonEntity.emptyObject().createArray("files");
        for (VirtualFile file : files) {
            jcontent = jcontent.createObject()
                .create("id", file.getId())
                .create("name",file.getName())
                .create("size", file.getSize())
                .create("date", ZonedDateTime
                    .ofInstant(file.getDate().toInstant(), ZoneId.systemDefault())
                    .format(DateTimeFormatter.ISO_INSTANT)
                )
                .parent();
        }
        jcontent = jcontent.parent();
        var ffiledJson = jcontent.toString().getBytes();
        // Send Headers
        var headers = ctx.getResponseHeaders();
        headers.add("content-type", "application/json");
        ctx.sendResponseHeaders(200, ffiledJson.length);
        // Send Response
        var response = ctx.getResponseBody();
        response.write(ffiledJson);
        response.close();
    }

    public void createFile(HttpExchange ctx) throws IOException {
        var headers = ctx.getRequestHeaders();
        var reqBody = ctx.getRequestBody();
        // Get Parameters
        String hFileName = headers.getFirst("FileName");
        // Get Binary Content
        byte[] bContent = reqBody.readAllBytes();
        
        // if (hFilename already exists) {
        //     throw new IOException();
        // }
        var createdFile = FileRepository.getInstance().createFile(hFileName, Instant.now().toEpochMilli(), bContent);
        // Serialize File Info
        var jcontent = JsonEntity
            .emptyObject()
            .createObject("file")
                .create("id", createdFile.getId())
                .create("name",createdFile.getName())
                .create("size", createdFile.getSize())
                .create("date", ZonedDateTime
                    .ofInstant(createdFile.getDate().toInstant(), ZoneId.systemDefault())
                    .format(DateTimeFormatter.ISO_INSTANT)
                )
            .parent();
        var ffiledJson = jcontent.toString().getBytes();
        // Send Headers
        var resHeaders = ctx.getResponseHeaders();
        resHeaders.add("content-type", "application/json");
        ctx.sendResponseHeaders(200, ffiledJson.length);
        // Send Response
        var response = ctx.getResponseBody();
        response.write(ffiledJson);
        response.close();
    }

    public void getFile(HttpExchange ctx) throws IOException {
        // Make Regex over URL to get the searched file Id
        final Matcher idMatcher = Pattern
            .compile("^/(?<id>[a-zA-Z_\\-0-9]+)")
            .matcher(ctx.getRequestURI().getPath());
        if (idMatcher.matches()) {
            // Valid URL
            var fId = idMatcher.group("id");
            System.out.println(fId);
            // Fetch File
            var vFile = FileRepository.getInstance().getFile(fId);
            // Guess mime type
            var vfMimeType = URLConnection.guessContentTypeFromName(vFile.getName());
            if (vfMimeType == null) {
                vfMimeType = URLConnection.guessContentTypeFromStream(
                    new ByteArrayInputStream(
                       vFile.getContent().get()
                    )
                );
            }
            // Define Response Headers
            var resHeaders = ctx.getResponseHeaders();
            resHeaders.add("content-type", vfMimeType == null ? "application/octet-stream" : vfMimeType);
            ctx.sendResponseHeaders(200, vFile.getContent().get().length);
            // Send File to Requester
            var response = ctx.getResponseBody();
            response.write(vFile.getContent().get());
        }
        else {
            ctx.sendResponseHeaders(400, 0);
        }
    }
    public void updateFile(HttpExchange ctx) throws IOException {
        // Make Regex over URL to get the searched file Id
        final Matcher idMatcher = Pattern
            .compile("^/(?<id>[a-zA-Z_\\-0-9]+)")
            .matcher(ctx.getRequestURI().getPath());
        if (idMatcher.matches()) {
            // Valid URL
            var fId = idMatcher.group("id");
            // Fetch File
            var cFile = FileRepository.getInstance().getFile(fId);
            var vFile = cFile.clone();
            // Fetch Context Data
            var headers = ctx.getRequestHeaders();
            var reqBody = ctx.getRequestBody();
            // Update Steps ==============
            // ==== FileName
            var hFileName = headers.getFirst("FileName");
            if (hFileName != null) {
                vFile.setName(hFileName);
            }
            // ==== 
            // Get Binary Content
            var contentLength = Integer.parseInt(headers.getFirst("Content-length"));
            if (contentLength > 0) {
                byte[] bContent = reqBody.readAllBytes();
                vFile.setContent(bContent);
            }
            // Save Updates
            FileRepository.getInstance().updateFile(vFile, cFile);
            // Serialize File Info
            var jcontent = JsonEntity
                .emptyObject()
                .createObject("file")
                    .create("id", vFile.getId())
                    .create("name",vFile.getName())
                    .create("size", vFile.getSize())
                    .create("date", ZonedDateTime
                        .ofInstant(vFile.getDate().toInstant(), ZoneId.systemDefault())
                        .format(DateTimeFormatter.ISO_INSTANT)
                    )
                .parent();
            var ffiledJson = jcontent.toString().getBytes();
            // Send Headers
            var resHeaders = ctx.getResponseHeaders();
            resHeaders.add("content-type", "application/json");
            ctx.sendResponseHeaders(200, ffiledJson.length);
            // Send Response
            var response = ctx.getResponseBody();
            response.write(ffiledJson);
            response.close();
        }
        else {
            ctx.sendResponseHeaders(400, 0);
        }
    }
}