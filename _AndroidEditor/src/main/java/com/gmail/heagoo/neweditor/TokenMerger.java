package com.gmail.heagoo.neweditor;

public class TokenMerger {
    public static Token merge(Token token) {
        if (token == null) {
            return null;
        }
        Token previous = token;
        for (Token curToken = token.next; curToken != null; curToken = curToken.next) {
            if (curToken.id == previous.id && curToken.offset == previous.offset + previous.length) {
                previous.length += curToken.length;
            } else {
                previous.next = curToken;
                previous = curToken;
            }
        }
        previous.next = null;
        return token;
    }
}
