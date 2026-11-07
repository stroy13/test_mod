package com.gmail.heagoo.apkeditor;

import brut.androlib.res.data.ResPackage;

public class CacheManager {
    private static CacheManager inst = new CacheManager();
    private ResPackage frameworkPackage;
    private ResPackage[] frameworkPackages;

    private CacheManager() {
    }

    public static CacheManager instance() {
        return inst;
    }

    public ResPackage getFrameworkPackage() {
        return this.frameworkPackage;
    }

    public ResPackage[] getFrameworkPackages() {
        return this.frameworkPackages;
    }

    public void setFrameworkPackages(ResPackage[] packages) {
        this.frameworkPackages = packages;
    }

    public void setFrameworkPackage(ResPackage rp) {
        this.frameworkPackage = rp;
    }
}
