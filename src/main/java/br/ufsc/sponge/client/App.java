// package br.ufsc.sponge.client;

// // import java.io.IOException;
// // import java.net.Authenticator;
// // import java.net.InetSocketAddress;
// // import java.net.ProxySelector;
// // import java.net.URI;
// // import java.net.URISyntaxException;
// // import java.net.http.*;
// // import java.net.http.HttpResponse.BodyHandlers;
// // import java.time.Duration.*;

// import ch.jalu.*;
// import kong.unirest.HttpResponse;
// import kong.unirest.JsonNode;
// import kong.unirest.Unirest;
// import java.io.File;
// import java.io.IOException;
// import java.nio.file.Files;
// import io.airlift.airline.*;
// import io.airlift.airline.Cli.CliBuilder;
// import io.airlift.airline.model.*;
// public class App {
//     public static void main(String[] args) throws IOException {
//         // Initialize from Settings
//         // var unirest = Unirest.primaryInstance();
        
//         File testFile = new File("test.txt");
//         if (!testFile.exists()) {
//             System.out.println("no such file");
//         }
        
//         CliBuilder<Runnable> builder = Cli.<Runnable>builder("sponge")
//             .withDescription("Proof of concept de Replicação Passiva")
//             .withDefaultCommand(Help.class)
//             .withCommands(Help.class, Add.class);
        
//         builder.withGroup("cu podre")
//             .withDescription("gay");

//         var spongeClient = builder.build();
//         spongeClient.parse(args).run();

//         var fileContent = Files.readAllBytes(testFile.toPath());

//         var res = Unirest.post("http://localhost:9647")
//             .body(fileContent)
//             .asString();

//         System.out.println(res.getBody());
        
        
//         System.out.println("client out");
//     }
// }
