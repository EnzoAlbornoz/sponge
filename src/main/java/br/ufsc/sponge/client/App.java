package br.ufsc.sponge.client;

import picocli.CommandLine;
import picocli.CommandLine.*;
import picocli.CommandLine.Model.CommandSpec;

import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONElement;
import kong.unirest.json.JSONObject;
@Command(name = "sponge-client", description = "Files Backup tool")
public class App implements Runnable {
    // Save CLI reference
    @Spec CommandSpec spec;
    // Global Options
    @Option(names = {"-H", "--host"}, description = "Hostname", defaultValue = "localhost", scope = ScopeType.INHERIT)
    String host;
    @Option(names = {"-P", "--port"}, description = "Port", defaultValue = "9647", scope = ScopeType.INHERIT)
    int port;
    // Commands
    @Command(
        name = "list",
        description = "List all files present on backup storage"
    )
    void list(
    ) {
        // Prepare Request
        var requestURI = "http://" + host + ":" + Integer.toString(port);
        // Execute Response
        var response = Unirest.get(requestURI).asJson();
        // Get File List
        var files = (JSONArray) response.getBody().getObject().get("files");
        var filesStrings = StreamSupport.stream(files.spliterator(), false).map(oFile -> {
            var file = (JSONObject) oFile;
            return new String[] {
                file.getString("id"),
                file.getString("size"),
                file.getString("name")
            };
        }).collect(Collectors.toList());
        // Compute Column Lengths
        var columnLengths = new HashMap<Integer, Integer>();
        for (int i = 0; i < filesStrings.size(); i++) {
            for (int j = 0; j < filesStrings.get(i).length; j++) {
                var val = Optional.ofNullable(columnLengths.putIfAbsent(j, 0)).orElse(0);
                var fscl = filesStrings.get(i)[j].length();
                if (val.intValue() < fscl) {
                    columnLengths.put(j, fscl);
                }

            }
        }
        // Define Pretty String
        final var formatString = new StringBuilder();
        columnLengths.forEach((_k, len) -> {
            formatString.append("%" + len.toString() + "s ");
        });
        formatString.append("\n");
        // Pretty Print File List
        for (var fileStrings : filesStrings) {
            // Print Info
            System.out.printf(formatString.toString(), (Object[]) fileStrings);
        }
    } 
    // Define Starting Point
    @Override
    public void run() {
        throw new ParameterException(spec.commandLine(), "Specify a subcommand");
        
    }

    public static void main(String[] args) throws IOException {
        // Initialize from Settings
        var exitCode = new CommandLine(new App()).execute(args);
        System.exit(exitCode);
    }
}
