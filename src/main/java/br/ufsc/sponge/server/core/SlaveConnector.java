package br.ufsc.sponge.server.core;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;

import org.tinylog.Logger;

import br.ufsc.sponge.server.config.ServerConfiguration;
import br.ufsc.sponge.server.interfaces.IConnector;
import ch.jalu.configme.SettingsManager;
import io.activej.eventloop.Eventloop;
import io.activej.launcher.Launcher;
import io.activej.rpc.client.RpcClient;
import io.activej.rpc.client.sender.RpcStrategies;

public class SlaveConnector implements IConnector {
    // Properties
    private SettingsManager settings;
    private RpcClient masterNodeClient;
    private Eventloop eventLoop;
    private Thread socketThread;
    // Constructors
    public SlaveConnector(SettingsManager settings) throws IOException {
        Logger.info("Initializing");
        // Get Options
        this.settings = settings;
        // Create Loop
        this.eventLoop = Eventloop.create();
        this.socketThread = new Thread(this.eventLoop);
        // Instantiate 
        var masterHost = settings.getProperty(ServerConfiguration.SLAVEOPTS_HOST);
        var masterPort = settings.getProperty(ServerConfiguration.SLAVEOPTS_PORT);
        this.masterNodeClient = RpcClient
            .create(this.eventLoop)
            .withMessageTypes(String.class)
            .withStrategy(RpcStrategies.server(new InetSocketAddress(masterHost, masterPort)))
            .withConnectTimeout(Duration.ofSeconds(10));
        // Load Routes
        Logger.info("Initialized");
    }
    // Methods
    public void startListening() throws IOException, Exception {
        // Send start command to server
        this.socketThread.start();
        this.masterNodeClient.startFuture().get();
        System.out.println("Making Request");
        var currentPort = settings.getProperty(ServerConfiguration.INSTANCE_PORT);
        var hostName = InetAddress.getLocalHost().getHostAddress() + ":" + currentPort;
        var res = this.masterNodeClient.sendRequest(hostName).toCompletableFuture().get();
        System.out.println(res);
    }
    // Helpers
}