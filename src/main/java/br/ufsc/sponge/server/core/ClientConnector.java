package br.ufsc.sponge.server.core;

import br.ufsc.sponge.server.config.ServerConfiguration;
import br.ufsc.sponge.server.controllers.ClientController;
import br.ufsc.sponge.server.interfaces.IConnector;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.*;

import org.tinylog.Logger;

import ch.jalu.configme.SettingsManager;


public class ClientConnector implements IConnector {
    // Properties
    private SettingsManager settings;
    private HttpServer server;
    // Constructors
    public ClientConnector(SettingsManager settings) throws IOException {
        Logger.info("Initializing");
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
        Logger.info("Initialized");
    }
    // Methods
    public void startListening() {
        // Send start command to server
        this.server.start();
    }
    // Helpers
    private void registerRoutes() {
        // Assim --------------------------------------------------
        Logger.info("Registering routes");
        this.server.createContext("/", (HttpExchange ctx) -> {
            try {
                Logger.info("{} on {}", ctx.getRequestMethod(), ctx.getRequestURI().getPath());
                // List
                switch(ctx.getRequestMethod()) {
                    case "GET": 
                        // Buscar arquivo ou listar arquivos
                        // Check for id existance
                        if (ctx.getRequestURI().getPath().equals("/")) {
                            // Trying to list all files
                            ClientController.getInstance().getAllFiles(ctx);
                        } else {
                            // Trying to fetch an archive by id
                            ClientController.getInstance().getFile(ctx);
                        }
                        break;
                    case "POST":
                        // Cria um novo arquivo
                        ClientController.getInstance().createFile(ctx);
                        break;
                    case "PUT":
                        // Substitui o conteudo de um arquivo
                        ClientController.getInstance().updateFile(ctx);
                        break;
                    case "DELETE":
                        // Deleta um arquivo
                        ClientController.getInstance().deleteFile(ctx);
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
        Logger.info("Routes registered");
    }
}
