package com.gmail.heagoo.apkeditor;

import java.io.IOException;
import java.io.Writer;

public final class PublicXmlWriter {
    public static void writeEntry(Writer out, String type, String name, int resourceId) throws IOException {
        out.write("<public type=\"");
        out.write(type);
        out.write("\" name=\"");
        out.write(name);
        out.write("\" id=\"0x");
        String id = Integer.toHexString(resourceId);
        for (int n = id.length(); n < 8; n++) {
            out.write(48);
        }
        out.write(id);
        out.write("\" />\n");
    }

    private PublicXmlWriter() {
    }
}
