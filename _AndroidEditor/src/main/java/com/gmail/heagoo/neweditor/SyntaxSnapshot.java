package com.gmail.heagoo.neweditor;

import com.gmail.heagoo.neweditor.TokenMarker;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

public final class SyntaxSnapshot {
    public final int length;
    public final List<Line> lines;

    public interface Color {
        int color(Token token);
    }

    public static final class Span {
        public final int color;
        public final int end;
        public final int start;

        Span(int a, int b, int c) {
            this.start = a;
            this.end = b;
            this.color = c;
        }
    }

    public static final class Line {
        public final int end;
        public final List<Span> spans;
        public final int start;

        Line(int s, int e, List<Span> v) {
            this.start = s;
            this.end = e;
            this.spans = v;
        }
    }

    private SyntaxSnapshot(List<Line> l, int n) {
        this.lines = l;
        this.length = n;
    }

    public int lineAt(int offset) {
        int low = 0;
        int high = this.lines.size() - 1;
        while (low < high) {
            int mid = ((low + high) + 1) >>> 1;
            if (this.lines.get(mid).start <= offset) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return low;
    }

    public static SyntaxSnapshot parse(String text, TokenMarker marker, Color color, BooleanSupplier cancelled) {
        Segment seg;
        String str = text;
        ArrayList<Line> lines = new ArrayList<>();
        int n = str.length();
        int spanCount = 0;
        int start = 0;
        TokenMarker.LineContext context = null;
        while (!cancelled.getAsBoolean()) {
            int i = 600000;
            if (lines.size() >= 600000) {
                throw new IllegalStateException("Слишком много строк");
            }
            int next = str.indexOf(10, start);
            int end = next < 0 ? n : next;
            ArrayList<Span> spans = new ArrayList<>();
            if (marker != null) {
                try {
                    Segment seg2 = new Segment();
                    seg2.array = str.substring(start, end).toCharArray();
                    seg2.offset = 0;
                    seg2.count = seg2.array.length;
                    DefaultTokenHandler h = new DefaultTokenHandler();
                    context = marker.markTokens(context, h, seg2);
                    Token t = TokenMerger.merge(h.getTokens());
                    while (t != null) {
                        if (t.id == 0 || t.length <= 0) {
                            seg = seg2;
                        } else {
                            int from = t.offset + start;
                            int to = t.length + from;
                            if (from < start || to > end || to <= from) {
                                seg = seg2;
                            } else {
                                spanCount++;
                                if (spanCount > i) {
                                    throw new IllegalStateException("Слишком много элементов подсветки");
                                }
                                seg = seg2;
                                try {
                                    spans.add(new Span(from, to, color.color(t)));
                                } catch (IllegalStateException e) {
                                    throw e;
                                } catch (Exception e2) {
                                    context = null;
                                    lines.add(new Line(start, end, spans));
                                    if (next < 0) {
                                    }
                                    return new SyntaxSnapshot(lines, n);
                                }
                            }
                        }
                        t = t.next;
                        i = 600000;
                        seg2 = seg;
                    }
                } catch (IllegalStateException e3) {
                    throw e3;
                } catch (Exception e4) {
                }
            }
            lines.add(new Line(start, end, spans));
            if (next < 0 && (start = next + 1) <= n) {
                str = text;
            } else {
                return new SyntaxSnapshot(lines, n);
            }
        }
        return null;
    }
}
