package com.gmail.heagoo.apkeditor;

import com.gmail.heagoo.apkeditor.ManifestSyntax;
import java.util.Collections;
import java.util.List;

class LineRecord {
    int indent;
    String lineData;
    int lineIndex;
    int sectionEnd;
    int sectionStart;
    private String tag;
    List<ManifestSyntax.Span> syntax = Collections.emptyList();
    boolean collapsed = false;
    boolean deleted = false;

    LineRecord(int index, String content, String hanging) {
        this.lineIndex = index;
        this.lineData = content;
        while (this.lineData.startsWith(hanging)) {
            this.indent++;
            this.lineData = this.lineData.substring(hanging.length());
        }
        while (this.lineData.startsWith("    ")) {
            this.indent++;
            this.lineData = this.lineData.substring(4);
        }
        this.lineData = this.lineData.trim();
        this.sectionStart = -1;
        this.sectionEnd = -1;
    }

    public String getName() {
        String data = this.lineData;
        int startPos = data.indexOf("android:name=\"");
        if (startPos == -1) {
            return null;
        }
        int startPos2 = startPos + 14;
        int endPos = data.indexOf("\"", startPos2);
        if (endPos != -1) {
            String name = data.substring(startPos2, endPos);
            return name;
        }
        String name2 = data.substring(startPos2);
        return name2;
    }

    String getSectionTag() {
        int startPos;
        String tag;
        if (this.tag != null) {
            return this.tag;
        }
        String data = this.lineData;
        if (this.lineIndex != this.sectionStart) {
            startPos = data.indexOf("</");
            if (startPos != -1) {
                startPos += 2;
            }
        } else {
            startPos = data.indexOf("<");
            if (startPos != -1) {
                startPos++;
            }
        }
        if (startPos == -1) {
            return null;
        }
        int endPos = data.indexOf(" ");
        if (endPos == -1) {
            if (this.sectionStart == this.sectionEnd) {
                endPos = data.indexOf("/>");
                if (endPos != -1) {
                    endPos--;
                }
            } else {
                endPos = data.indexOf(">");
            }
        }
        if (endPos != -1) {
            tag = data.substring(startPos, endPos);
        } else {
            tag = data.substring(startPos);
        }
        String tag2 = tag.trim();
        this.tag = tag2;
        return tag2;
    }
}
