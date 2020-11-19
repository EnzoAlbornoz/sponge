package br.ufsc.sponge.server.core;

import br.ufsc.sponge.server.config.ServerConfiguration;
import br.ufsc.sponge.server.controllers.ClientController;
import br.ufsc.sponge.server.services.FileService;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import br.ufsc.sponge.server.repositories.*;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.*;

import ch.jalu.configme.SettingsManager;


public class ClientConnector {
    // Properties
    private SettingsManager settings;
    private HttpServer server;
    // Constructors
    public ClientConnector(SettingsManager settings) throws IOException {
        // Get Options
        this.settings = settings;
        // Instantiate Server
        this.server = HttpServer.create(
            new InetSocketAddress(
                settings.getProperty(ServerConfiguration.INSTANCE_PORT)
            ),
            100 // Max de Conexões que ele faz bufffer
        );
        // Load Routes
        this.registerRoutes();
    }
    // Methods
    public void startListening() {
        // Send start command to server
        this.server.start();
    }
    // Helpers
    private void registerRoutes() {
        // Assim --------------------------------------------------
        this.server.createContext("/", (HttpExchange ctx) -> {
            try {

                // List
                switch(ctx.getRequestMethod()) {
                    case "GET": 
                        // Buscar arquivo ou listar arquivos
                        // Check for id existance
                        if (ctx.getRequestURI().getPath().equals("/")) {
                            // Trying to list all files
                            System.out.println("Received get to LIST files");
                            ClientController.getInstance().getAllFiles(ctx);
                        } else {
                            // Trying to fetch an archive by id
                            System.out.println("Received get to " + ctx.getRequestURI().getPath());
                            ClientController.getInstance().getFile(ctx);
                        }
                        break;
                    case "POST":
                        // Cria um novo arquivo
                        System.out.println("Create File");
                        ClientController.getInstance().createFile(ctx);
                        break;
                    case "PUT":
                        // Substitui o conteudo de um arquivo
                        System.out.println("Update File");
                        ClientController.getInstance().updateFile(ctx);
                        break;
                    default:
                        // Non valid method
                        ctx.sendResponseHeaders(405, -1);
                        ctx.close();
                        break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
                ctx.sendResponseHeaders(500, -1);
            }
        });
    }

    // private

}
