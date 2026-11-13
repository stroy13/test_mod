package com.gmail.heagoo.apkeditor.util;

import java.util.*;

/** Multi-pattern scan of alias suffixes, including overlapping and nested matches. */
final class Aapt2AliasScanner {
    private static final class Node {
        Map<Character, Integer> edges;
        int fail, output = -1, pattern = -1;
        int next(char c) { if (edges == null) return -1; Integer n = edges.get(c); return n == null ? -1 : n; }
        void put(char c, int n) { if (edges == null) edges = new HashMap<>(); edges.put(c, n); }
    }
    private final List<Node> nodes = new ArrayList<>();
    private final List<String> suffixes = new ArrayList<>();
    private final Map<String,Integer> indexes = new HashMap<>();
    private final int[] used;

    Aapt2AliasScanner(Collection<String> names) {
        nodes.add(new Node());
        for (String name : names) {
            String suffix = name.substring(1);
            if (indexes.containsKey(suffix)) continue;
            int index = suffixes.size(); indexes.put(suffix,index); suffixes.add(suffix);
            int state = 0;
            for (int i=0; i<suffix.length(); i++) {
                char ch = suffix.charAt(i); int next=nodes.get(state).next(ch);
                if (next < 0) { next=nodes.size(); nodes.add(new Node()); nodes.get(state).put(ch,next); }
                state=next;
            }
            nodes.get(state).pattern=index;
        }
        used = new int[suffixes.size()];
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        if (nodes.get(0).edges != null) for (int child : nodes.get(0).edges.values()) queue.add(child);
        while (!queue.isEmpty()) {
            int parent=queue.remove(); Node n=nodes.get(parent);
            if (n.edges == null) continue;
            for (Map.Entry<Character,Integer> edge : n.edges.entrySet()) {
                int child=edge.getValue(), f=n.fail; char ch=edge.getKey();
                while (f != 0 && nodes.get(f).next(ch) < 0) f=nodes.get(f).fail;
                int next=nodes.get(f).next(ch); if (next >= 0) f=next;
                Node childNode=nodes.get(child); childNode.fail=f;
                childNode.output=nodes.get(f).pattern >= 0 ? f : nodes.get(f).output;
                queue.add(child);
            }
        }
    }

    void scan(String text) {
        int state=0;
        for (int i=0; i<text.length(); i++) {
            char c=text.charAt(i); int next=nodes.get(state).next(c);
            while (state != 0 && next < 0) { state=nodes.get(state).fail; next=nodes.get(state).next(c); }
            state=next < 0 ? 0 : next;
            for (int n=state; n>=0; n=nodes.get(n).output) {
                int index=nodes.get(n).pattern;
                if (index < 0) continue;
                int before=i-suffixes.get(index).length();
                if (before >= 0) { char prefix=text.charAt(before); if (prefix >= 'a' && prefix <= 'z') used[index] |= 1 << (prefix-'a'); }
            }
        }
    }

    String choose(String name) {
        int mask=used[indexes.get(name.substring(1))];
        for (char c='a'; c<='z'; c++) if ((mask & (1 << (c-'a'))) == 0) return c + name.substring(1);
        return null;
    }
}
