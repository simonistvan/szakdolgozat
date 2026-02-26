package fileexplorer.fileexplorer;

import java.io.File;
import java.io.IOException;

public class TempForDrive {
    private final File tempRoot;

    public TempForDrive() throws IOException {
        File tempFile = File.createTempFile("temp", "");
        if(!tempFile.delete()){
            throw new IOException("Nem sikerült törölni a fájlt.");
        }

        tempRoot = new File(tempFile.getAbsolutePath());
        if(!tempRoot.mkdir()){
            throw new IOException("Nem sikerült létrehozni a könyvtárat.");
        }
    }

    public File getTempRoot(){
        return tempRoot;
    }
}
