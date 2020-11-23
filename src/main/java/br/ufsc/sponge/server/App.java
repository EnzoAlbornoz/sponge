package br.ufsc.sponge.server;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import org.tinylog.Logger;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.function.Failable;

import ch.jalu.configme.SettingsManager;
import ch.jalu.configme.SettingsManagerBuilder;

import java.io.IOException;
import java.nio.file.*;
import java.util.Optional;
import java.util.concurrent.Callable;

import br.ufsc.sponge.server.config.ServerConfiguration;
import br.ufsc.sponge.server.config.ServerType;
import br.ufsc.sponge.server.core.ClientConnector;
import br.ufsc.sponge.server.core.MasterConnector;
import br.ufsc.sponge.server.core.SlaveConnector;
import br.ufsc.sponge.server.interfaces.IConnector;
import br.ufsc.sponge.server.repositories.FileRepository;
import br.ufsc.sponge.server.repositories.ReplicationRepository;

@Command(
    name = "sponge",
    description = "File backup tool"
)
public class App implements Callable<Integer> {

    @Option(
        names = { "-c", "--config" },
        description = "use an alternative configuration file",
        paramLabel = "<config file path>"
    )
    private String configPath = Path.of(System.getProperty("user.home")).resolve("./.sponge/storage").toString();

    @Override
    public Integer call() throws Exception {
        Logger.info("[Core] Starting application");
        // Initialize from Settings
        Logger.info("[Core] Loading settings");
    
        final SettingsManager settingsManager = SettingsManagerBuilder
            .withYamlFile(Path.of(configPath))
            .configurationData(ServerConfiguration.class)
            .useDefaultMigrationService()
            .create();
        final var isMasterNode = settingsManager.getProperty(ServerConfiguration.INSTANCE_TYPE) == ServerType.MASTER;
    
        Logger.info("[Core] Settings loaded");
        Logger.info("[Core] Node type: {}", StringUtils.capitalize(settingsManager.getProperty(ServerConfiguration.INSTANCE_TYPE).toString()));
        // Initialize Repositories
        Logger.info("[Modules] Initializing modules");

        FileRepository.getInstance().initialize(settingsManager);
        if (isMasterNode) {
            ReplicationRepository.getInstance().initialize(settingsManager);
        }

        Optional<IConnector> clientConnector = Optional.empty();
        Optional<IConnector> nodeConnector = Optional.empty();
        if (isMasterNode) {
            // Instantiate Master Connectors
            clientConnector = Optional.of(new ClientConnector(settingsManager));
            nodeConnector = Optional.of(MasterConnector.create(settingsManager));
            // Update Repositories
            ReplicationRepository.getInstance().registerConnector((MasterConnector) nodeConnector.get());
        } else {
            // Instantiate Master Connectors
            nodeConnector = Optional.of(SlaveConnector.create(settingsManager));
        }

        Logger.info("[Modules] Modules loaded");
        Logger.info("[Core] Starting listening");

        clientConnector.ifPresent(Failable.asConsumer((node) -> node.startListening()));
        nodeConnector.ifPresent(Failable.asConsumer((node) -> node.startListening()));

        Logger.info("[Core] Application ready to receive connections");
        
        return 0;
    }
    public static void main(String[] args) throws IOException {
        new CommandLine(new App()).execute(args);
    }
}
