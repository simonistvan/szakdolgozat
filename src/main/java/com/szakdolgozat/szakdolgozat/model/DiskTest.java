package com.szakdolgozat.szakdolgozat.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class DiskTest {

    public static void main(String[] args) {
        List<Disk> disks = new ArrayList<Disk>();
        File[] roots = File.listRoots();
        for (File root : roots) {
            disks.add(new Disk(
                    root.getPath(),
                    root.getPath(),
                    root.getTotalSpace(),
                    root.getFreeSpace()
            ));
        }

        Scanner scanner = new Scanner(System.in);
        String currentDir = "C:\\";

        while (true) {
            System.out.print(currentDir + "> ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("exit")) {
                System.out.println("Kilépés");
                break;
            }

            String[] parts = input.split(" ");
            String cmd = parts[0].toLowerCase();

            try {
                switch (cmd) {
                    case "ls" -> runCommand(currentDir, "cmd.exe", "/c", "dir");

                    case "cd" -> {
                        if (parts.length > 1) {
                            currentDir = cd(currentDir, parts[1]);
                        } else {
                            System.out.println("Hiányzó útvonal!");
                        }
                    }
                    case "disks" -> {
                        for(Disk disk : disks) {
                            System.out.println(disk.toString());
                        }

                    }
                    case "copy" -> {
                        if (parts.length != 3) {
                            System.out.println("Használat: copy <forrás> <cél>");
                            break;
                        }

                        File src = new File(parts[1]);
                        File dst = new File(parts[2]);

                        if (!src.exists()) {
                            System.out.println("A forrás nem létezik: " + src.getAbsolutePath());
                            break;
                        }

                        // ellenőrzéssel megnézzük, hogy abszólut útvonalról van-e szó, ha igen akkor kiegészítjük a jelenlegi könyvtárral
                        if (!src.isAbsolute()) {
                            src = new File(currentDir, parts[1]);
                        }
                        if (!dst.isAbsolute()) {
                            dst = new File(currentDir, parts[2]);
                        }

                        if(src.equals(dst)){
                            int i = 1;
                            dst = new File(parts[2]+"("+i+")");
                            while(dst.exists()){
                                i++;
                                dst = new File(parts[2]+"("+i+")");
                                if (!dst.isAbsolute()) {
                                    dst = new File(currentDir, parts[2]+"("+i+")");
                                }

                            }
                        }



                        if (src.isDirectory()) {
                            copyFolder(src, dst);
                        } else if (src.isFile()) {
                            File target;
                            // ha a dst könyvtár, akkor abba másoljuk be a fájlt
                            if(dst.isDirectory()){
                                target = new File(dst, src.getName());
                            } // ha a dst nem könyvtár, akkor az lesz a cél.
                            else{
                                target = dst;
                            }

                            if (!target.getParentFile().exists()) { // itt kerül ellenőrzésre, hogy a cél mappának léteznek-e a szülő mappái, ha nem léteznek akkor létrehozza
                                target.getParentFile().mkdirs();
                            }

                            Files.copy(  //itt kerül a tényleges másolás. A forrás útját átmásolja a célba, azaz létrehozza ott is. ha létezik azonos nevű fájl az felülírásra kerül.
                                    src.toPath(),
                                    target.toPath(),
                                    StandardCopyOption.REPLACE_EXISTING
                            );
                        }
                    }

                    case "del" -> {
                        File file = new File(currentDir, parts[1]);
                        del(file);
                    }

                    case "create" -> {
                        if(parts.length!=1){
                            System.out.println("A parancs 1 paramétert tud feldolgozni.");
                        }
                        else{
                            runCommand(currentDir, "cmd.exe", "/c", "type nul > " + parts[1]);
                        }
                    }

                    case "rn" -> {
                        File file = new File(currentDir, parts[1]);
                        File renamed = new File(currentDir, parts[2]);
                        if (!file.renameTo(renamed)) {
                            System.out.println("Átnevezés sikertelen!");
                        }
                    }

                    default -> System.out.println("Ismeretlen parancs: " + cmd);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        scanner.close();
    }

    public static String cd(String currentDir, String path) {
        File newDir;
        File current = new File(currentDir);

        if (path.equals("back")) { // visszalépésért felel, cd .. parancs megfelelője
            if(current.getParentFile() == null) {
                System.out.println("Nem tudsz visszább lépni.");
                return currentDir;
            }
            newDir = new File(currentDir).getParentFile();

        } else if (path.matches("^[A-Za-z]:$")) { // A regex szolgál, hogy az input csak meghajtó lehessen
            newDir = new File(path + "\\");
        } else { // ez szolgál a könyvtárváltásra
            newDir = new File(currentDir, path);
        }

        if (!newDir.exists() || !newDir.isDirectory()) { // itt kerül ellenőrzésre az új könyvtár
            System.err.println("A megadott könyvtár nem létezik: " + path);
            return currentDir;
        }

        return newDir.getAbsolutePath();
    }

    public static void runCommand(String workingDir, String... command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(workingDir));
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );
            BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream())
            );

            String line;
            while ((line = reader.readLine()) != null) System.out.println(line);
            while ((line = errorReader.readLine()) != null) System.err.println(line);

            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void copyFolder(File src, File dst) throws IOException {
        if (src.isDirectory()) {
            if (!dst.exists()) {
                dst.mkdirs();
            }

            File[] files = src.listFiles();
            for (File file : files) {
                copyFolder(file, new File(dst, file.getName()));
            }
        } else {
            Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static void del(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files.length > 0) {
                for (File delfile : files) {
                    del(delfile);
                }
            }
        }

        if (!file.delete()) {
            System.err.println("Nem sikerült törölni: " + file.getAbsolutePath());
        }
    }
}
