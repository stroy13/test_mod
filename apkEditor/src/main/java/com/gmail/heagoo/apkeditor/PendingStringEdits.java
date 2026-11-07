package com.gmail.heagoo.apkeditor;

import com.gmail.heagoo.neweditor.ParserRule;
import common.types.StringItem;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

final class PendingStringEdits {
    private static File file(File filesDir, String token) throws IOException {
        if (token == null || !token.matches("[0-9a-f-]{36}")) {
            throw new IOException("Invalid edit session");
        }
        File dir = new File(filesDir, "pending-string-edits");
        if (dir.isDirectory() || dir.mkdirs() || dir.isDirectory()) {
            return new File(dir, token + ".dat");
        }
        throw new IOException("Cannot create edit journal folder");
    }

    static HashMap<String, Map<String, String>> snapshot(Map<String, Map<String, String>> edits) {
        HashMap<String, Map<String, String>> copy = new HashMap<>();
        if (edits != null) {
            for (Map.Entry<String, Map<String, String>> e : edits.entrySet()) {
                if (e.getKey() != null && e.getValue() != null && !e.getValue().isEmpty()) {
                    copy.put(e.getKey(), new HashMap(e.getValue()));
                }
            }
        }
        return copy;
    }

    static void save(File filesDir, String token, Map<String, Map<String, String>> edits) throws IOException {
        File target = file(filesDir, token);
        File tmp = File.createTempFile(".pending-", ".tmp", target.getParentFile());
        try {
            FileOutputStream fos = new FileOutputStream(tmp);
            try {
                ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(fos, ParserRule.REGEXP));
                try {
                    out.writeObject(snapshot(edits));
                    out.flush();
                    fos.getFD().sync();
                    out.close();
                    fos.close();
                    if (!tmp.renameTo(target)) {
                        throw new IOException("Cannot publish edit journal");
                    }
                } finally {
                }
            } finally {
            }
        } finally {
            tmp.delete();
        }
    }

    static HashMap<String, Map<String, String>> load(File filesDir, String token) throws IOException {
        File target = file(filesDir, token);
        if (!target.isFile()) {
            throw new FileNotFoundException("Edit journal not found");
        }
        if (target.length() > 67108864) {
            throw new IOException("Edit journal too large");
        }
        try {
            ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(target), ParserRule.REGEXP));
            try {
                Object o = in.readObject();
                if (!(o instanceof Map)) {
                    throw new IOException("Invalid edit journal");
                }
                HashMap<String, Map<String, String>> result = new HashMap<>();
                for (Map.Entry<?, ?> e : ((Map<?, ?>) o).entrySet()) {
                    if (!(e.getKey() instanceof String) || !(e.getValue() instanceof Map)) {
                        throw new IOException("Invalid edit journal entry");
                    }
                    HashMap<String, String> map = new HashMap<>();
                    for (Map.Entry<?, ?> v : ((Map<?, ?>) e.getValue()).entrySet()) {
                        if (!(v.getKey() instanceof String) || !(v.getValue() instanceof String)) {
                            throw new IOException("Invalid edit value");
                        }
                        map.put((String) v.getKey(), (String) v.getValue());
                    }
                    result.put((String) e.getKey(), map);
                }
                in.close();
                return result;
            } catch (Throwable th) {
                try {
                    in.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        } catch (ClassCastException | ClassNotFoundException e2) {
            throw new IOException("Cannot read edit journal", e2);
        }
    }

    static void delete(File filesDir, String token) {
        if (token == null || !token.matches("[0-9a-f-]{36}")) {
            return;
        }
        new File(new File(filesDir, "pending-string-edits"), token + ".dat").delete();
    }

    static void merge(Map<String, ArrayList<StringItem>> all, Map<String, Map<String, String>> edits) {
        if (all == null || edits == null) {
            return;
        }
        for (Map.Entry<String, Map<String, String>> e : edits.entrySet()) {
            ArrayList<StringItem> list = all.get(e.getKey());
            Map<String, String> values = e.getValue();
            if (list != null && values != null && !values.isEmpty()) {
                Set<String> applied = new HashSet<>();
                Iterator<StringItem> it = list.iterator();
                while (it.hasNext()) {
                    StringItem item = it.next();
                    if (values.containsKey(item.name) && applied.add(item.name)) {
                        item.value = values.get(item.name);
                    }
                }
            }
        }
    }

    private PendingStringEdits() {
    }
}
