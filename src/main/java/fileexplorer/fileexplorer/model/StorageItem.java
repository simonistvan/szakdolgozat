package fileexplorer.fileexplorer.model;

public interface StorageItem {
    String getName();
    String getId();
    boolean isFolder();
    String getThumbnailUrl();
}
