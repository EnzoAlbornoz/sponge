package br.ufsc.sponge.server.core;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.tinylog.Logger;

import br.ufsc.sponge.server.config.ServerConfiguration;
import br.ufsc.sponge.server.interfaces.IConnector;
import ch.jalu.configme.SettingsManager;
import io.activej.eventloop.Eventloop;
import io.activej.promise.Promise;
import io.activej.rpc.server.RpcServer;

public class MasterConnector implements IConnector {
    // Properties
    private SettingsManager settings;
    private RpcServer server;
    private Eventloop eventLoop;
    private Thread socketThread;
    // Constructors
    public MasterConnector(SettingsManager settings) throws IOException {
        Logger.info("Initializing");
        // Get Options
        this.settings = settings;
        // Create Loop
        this.eventLoop = Eventloop.create();
        this.socketThread = new Thread(this.eventLoop); 
        // Instantiate Server
        var port = settings.getProperty(ServerConfiguration.MASTEROPTS_PORT);
        this.server = RpcServer
            .create(this.eventLoop)
            .withListenPort(port);
        this.registerActions();
        // Load Routes
        Logger.info("Initialized");
    }
    // Methods
    public void startListening() throws IOException {
        // Send start command to server
        this.server.listen();
        this.socketThread.start();
    }
    // Helpers
    private void registerActions() {
        Logger.info("Registering actions");
        this.server = this.server
            // Register Actions
            .withMessageTypes(String.class)
            // Register Handlers for Actions
            .withHandler(String.class, address -> {
                Logger.info(address);
                var sAddr = address.split(":");
                var host = sAddr[0];
                var port = Integer.parseInt(sAddr[1]);
                var socketAddr = new InetSocketAddress(host, port);
                Logger.info(socketAddr.getAddress() + " PONG!");
                return Promise.of("vapo");
            });
    }
}
    
