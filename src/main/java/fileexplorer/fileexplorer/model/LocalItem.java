package fileexplorer.fileexplorer.model;

public record LocalItem(String name, String id, boolean isFolder) implements StorageItem {

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
        if(id!=null){
            return new java.io.File(id).toURI().toString();
        }
        return null;
    }
}
