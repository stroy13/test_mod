package com.gmail.heagoo.neweditor;

import com.gmail.heagoo.neweditor.TokenMarker;

public interface TokenHandler {
    void handleToken(Segment segment, byte b, int i, int i2, TokenMarker.LineContext lineContext);

    void setLineContext(TokenMarker.LineContext lineContext);
}
