package fileexplorer.fileexplorer;

import java.io.File;

public class VirtualFile {
    private String id;
    private String name;
    private String path;
    private boolean isFolder;
    private boolean isDrive;

    public VirtualFile(File file) {
        this.name = file.getName();
        this.path = file.getPath();
        this.isFolder = file.isDirectory();
        this.isDrive = false;
    }

    public VirtualFile(String id, String name, boolean isFolder){
        this.id = id;
        this.name = name;
        this.isFolder = isFolder;
        this.isDrive = true;
    }

    public String getName(){
        return name;
    }

    public boolean isFolder(){
        return isFolder;
    }

    public boolean isDrive(){
        return isDrive;
    }

    public String getPathOrId() {
        return isDrive ? id : path;
    }

    @Override
    public String toString(){
        return (isFolder ? "\uD83D\uDCC1\t" : "\uD83D\uDDCE\t") + name;
    }
}
