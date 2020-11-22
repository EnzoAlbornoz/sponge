package br.ufsc.sponge.server.repositories;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.tinylog.Logger;

import br.ufsc.sponge.server.config.ServerConfiguration;
import ch.jalu.configme.SettingsManager;

public class FileRepository {

    
    private FileRepository() {
        cachedFiles = new HashMap<String, VirtualFile>();
    };

    private boolean initialized = false;
    private SettingsManager settings;
    private HashMap<String, VirtualFile> cachedFiles;
    
    private static FileRepository single_instance = null;

    public static FileRepository getInstance() {
        if (single_instance == null)
            single_instance = new FileRepository();
        return single_instance;
    }

    private Path getSharedFolderPath() {
        var sharedFolderLocation = settings.getProperty(ServerConfiguration.INSTANCE_SHARED_FOLDER)
            .replaceFirst("^~", System.getProperty("user.home"));
        return Paths.get(sharedFolderLocation).toAbsolutePath().normalize();
    }

    public void initialize(SettingsManager settings) throws IOException {
        Logger.info("Initializing");
        // Initialize Settings
        this.settings = settings;
        // Initialize Files Cache
        var sFolderPath = this.getSharedFolderPath();
        Logger.info("Searching for shared folder {}", sFolderPath.toString());
        if (this.isStoragePresent()) {
            Logger.info("Shared folder exists");
            Logger.info("[Files] Loading files");
            // Load file paths
            var filePaths = Files.newDirectoryStream(sFolderPath);
            // Process files
            for (Path filePath : filePaths) {
                // Read File Content
                var physicalFile = filePath.toFile();
                Logger.info("[File] Found {}", physicalFile.getName());
                // Transforms it into consumable file
                var vFile = new VirtualFile(physicalFile);
                // Add vfile to cache
                this.cachedFiles.put(vFile.getId(), vFile);
            }
            Logger.info("[Files] All files loaded");
        } else {
            // Create folder
            Logger.info("Shared folder not found");
            Logger.info("Creating new shared folder");
            Files.createDirectories(sFolderPath);
            Logger.info("Shared folder created");
        }
        // Mark as Initialized
        this.initialized = true;
        Logger.info("Initialized");
    }

    public List<VirtualFile> listFiles() throws IOException {
        return new ArrayList<VirtualFile>(this.cachedFiles.values());
    }

    public VirtualFile createFile(String name, Long date, byte[] content) throws IOException {
        VirtualFile newFile = new VirtualFile(name, date, content);
        Path sharedFolderPath = this.getSharedFolderPath();
        try {
            // Start Transaction
            this.cachedFiles.put(newFile.getId(), newFile);
            // Actually write file to the FS
            var fileWriter = new FileOutputStream(sharedFolderPath.resolve(newFile.toFileNameHash()).toFile());
            fileWriter.write(newFile.getContent().get());
            fileWriter.close();
            // End Transaction
            return newFile;
        } catch (Exception e) {
            // Rollback
            this.cachedFiles.remove(newFile.getId());
            throw e;
        }
    }

    public Optional<VirtualFile> getFile(String fileId) throws IOException {
        var cachedFile = Optional.ofNullable(this.cachedFiles.get(fileId));
        if (cachedFile.isPresent() && cachedFile.get().getContent().isEmpty()) {
            // Read File Content
            var contentStream = new FileInputStream(
                new File(
                    this.getSharedFolderPath().toFile(),
                    cachedFile.get().toFileNameHash()
                )
            );
            var content = contentStream.readAllBytes();
            cachedFile.get().setContent(content);
        }
        return cachedFile;
    }
    public void updateFile(VirtualFile modifiedFile, String originalFileHash, String originalFileId) throws IOException {
        Path sharedFolderPath = this.getSharedFolderPath();
        Optional<VirtualFile> deletedFile = Optional.empty();
        try {
            // Start Transaction
            deletedFile = Optional.of(this.cachedFiles.remove(originalFileId));
            this.cachedFiles.put(modifiedFile.getId(), modifiedFile);
            // Remove Old From FS
            Files.deleteIfExists(sharedFolderPath.resolve(originalFileHash));
            // Actually write file to the FS
            var fileWriter = new FileOutputStream(sharedFolderPath.resolve(modifiedFile.toFileNameHash()).toFile());
            fileWriter.write(modifiedFile.getContent().get());
            fileWriter.close();
            // End Transaction
        } catch (Exception e) {
            e.printStackTrace();
            // Rollback 
            this.cachedFiles.put(originalFileId, deletedFile.get());
            // Throw to out scope
            throw e;
        }
    }

    public Optional<VirtualFile> deleteFile(String targetFileId) throws UnsupportedEncodingException, IOException {
        // Remove file from cache
        var removedFile = Optional.ofNullable(this.cachedFiles.remove(targetFileId));
        // Remove file From FS
        if (removedFile.isPresent()) {
            Path sharedFolderPath = this.getSharedFolderPath();
            Files.deleteIfExists(sharedFolderPath.resolve(removedFile.get().toFileNameHash()));
        }
        // Return possible deleted file
        return removedFile;
    }

    private boolean isStoragePresent() {
        var sharedFolderLocation = settings.
            getProperty(ServerConfiguration.INSTANCE_SHARED_FOLDER)
            .replaceFirst("^~", System.getProperty("user.home"));
        Path sharedFolderPath = Paths.get(sharedFolderLocation).toAbsolutePath().normalize();
        return sharedFolderPath.toFile().exists();
    }

    public boolean createFileSlave(VirtualFile vFile) {
        var sharedFolderPath = this.getSharedFolderPath();
        try {
            this.cachedFiles.put(vFile.getId(), vFile);
            // Actually write file to the FS
            var fileWriter = new FileOutputStream(sharedFolderPath.resolve(vFile.toFileNameHash()).toFile());
            fileWriter.write(vFile.getContent().get());
            fileWriter.close();
            // End Transaction
            return true;
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}