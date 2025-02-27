package com.melonloader.installer.core;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class PathDefinitions {
    public Path base;
    public Path dexBase;
    public Path dexOriginal;
    public Path dexPatch;
    public Path dexOutput;
    public Path outputAPK;
    public Path libraryAPK;
    public Path dependenciesDir;

    public Path unityBase;
    public Path unityNativeBase;
    public Path unityManagedBase;
    public Path unityZip;

    public Path keystore;

    public PathDefinitions(Properties properties)
    {
        base = Paths.get(properties.tempDir);

        dexBase = Paths.get(base.toString(), "dex");
        dexOriginal = Paths.get(dexBase.toString(), "original");
        dexPatch = Paths.get(dexBase.toString(), "patch");
        dexOutput = Paths.get(dexBase.toString(), "output");
        outputAPK = properties.outputApk != null ? Paths.get(properties.outputApk) : Paths.get(base.toString(), "base.apk");
        libraryAPK = properties.libraryApk == "" ? null : Paths.get(base.toString(), Paths.get(properties.libraryApk).getFileName().toString());
        dependenciesDir = Paths.get(base.toString(), "dependencies");

        dexPatch = Paths.get(dependenciesDir.toString(), "dex");

        unityBase = Paths.get(base.toString(), "unity");
        unityNativeBase = Paths.get(unityBase.toString(), "Libs");
        unityManagedBase = Paths.get(unityBase.toString(), "Managed");
        unityZip = Paths.get(base.toString(), "unity.zip");

        keystore = Paths.get(base.toString(), "key.keystore");
    }
}
