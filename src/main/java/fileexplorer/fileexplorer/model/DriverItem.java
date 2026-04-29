package fileexplorer.fileexplorer.model;

public record DriverItem(String id, String name, boolean isFolder, String thumbnailUrl) implements StorageItem {
    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isFolder() {
        return isFolder;
    }

    @Override
    public String getThumbnailUrl() {
        return thumbnailUrl;
    }
}
