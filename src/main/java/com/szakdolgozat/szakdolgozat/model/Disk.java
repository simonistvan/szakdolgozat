package com.szakdolgozat.szakdolgozat.model;

public class Disk {
    private String path;
    private String name;
    private long totalSpace;
    private long freeSpace;


    public Disk(String path, String name, long totalSpace, long freeSpace) {
        this.path = path;
        this.name = name;
        this.totalSpace = totalSpace;
        this.freeSpace = freeSpace;
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getTotalSpace() {
        return totalSpace;
    }

    public long getFreeSpace() {
        return freeSpace;
    }

    @Override
    public String toString() {
        return "Name: "+getName()+"\nTotal Space: "+getTotalSpace()/(1024*1024*1024)+" GB\nFree Space: "+getFreeSpace()/(1024*1024*1024)+" GB";
    }
}