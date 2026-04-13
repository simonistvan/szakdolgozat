package fileexplorer.fileexplorer.provider;

import fileexplorer.fileexplorer.model.StorageItem;

import java.io.InputStream;
import java.util.List;

public interface StorageProvider {
    List<StorageItem> listContents(String parentId) throws Exception;
    void delete(StorageItem storageItem) throws Exception;
    void rename(StorageItem storageItem, String newName) throws Exception;
    InputStream download(StorageItem storageItem) throws Exception;
    void upload(String parentId, String name, InputStream data) throws Exception;
    String createFolder(String parentId, String folderName) throws Exception;
}
