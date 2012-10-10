package net.rubygrapefruit.platform.internal;

import net.rubygrapefruit.platform.NativeException;
import net.rubygrapefruit.platform.internal.jni.NativeLibraryFunctions;

import java.io.*;
import java.net.URL;
import java.nio.channels.FileLock;

public class NativeLibraryLocator {
    private final File extractDir;

    public NativeLibraryLocator(File extractDir) {
        this.extractDir = extractDir;
    }

    public File find(String libraryName) throws IOException {
        if (extractDir != null) {
            File libFile = new File(extractDir, String.format("%s/%s", NativeLibraryFunctions.VERSION, libraryName));
            File lockFile = new File(libFile.getParentFile(), libFile.getName() + ".lock");
            lockFile.getParentFile().mkdirs();
            lockFile.createNewFile();
            RandomAccessFile lockFileAccess = new RandomAccessFile(lockFile, "rw");
            try {
                // Take exclusive lock on lock file
                FileLock lock = lockFileAccess.getChannel().lock();
                if (lockFile.length() > 0 && lockFileAccess.readBoolean()) {
                    // Library has been extracted
                    return libFile;
                }
                URL resource = getClass().getClassLoader().getResource(libraryName);
                if (resource != null) {
                    // Extract library and write marker to lock file
                    libFile.getParentFile().mkdirs();
                    copy(resource, libFile);
                    lockFileAccess.seek(0);
                    lockFileAccess.writeBoolean(true);
                    return libFile;
                }
            } finally {
                // Also releases lock
                lockFileAccess.close();
            }
        } else {
            URL resource = getClass().getClassLoader().getResource(libraryName);
            if (resource != null) {
                File libFile;
                File libDir = File.createTempFile("native-platform", "dir");
                libDir.delete();
                libDir.mkdirs();
                libFile = new File(libDir, libraryName);
                libFile.deleteOnExit();
                copy(resource, libFile);
                return libFile;
            }
        }

        File libFile = new File("build/binaries/" + libraryName);
        if (libFile.isFile()) {
            return libFile;
        }

        return null;
    }

    private static void copy(URL source, File dest) {
        try {
            InputStream inputStream = source.openStream();
            try {
                OutputStream outputStream = new FileOutputStream(dest);
                try {
                    byte[] buffer = new byte[4096];
                    while (true) {
                        int nread = inputStream.read(buffer);
                        if (nread < 0) {
                            break;
                        }
                        outputStream.write(buffer, 0, nread);
                    }
                } finally {
                    outputStream.close();
                }
            } finally {
                inputStream.close();
            }
        } catch (IOException e) {
            throw new NativeException(String.format("Could not extract native JNI library."), e);
        }
    }
}