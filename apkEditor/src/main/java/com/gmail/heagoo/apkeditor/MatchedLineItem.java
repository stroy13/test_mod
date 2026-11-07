package com.gmail.heagoo.apkeditor;

class MatchedLineItem {
    String lineContent;
    int lineIndex;
    int matchedPosition;

    public MatchedLineItem(int lineIndex, int position, String line) {
        this.lineIndex = lineIndex;
        this.matchedPosition = position;
        this.lineContent = line;
    }
}
