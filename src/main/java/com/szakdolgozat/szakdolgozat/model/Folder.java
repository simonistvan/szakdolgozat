package com.szakdolgozat.szakdolgozat.model;

import java.io.File;
import java.util.List;

public class Folder {
    private String name;
    private String path;
    private String owner;
    private List<File> files;
    private List<Folder> folders;
    private long size;

    public Folder(String name, String path, String owner, List<File> files, List<Folder> folders, long size) {
        this.name = name;
        this.path = path;
        this.owner = owner;
        this.files = files;
        this.folders = folders;
        this.size = size;
    }

    public void setName(Folder folder, String name){
        this.name = name;
    }

    public void setPath(String path){
        this.path = path;
    }

    public void setOwner(String owner)
    {
        this.owner = owner;

    }

    public void setFiles(List<File> files)
    {
        this.files = files;
    }

    public void setFolders(List<Folder> folders)
    {
        this.folders = folders;
    }

    public void setSize(long size)
    {
        this.size = size;
    }

    public String getName()
    {
        return name;
    }

    public String getPath()
    {
        return path;
    }

    public String getOwner()
    {
        return owner;
    }

    public List<File> getFiles()
    {
        return files;
    }

    public List<Folder> getFolders()
    {
        return folders;
    }

    public long getSize()
    {
        return size;
    }

}
