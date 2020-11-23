package br.ufsc.sponge.server.core;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.function.Failable;
import org.apache.commons.lang3.tuple.Pair;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.tinylog.Logger;

import br.ufsc.sponge.server.config.ServerConfiguration;
import br.ufsc.sponge.server.interfaces.IConnector;
import ch.jalu.configme.SettingsManager;

public class MasterConnector extends WebSocketServer implements IConnector {
    // Properties
    private SettingsManager settings;
    private Optional<HashMap<InetSocketAddress, CompletableFuture<Boolean>>> broadcastMap = Optional.empty();

    private MasterConnector(InetSocketAddress address) {
        super(address);
    }

    public static MasterConnector create(SettingsManager settings) {
        Logger.info("Initializing");
        // Get Options
        var port = settings.getProperty(ServerConfiguration.MASTEROPTS_PORT);
        var connector = new MasterConnector(new InetSocketAddress(port));
        // Define Instance Properties
        connector.settings = settings;
        // Load Routes
        Logger.info("Initialized");
        return connector;
    }

    @Override
    public void onClose(WebSocket socket, int arg1, String arg2, boolean arg3) {
        broadcastMap.ifPresent(bmap -> {
            bmap.get(socket.getRemoteSocketAddress()).complete(false);
        });
    }

    @Override
    public void onError(WebSocket socket, Exception exception) {
        if (socket != null) {
            broadcastMap.ifPresent(bmap -> {
                bmap.get(socket.getRemoteSocketAddress()).complete(false);
            });
        }
    }

    @Override
    public void onMessage(WebSocket socket, String message) {
        // Mark Message as Resolved
        var messageResult = Boolean.parseBoolean(message);
        broadcastMap.ifPresentOrElse(bmap -> {
            Logger.info("[onMessage] Received status {} from {}", messageResult, socket.getRemoteSocketAddress());
            bmap.get(socket.getRemoteSocketAddress()).complete(messageResult);
        }, () -> Logger.error("[onMessage] Received invalid message from {}", socket.getRemoteSocketAddress()));

    }

    @Override
    public void onOpen(WebSocket socket, ClientHandshake arg1) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onStart() {
        // TODO Auto-generated method stub
    }

    public CompletableFuture<List<Pair<InetSocketAddress, Boolean>>> broadcast(Message message) {
        // Serialize
        Logger.info("[broadcast] Serializing message");
        var messageBytes = SerializationUtils.serialize(message);
        Logger.info("[broadcast] Serialized message with {} bytes", messageBytes.length);
        // Generate Broadcast Hash
        var broadcastResultMap = new HashMap<InetSocketAddress, CompletableFuture<Boolean>>();
        Logger.info("[broadcast] Creating futures");
        this.getConnections().forEach(conn -> {
            var connId = conn.getRemoteSocketAddress();
            var messageResult = new CompletableFuture<Boolean>();
            // Send Message
            Logger.info("[broadcast] Sending message to {}", connId);
            conn.send(messageBytes);
            // Register on Result Map
            broadcastResultMap.put(connId, messageResult);
        });
        // Register Result Map on Instance
        broadcastMap = Optional.of(broadcastResultMap);
        // Wait for Completititititition
        var result = CompletableFuture.allOf(broadcastResultMap.values().toArray(CompletableFuture[]::new))
                .thenApply(Failable.asFunction((_void) -> {
                    Logger.info("[broadcast] Processing results");
                    broadcastMap = Optional.empty();
                    // CompletableFuture<Boolean>[] rFutures =
                    // broadcastResultMap.values().toArray(CompletableFuture[]::new);
                    // var res = new Boolean[rFutures.length];
                    // for (int i = 0; i < rFutures.length; i++) {
                    // res[i] = rFutures[i].get();
                    // }
                    // return res;
                // return res;
                return (
                        broadcastResultMap.entrySet().stream()
                        .map(
                            Failable.asFunction(
                                (entry) -> Pair.of(entry.getKey(), entry.getValue().get())
                            )
                        ).collect(Collectors.toList())
                );
            }));
        return result;
    }

    public CompletableFuture<List<Pair<InetSocketAddress, Boolean>>> broadcast(List<InetSocketAddress> socketAddresses, Message message) {
        // Serialize
        Logger.info("[broadcast] Serializing message");
        var messageBytes = SerializationUtils.serialize(message);
        Logger.info("[broadcast] Serialized message with {} bytes", messageBytes.length);
        // Generate Broadcast Hash
        var broadcastResultMap = new HashMap<InetSocketAddress, CompletableFuture<Boolean>>();
        Logger.info("[broadcast] Creating futures");
        this.getConnections().forEach(conn -> {
            var connId = conn.getRemoteSocketAddress();
            if (socketAddresses.contains(connId)) {
                var messageResult = new CompletableFuture<Boolean>();
                // Send Message
                Logger.info("[broadcast] Sending message to {}", connId);
                conn.send(messageBytes);
                // Register on Result Map
                broadcastResultMap.put(connId, messageResult);
            }
        });
        // Register Result Map on Instance
        broadcastMap = Optional.of(broadcastResultMap);
        // Wait for Completititititition
        var result = CompletableFuture.allOf(broadcastResultMap.values().toArray(CompletableFuture[]::new))
                .thenApply(Failable.asFunction((_void) -> {
                    Logger.info("[broadcast] Processing results");
                    broadcastMap = Optional.empty();
                    // CompletableFuture<Boolean>[] rFutures =
                    // broadcastResultMap.values().toArray(CompletableFuture[]::new);
                    // var res = new Boolean[rFutures.length];
                    // for (int i = 0; i < rFutures.length; i++) {
                    // res[i] = rFutures[i].get();
                    // }
                    // return res;
                // return res;
                return (
                        broadcastResultMap.entrySet().stream()
                        .map(
                            Failable.asFunction(
                                (entry) -> Pair.of(entry.getKey(), entry.getValue().get())
                            )
                        ).collect(Collectors.toList())
                );
            }));
        return result;
    }

    
    @Override
    public void startListening() {
        this.run();
    }
    
}
