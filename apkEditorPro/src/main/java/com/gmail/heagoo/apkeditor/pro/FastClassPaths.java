package com.gmail.heagoo.apkeditor.pro;

import com.android.tools.smali.util.PathUtil;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/* loaded from: /tmp/apkx/classes26.dex */
final class FastClassPaths {
    private final CaseProbe probe;
    private final Node root;

    interface CaseProbe {
        boolean isSensitive(File file) throws IOException;
    }

    FastClassPaths(File root) {
        this(root, new CaseProbe() {
            @Override // com.gmail.heagoo.apkeditor.pro.FastClassPaths.CaseProbe
            public final boolean isSensitive(File file) throws IOException {
                return PathUtil.testCaseSensitivity(file);
            }
        });
    }

    FastClassPaths(File root, CaseProbe probe) {
        this.root = new Node(root);
        this.probe = probe;
    }

    static boolean supports(String descriptor) {
        if (descriptor == null || descriptor.length() < 3 || descriptor.charAt(0) != 'L' || descriptor.charAt(descriptor.length() - 1) != ';') {
            return false;
        }
        int begin = 1;
        for (int i = 1; i < descriptor.length(); i++) {
            char c = descriptor.charAt(i);
            if (c == '/' || i == descriptor.length() - 1) {
                int length = i - begin;
                if (length != 0) {
                    if (length <= (c == '/' ? 249 : 243)) {
                        begin = i + 1;
                    }
                }
                return false;
            }
            if ((c < 'a' || c > 'z') && ((c < 'A' || c > 'Z') && ((c < '0' || c > '9') && c != '_' && c != '$' && c != '-'))) {
                return false;
            }
        }
        return true;
    }

    static boolean canUse(File output, String[] descriptors) {
        String[] children;
        String os = System.getProperty("os.name", "");
        if (os.startsWith("Windows") || !"I".toLowerCase().equals("i") || (children = output.list()) == null || children.length != 0) {
            return false;
        }
        for (String type : descriptors) {
            if (!supports(type)) {
                return false;
            }
        }
        return true;
    }

    File get(String descriptor) throws IOException {
        Node node = this.root;
        int begin = 1;
        for (int i = 1; i < descriptor.length(); i++) {
            char c = descriptor.charAt(i);
            if (c == '/' || i == descriptor.length() - 1) {
                String name = descriptor.substring(begin, i);
                if (c != '/') {
                    return node.file(name);
                }
                node = node.directory(name);
                begin = i + 1;
            }
        }
        throw new IOException("Invalid class descriptor: " + descriptor);
    }

    private final class Node {
        final File dir;
        final Map<String, Node> directories = new HashMap();
        final Map<String, Counts> groups = new HashMap();
        Boolean sensitive;

        Node(File dir) {
            this.dir = dir;
        }

        Node directory(String name) throws IOException {
            Node old = this.directories.get(name);
            if (old != null) {
                return old;
            }
            int suffix = next(name);
            Node created = FastClassPaths.this.new Node(new File(this.dir, suffix == 0 ? name : name + "." + suffix));
            this.directories.put(name, created);
            return created;
        }

        File file(String name) throws IOException {
            int suffix = next(name + ".smali");
            return new File(this.dir, name + (suffix == 0 ? "" : "." + suffix) + ".smali");
        }

        int next(String name) throws IOException {
            String folded = name.toLowerCase();
            Counts group = this.groups.get(folded);
            if (group == null) {
                this.groups.put(folded, new Counts(name));
                return 0;
            }
            if (this.sensitive == null && !group.first.equals(name)) {
                if (!this.dir.isDirectory() && !this.dir.mkdirs() && !this.dir.isDirectory()) {
                    throw new IOException("Cannot create directory: " + this.dir);
                }
                try {
                    this.sensitive = Boolean.valueOf(FastClassPaths.this.probe.isSensitive(this.dir));
                } catch (IOException e) {
                    this.sensitive = false;
                }
            }
            if (group.exact == null) {
                group.exact = new HashMap();
                group.exact.put(group.first, 1);
            }
            int exact = group.exact.getOrDefault(name, 0).intValue();
            int result = Boolean.TRUE.equals(this.sensitive) ? exact : group.total;
            group.total++;
            group.exact.put(name, Integer.valueOf(exact + 1));
            if (result > 99999) {
                throw new IOException("Too many conflicting class filenames");
            }
            return result;
        }
    }

    private static final class Counts {
        Map<String, Integer> exact;
        final String first;
        int total = 1;

        Counts(String first) {
            this.first = first;
        }
    }
}
