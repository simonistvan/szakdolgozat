package fileexplorer.fileexplorer.model;

public class PreViewItem implements StorageItem {

    private final String name;
    private final String id;
    private final boolean folder;
    private final String thumbnailUrl;

    public PreViewItem(String name, String id, boolean folder, String thumbnailUrl) {
        this.name = name;
        this.id = id;
        this.folder = folder;
        this.thumbnailUrl = thumbnailUrl;
    }

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
        return folder;
    }

    @Override
    public String getThumbnailUrl() {
        if (thumbnailUrl != null) {
            return thumbnailUrl;
        }
        if (id != null) {
            return new java.io.File(id).toURI().toString();
        }
        return null;
    }

    @Override
    public String toString(){
        return name;
    }
}