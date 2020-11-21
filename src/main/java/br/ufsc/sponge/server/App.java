package br.ufsc.sponge.server;

import org.apache.commons.lang3.StringUtils;
import org.tinylog.Logger;

import ch.jalu.configme.SettingsManager;
import ch.jalu.configme.SettingsManagerBuilder;

import java.io.IOException;
import java.nio.file.*;

import br.ufsc.sponge.server.config.ServerConfiguration;
import br.ufsc.sponge.server.core.ClientConnector;
import br.ufsc.sponge.server.repositories.FileRepository;

/**
 * Hello world!
 *
 */
public class App {

    public static void main(String[] args) throws IOException {
        Logger.info("[Core] Starting application");
        // Initialize from Settings
        Logger.info("[Core] Loading settings");

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
            
        Logger.info("[Core] Settings loaded");
        Logger.info("[Core] Node type: {}", StringUtils.capitalize(settingsManager.getProperty(ServerConfiguration.INSTANCE_TYPE).toString()));
        // Initialize Repositories
        Logger.info("[Modules] Initializing modules");
        
        FileRepository.getInstance().initialize(settingsManager);
        final var clientConnector = new ClientConnector(settingsManager);
        
        Logger.info("[Modules] Modules loaded");
        Logger.info("[Core] Starting listening");
        
        clientConnector.startListening();
        
        Logger.info("[Core] Application ready to receive connections");
    }
}
