package com.gmail.heagoo.neweditor;

import com.gmail.heagoo.neweditor.TokenMarker;

public class DefaultTokenHandler implements TokenHandler {
    protected Token firstToken;
    protected Token lastToken;
    protected TokenMarker.LineContext lineContext;

    public void init() {
        this.firstToken = null;
        this.lastToken = null;
    }

    public Token getTokens() {
        return this.firstToken;
    }

    @Override
    public void handleToken(Segment seg, byte id, int offset, int length, TokenMarker.LineContext context) {
        Token token = createToken(id, offset, length, context);
        if (token != null) {
            addToken(token, context);
        }
    }

    public TokenMarker.LineContext getLineContext() {
        return this.lineContext;
    }

    @Override
    public void setLineContext(TokenMarker.LineContext lineContext) {
        this.lineContext = lineContext;
    }

    protected ParserRuleSet getParserRuleSet(TokenMarker.LineContext context) {
        while (context != null) {
            if (!context.rules.isBuiltIn()) {
                return context.rules;
            }
            context = context.parent;
        }
        return null;
    }

    protected Token createToken(byte id, int offset, int length, TokenMarker.LineContext context) {
        return new Token(id, offset, length, getParserRuleSet(context));
    }

    protected void addToken(Token token, TokenMarker.LineContext context) {
        if (this.firstToken == null) {
            this.lastToken = token;
            this.firstToken = token;
        } else {
            this.lastToken.next = token;
            this.lastToken = this.lastToken.next;
        }
    }
}
