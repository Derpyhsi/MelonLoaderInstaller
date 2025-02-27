package com.melonloader.installer.core.steps;

import com.melonloader.installer.core.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Step__50__RepackApk extends InstallerStep {
    public boolean Run() throws IOException {
        ZipHelper zipHelper = new ZipHelper(paths.outputAPK.toString());

        CopyTo(zipHelper, paths.dexOutput, "*.dex", "");
        if (!properties.hasOriginalDex)
            CopyTo(zipHelper, paths.dexOriginal, "*.dex", "originalDex");

        CopyTo(zipHelper, Paths.get(paths.dependenciesDir.toString(), "core"), "*.dll", "assets/melonloader/etc");
        CopyTo(zipHelper, Paths.get(paths.dependenciesDir.toString(), "managed"), "*.dll", "assets/melonloader/etc/managed");
        CopyTo(zipHelper, Paths.get(paths.dependenciesDir.toString(), "mono", "bcl"), "*.dll", "assets/melonloader/etc/managed");
        CopyTo(zipHelper, Paths.get(paths.dependenciesDir.toString(), "support_modules"), "*.dll", "assets/melonloader/etc/support");
        CopyTo(zipHelper, Paths.get(paths.dependenciesDir.toString(), "assembly_generation"), "*.dll", "assets/melonloader/etc/assembly_generation/managed");
        CopyTo(zipHelper, paths.unityManagedBase, "*.dll", "assets/melonloader/etc/assembly_generation/unity");

        // Copy entire /etc/ folder
        zipHelper.QueueWrite(Paths.get(paths.dependenciesDir.toString(), "etc").toString(), "assets/bin/Data/Managed/etc");

        // Not a split APK, handle as normal
        properties.logger.Log(String.valueOf(properties.isSplit));
        if (!properties.isSplit)
        {
            CopyTo(zipHelper, Paths.get(paths.dependenciesDir.toString(), "native"), "*.so", "lib/arm64-v8a");
            CopyTo(zipHelper, Paths.get(paths.unityNativeBase.toString(), "arm64-v8a"), "*.so", "lib/arm64-v8a");
        }
        // Split APK, scream and shout due to the pain and suffering it has brought upon us
        else
        {
            // Write modded manifest
            zipHelper.QueueWrite(Paths.get(properties.tempDir, "AndroidManifest.xml").toString(), "AndroidManifest.xml");

            ZipHelper libHelper = new ZipHelper(paths.libraryAPK.toString());
            CopyTo(libHelper, Paths.get(paths.dependenciesDir.toString(), "native"), "*.so", "lib/arm64-v8a");
            CopyTo(libHelper, Paths.get(paths.unityNativeBase.toString(), "arm64-v8a"), "*.so", "lib/arm64-v8a");
            libHelper.Write();
        }

        zipHelper.Write();

        return true;
    }

//    private static void CopyTo(ZipHelper zipHelper, FileSystem fs, String matcher, String dest) throws IOException {
//        CopyTo(zipHelper, fs, matcher, );
//    }

    private void CopyTo(ZipHelper zipHelper, Path base, String matcher, String dest) throws IOException {
        List<Path> srcFiles = GetFiles(base, matcher);

//        Files.createDirectories(Paths.get(dest));

        for (Path srcFile : srcFiles) {
//            Files.copy(srcFile, targetFS.getPath(dest.toString(), srcFile.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);
            zipHelper.QueueWrite(srcFile.toString(), (dest.equals("") ? "" : dest + "/") + srcFile.getFileName());
        }
    }

    private static List<Path> GetFiles(Path base, String glob) throws IOException {
        File directory = new File(base.toString());
        FileFilter filter = new WildcardFileFilter(glob);
        File[] files = directory.listFiles(filter);

        List<Path> output = new ArrayList<>();

        if (files == null || files.length == 0) {
            Main.GetProperties().logger.Log("WARNING: PATH [" + base.toString() + "/" + glob + "] is empty");
            return output;
        }
        for (File file : files) {
            output.add(file.toPath());
            Main.GetProperties().logger.Log("file: " + file.getPath().toString());
        }

        return output;
//        PathMatcher maskMatcher = fs.getPathMatcher(glob);
//
//        List<Path> matchedFiles;
//        final List<Path> filesToRemove;
//
//        matchedFiles = Files.walk(base)
//                .collect(Collectors.toList());
//
//        filesToRemove = new ArrayList<>(matchedFiles.size());
//
//        matchedFiles.forEach(foundPath -> {
//            if (!maskMatcher.matches(base.relativize(foundPath)) || Files.isDirectory(foundPath)) {
//                filesToRemove.add(foundPath);
//            }
//        });
//
//        matchedFiles.removeAll(filesToRemove);
//
//        return matchedFiles;
    }
}
