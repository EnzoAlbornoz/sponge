package br.ufsc.sponge.server;

import ch.jalu.configme.SettingsManager;
import ch.jalu.configme.SettingsManagerBuilder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.util.Map;

import br.ufsc.sponge.server.config.ServerConfiguration;
import br.ufsc.sponge.server.core.ClientConnector;
// import com.sun.net.httpserver.HttpServer;
import br.ufsc.sponge.server.repositories.FileRepository;

/**
 * Hello world!
 *
 */
public class App {

    public static void main(String[] args) throws IOException {
        // Initialize from Settings
        final SettingsManager settingsManager = SettingsManagerBuilder
            .withYamlFile(
                Path.of(
                    "~/.sponge/config.yaml"
                    .replaceFirst("^~", System.getProperty("user.home"))
                )
            )
            .configurationData(ServerConfiguration.class)
            .useDefaultMigrationService()
            .create();

        System.out.println(settingsManager.getProperty(ServerConfiguration.INSTANCE_TYPE));
        // Initialize Repositories
        FileRepository.getInstance().initialize(settingsManager);
        final var clientConnector = new ClientConnector(settingsManager); 
        clientConnector.startListening();

        // setup or find main directory for storing crap
        // Path path = FileSystems.getDefault().getPath(System.getProperty("user.dir"), "storage");
        // System.out.println("curr dir" + path.toString());
        // if (Files.exists(path))
        // {
        //     System.out.println("existe");
        //     if (App.isDirEmpty(path)) {
        //         System.out.println("Ta vazio ");    
        //         // return
        //     } else {
        //         // fudeu consult slaves
        //     }
        // } else
        // {
        //     // First time: create the folder
        //     System.out.println("podre ta aq n, criando");
        //     Files.createDirectory(path);
        //     // return
        // }

        // var port = settingsManager.getProperty(ServerConfiguration.INSTANCE_PORT);
        // try {
        //     var server = HttpServer.create(new InetSocketAddress(port), 100);
        //     server.createContext("/", (HttpExchange exc) -> {
        //         System.out.println("Received Connection");
        //         var responseContent = "ALlAHU AKBAR".getBytes();
        //         var responseStream = exc.getResponseBody();
        //         exc.sendResponseHeaders(200, responseContent.length);
        //         responseStream.write(responseContent);
        //         responseStream.close();

        //     });
        //     System.out.println("Starting Listening");
        //     server.start();
        // } catch (IOException e) {
        //     // TODO Auto-generated catch block
        //     e.printStackTrace();
        // }
    }
    private static boolean isDirEmpty(final Path directory) throws IOException {
        try(DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory)) {
            return !dirStream.iterator().hasNext();
        }
    }
}
