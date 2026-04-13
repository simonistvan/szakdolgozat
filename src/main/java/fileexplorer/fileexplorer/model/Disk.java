package fileexplorer.fileexplorer.model;

public class Disk {
    private final String path;
    private final long totalSpace;
    private String costumaName;
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

    public void setCustomName(String name) {
        this.costumaName = name;
    }

    @Override
    public String toString() {
        return (costumaName != null) ? costumaName : (path == null || path.isEmpty()) ? "Root" : path;
    }
}
