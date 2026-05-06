package fileexplorer.fileexplorer.provider;

import com.google.api.services.drive.model.File;
import fileexplorer.fileexplorer.model.DriverItem;
import fileexplorer.fileexplorer.model.StorageItem;
import fileexplorer.fileexplorer.service.GoogleDriveManager;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

public class DriverProvider implements StorageProvider {

    private final GoogleDriveManager manager;

    public DriverProvider(GoogleDriveManager manager) {
        this.manager = manager;
    }

    @Override
    public List<StorageItem> listContents(String parentId) throws Exception {
        List<File> files = (parentId == null || parentId.isEmpty() || parentId.equalsIgnoreCase("Google Drive"))
                ? manager.listFilesInRoot(100)
                :manager.listFilesInFolder(parentId,100);

        return files.stream()
                .map(f -> new DriverItem(
                        f.getId(),
                        f.getName(),
                        "application/vnd.google-apps.folder".equals(f.getMimeType()),
                        f.getThumbnailLink()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public void delete(StorageItem storageItem) throws Exception {
        manager.deleteDriveSmartOne(storageItem.getId());
    }

    @Override
    public void rename(StorageItem storageItem, String newName) throws Exception {
        manager.renameFile(storageItem.getId(), newName);
    }

    @Override
    public InputStream download(StorageItem storageItem) throws Exception {
        return manager.getDriveService().files()
                .get(storageItem.getId())
                .executeMediaAsInputStream();
    }

    @Override
    public void upload(String parentId, String name, InputStream data) throws Exception {
        com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
        fileMetadata.setName(name);

        //parentId már egy String, listába lehet pakolni getId() nélkül
        if (parentId != null && !parentId.isEmpty() && !parentId.equalsIgnoreCase("root")) {
            fileMetadata.setParents(java.util.Collections.singletonList(parentId));
        }

        com.google.api.client.http.InputStreamContent mediaContent =
                new com.google.api.client.http.InputStreamContent("application/octet-stream", data);

        manager.getDriveService().files()
                .create(fileMetadata, mediaContent)
                .setFields("id")
                .execute();
    }

    @Override
    public String createFolder(String parentId, String folderName) throws Exception {
        String effectiveParent = (parentId == null || parentId.isEmpty() || parentId.equals("root")) ? null : parentId;
        return manager.createDriveFolder(folderName, effectiveParent);
    }
}
