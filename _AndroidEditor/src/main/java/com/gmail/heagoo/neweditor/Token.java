package com.gmail.heagoo.neweditor;

public class Token {
    public static final byte COMMENT1 = 1;
    public static final byte COMMENT2 = 2;
    public static final byte COMMENT3 = 3;
    public static final byte COMMENT4 = 4;
    public static final byte DIGIT = 5;
    public static final byte END = Byte.MAX_VALUE;
    public static final byte FUNCTION = 6;
    public static final byte ID_COUNT = 19;
    public static final byte INVALID = 7;
    public static final byte KEYWORD1 = 8;
    public static final byte KEYWORD2 = 9;
    public static final byte KEYWORD3 = 10;
    public static final byte KEYWORD4 = 11;
    public static final byte LABEL = 12;
    public static final byte LITERAL1 = 13;
    public static final byte LITERAL2 = 14;
    public static final byte LITERAL3 = 15;
    public static final byte LITERAL4 = 16;
    public static final byte MARKUP = 17;
    public static final byte NULL = 0;
    public static final byte OPERATOR = 18;
    public static final String[] TOKEN_TYPES = {"NULL", "COMMENT1", "COMMENT2", "COMMENT3", "COMMENT4", "DIGIT", "FUNCTION", "INVALID", "KEYWORD1", "KEYWORD2", "KEYWORD3", "KEYWORD4", "LABEL", "LITERAL1", "LITERAL2", "LITERAL3", "LITERAL4", "MARKUP", "OPERATOR"};
    public byte id;
    public int length;
    public Token next;
    public int offset;
    public ParserRuleSet rules;

    public Token(byte id, int offset, int length, ParserRuleSet rules) {
        this.id = id;
        this.offset = offset;
        this.length = length;
        this.rules = rules;
    }

    public static byte stringToToken(String value) {
        try {
            return Token.class.getField(value).getByte(null);
        } catch (Exception e) {
            return (byte) -1;
        }
    }

    public static String tokenToString(byte token) {
        return token == Byte.MAX_VALUE ? "END" : TOKEN_TYPES[token];
    }

    public String toString() {
        return "[id=" + ((int) this.id) + ",offset=" + this.offset + ",length=" + this.length + "]";
    }
}
