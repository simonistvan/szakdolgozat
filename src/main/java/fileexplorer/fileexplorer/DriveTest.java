package fileexplorer.fileexplorer;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.util.List;

public class DriveTest {
    public static void main(String[] args) {
        try {
            Drive driveService = GoogleDriveAuth.getDriveService();

            FileList result = driveService.files().list()
                    .setPageSize(10)
                    .setFields("files(id, name)")
                    .execute();

            List<File> files = result.getFiles();

            if (files == null || files.isEmpty()) {
                System.out.println("Nincsenek fájlok a Google Drive-on.");
            } else {
                System.out.println("Fájlok a Drive-ban:");
                for (File file : files) {
                    System.out.printf("➡ %s (%s)%n", file.getName(), file.getId());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
