package fileexplorer.fileexplorer;

import java.io.File;

public class Disk {
    private final String path;
    private final long totalSpace;
    private final long freeSpace;

    public Disk(String path, long totalSpace, long freeSpace) {
        this.path = path;
        this.totalSpace = totalSpace;
        this.freeSpace = freeSpace;
    }

    public String getPath() {
        return path;
    }
    public long getTotalSpace() {
        return totalSpace;
    }
    public long getFreeSpace() {
        return freeSpace;
    }

    @Override
    public String toString() {
        return path;
    }
}
