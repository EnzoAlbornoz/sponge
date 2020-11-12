package br.ufsc.sponge.server.controllers;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import com.sun.net.httpserver.*;

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
        files.forEach((file) -> { System.out.println(file.toString()); });
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
        // var answer = FileRepository.createFile("ctx.filename()", byte[]);
    }
}