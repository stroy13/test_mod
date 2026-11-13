package com.gmail.heagoo.folderlist;

import java.util.Locale;

public final class FileNameFilter {
    private FileNameFilter() {}
    public static boolean matches(String name, String query) {
        return "..".equals(name) || query == null || query.isEmpty()
                || name.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
    }
}
