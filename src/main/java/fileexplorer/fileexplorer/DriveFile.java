package fileexplorer.fileexplorer;

public class DriveFile {
    private String id;
    private String name;
    private String mimeType;

    public  DriveFile(String id, String name, String mimeType) {
        this.id = id;
        this.name = name;
        this.mimeType = mimeType;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getMimeType() {
        return mimeType;
    }

    public boolean isFolder (){
        return ("application/vnd.google-apps.folder".equals(mimeType));
    }

    public String toString(){
        return (isFolder() ? "\uD83D\uDCC1\t":"\uD83D\uDDCE\t") + name;
    }
}
