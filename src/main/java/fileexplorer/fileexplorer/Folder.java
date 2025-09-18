package fileexplorer.fileexplorer;

import java.awt.*;

public class Folder {
    private final String path;
    private String name;

    public Folder(String path,String name) {
        this.path = path;
        this.name = name;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }


    @Override
    public String toString() {
        return getPath();
    }
}
