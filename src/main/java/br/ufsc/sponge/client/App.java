package br.ufsc.sponge.client;

import picocli.CommandLine;
import picocli.CommandLine.*;
import picocli.CommandLine.Model.CommandSpec;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import java.util.Map;

import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import me.tongfei.progressbar.ProgressBar;

@Command(name = "sponge-client", description = "Files Backup tool")
public class App implements Runnable {
    // Save CLI reference
    @Spec
    CommandSpec spec;
    // Global Options
    @Option(names = { "-H", "--host" }, description = "Hostname", defaultValue = "localhost", scope = ScopeType.INHERIT)
    String host;
    @Option(names = { "-P", "--port" }, description = "Port", defaultValue = "9647", scope = ScopeType.INHERIT)
    int port;

    // Commands
    @Command(name = "list", description = "List all files present on backup storage")
    void list() {
        // Prepare Request
        var requestURI = "http://" + host + ":" + Integer.toString(port);
        // Execute Response
        var response = Unirest.get(requestURI).asJson();
        // Get File List
        var files = (JSONArray) response.getBody().getObject().get("files");
        var filesStrings = StreamSupport.stream(files.spliterator(), false).map(oFile -> {
            var file = (JSONObject) oFile;
            return new String[] { file.getString("id"), file.getString("size"), file.getString("name") };
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

    @Command(name = "get", description = "get the content of a particular file from the remote")
    void get(
        @Parameters(
            arity = "1",
            paramLabel = "<id of the file>",
            description = "must be file id, not name. Use List to get the Ids"
        ) String fileId
    ) throws FileNotFoundException, IOException {
        // Initialize Progress Bar
        var pBar = new ProgressBar(fileId, -1);
        // Prepare Request
        var requestURI = "http://" + host + ":" + Integer.toString(port) + "/" + fileId;
        var response = Unirest.get(requestURI).downloadMonitor((b, fileName, bytesWritten, totalBytes) -> {
            if (pBar.getMax() != totalBytes) {
                pBar.maxHint(totalBytes);
                pBar.setExtraMessage("Downloading...");
            }
            if (bytesWritten > pBar.getCurrent()) {
                pBar.stepTo(bytesWritten);
            }
        }).asFile("./" + fileId);
        // Close Progress Bar
        pBar.close();
        // Get Meta
        var file = response.getBody();
        var fName = response.getHeaders().getFirst("File-name");
        var fType = response.getHeaders().getFirst("Content-type");
        // Rename File
        file.renameTo(file.toPath().resolveSibling(fName).toFile());
        System.out.println("Succesfully downloaded " + fName + " of type " + fType);
    }
    @Command(name = "update", description = "Change the content of a particular file with the content of a local file")
    void update(
        @Parameters(
            arity = "1",
            paramLabel = "<target id>",
            description = "the target file id to update"
        ) String targetId,
        @Parameters(
            arity = "1",
            paramLabel = "<local file path>",
            description = "path to the file"
        ) String source
    ) throws Exception {
        // Prepare Request
        var requestURI = "http://" + host + ":" + Integer.toString(port) + "/" + targetId;
        var sourcePath = Path.of(source);
        Map<String, String> headerMap = Map.of(
            "FileName", sourcePath.getFileName().toString(),
            "Date", Long.toString(Instant.now().toEpochMilli())
        );
        // Make Request
        var responseBody = Unirest.put(requestURI)
            .headers(headerMap)
            .body(Files.readAllBytes(sourcePath))
            .asJson()
            .getBody();
        // Parse Reponse
        var fileJson = ((JSONObject) responseBody.getObject().get("file"));
        var id = fileJson.getString("id");
        var name = fileJson.getString("name");
        var size = fileJson.getString("size");
        var date = fileJson.getString("date");
        // Print Operation
        System.out.println("Updated to: \n" + 
                        "name: " + name + ", size: " + size + "\n" +
                        "date: " + Instant.parse(date) + "\nid: " + id);
    }
    @Command(name = "create", description = "create a file with the content of a local file")
    void create(
        @Parameters(
            arity = "1",
            paramLabel = "<local file path>",
            description = "path to the file"
        ) String source
    ) throws Exception {
        // Prepare Request
        var requestURI = "http://" + host + ":" + Integer.toString(port);
        var sourcePath = Path.of(source);
        Map<String, String> headerMap = Map.of(
            "FileName", sourcePath.getFileName().toString(),
            "Date", Long.toString(Instant.now().toEpochMilli())
        );
        // Make Request
        var responseBody = Unirest.post(requestURI)
            .headers(headerMap)
            .body(Files.readAllBytes(sourcePath))
            .asJson()
            .getBody();
        // Parse Reponse
        var fileJson = ((JSONObject) responseBody.getObject().get("file"));
        var id = fileJson.getString("id");
        var name = fileJson.getString("name");
        var size = fileJson.getString("size");
        var date = fileJson.getString("date");
        // Print Operation
        System.out.println("Pushed file: \n" + 
                        "name: " + name + ", size: " + size + "\n" +
                        "date: " + Instant.parse(date) + "\nid: " + id);
    }
    @Command(name = "delete", description = "delete a file")
    void delete(
        @Parameters(
            arity = "1",
            paramLabel = "<target id>",
            description = "the target file id to delete"
        ) String targetId
    ) throws Exception {
        // Prepare Request
        var requestURI = "http://" + host + ":" + Integer.toString(port) + "/" + targetId;
        var response = Unirest.delete(requestURI).asEmpty();
        if (response.isSuccess()) {
            System.out.println("File sucessfully deleted");
        } else {
            System.out.println("File does not exists");
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
