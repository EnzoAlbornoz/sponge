package br.ufsc.sponge.client;

// import java.io.IOException;
// import java.net.Authenticator;
// import java.net.InetSocketAddress;
// import java.net.ProxySelector;
// import java.net.URI;
// import java.net.URISyntaxException;
// import java.net.http.*;
// import java.net.http.HttpResponse.BodyHandlers;
// import java.time.Duration.*;

import ch.jalu.*;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class App {
    public static void main(String[] args) throws IOException {
        // Initialize from Settings
        // var unirest = Unirest.primaryInstance();
        
        File testFile = new File("test.txt");
        if (!testFile.exists()) {
            System.out.println("no such file");
        }

        var fileContent = Files.readAllBytes(testFile.toPath());

        var res = Unirest.post("http://localhost:9647")
            .body(fileContent)
            .asString();

        System.out.println(res.getBody());
        
        
        System.out.println("client out");
    }
}
