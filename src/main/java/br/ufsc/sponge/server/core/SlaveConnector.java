package br.ufsc.sponge.server.core;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.http.client.utils.URIBuilder;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.tinylog.Logger;

import br.ufsc.sponge.server.config.ServerConfiguration;
import br.ufsc.sponge.server.controllers.ReplicationController;
import br.ufsc.sponge.server.interfaces.IConnector;
import ch.jalu.configme.SettingsManager;

public class SlaveConnector extends WebSocketClient implements IConnector {
    // Properties
    private SettingsManager settings;

    private SlaveConnector(URI address) {
        super(address);
    }

    public static SlaveConnector create(SettingsManager settings) throws URISyntaxException {
        Logger.info("Slave Initializing");
        // Get Options
        var host = settings.getProperty(ServerConfiguration.SLAVEOPTS_HOST);
        var port = settings.getProperty(ServerConfiguration.SLAVEOPTS_PORT);
        var uriAddress = new URIBuilder().setScheme("ws").setHost(host).setPort(port).build();
        var connector = new SlaveConnector(uriAddress);
        // Define Instance Properties
        connector.settings = settings;
        Logger.info("Slave Initialized");
        return connector;
    }

    @Override
    public void onClose(int arg0, String arg1, boolean arg2) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onError(Exception error) {
        // TODO Auto-generated method stub
        Logger.error(error);

    }

    @Override
    public void onMessage(String message) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onMessage(ByteBuffer buffer) {
        Message message = SerializationUtils.deserialize(buffer.array());
        switch(message.command) {
            case CREATE:
                ReplicationController.getInstance().create(this, message);
                break;
            case UPDATE:
                ReplicationController.getInstance().update(this, message);
                break;
            case DELETE:
                ReplicationController.getInstance().delete(this, message);
                break;
            default:
                break;
        }
    }

    @Override
    public void onOpen(ServerHandshake sHandshake) {
        // TODO Auto-generated method stub

    }

    @Override
    public void startListening() throws InterruptedException {
        Logger.info("[startListening] Connecting to Master");
        this.connectBlocking();
        Logger.info("[startListening] Connected");
    }
    
}
