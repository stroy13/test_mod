package com.gmail.heagoo.apkeditor.util;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class Aapt2AliasScannerTest {
    @Test public void matchesNaiveSearchForOverlappingAndRandomNames() {
        Random random = new Random(404); List<String> names = new ArrayList<>();
        names.add("$abc"); names.add("$bc"); names.add("$c"); names.add("$aaa");
        for (int i=0;i<785;i++) names.add("$name_" + i + "_" + (i % 11));
        List<String> texts = new ArrayList<>(); texts.add("zabc aabc bbc caaa aaaa");
        for (int i=0;i<2500;i++) {
            String suffix=names.get(random.nextInt(names.size())).substring(1);
            texts.add("/res/drawable/"+(char)('a'+random.nextInt(26))+suffix+".xml random");
        }
        Aapt2AliasScanner scanner=new Aapt2AliasScanner(names);
        for (String text:texts) scanner.scan(text);
        for (String name:names) {
            String expected=null;
            for(char c='a';c<='z';c++) { String candidate=c+name.substring(1); boolean found=false; for(String text:texts) if(text.contains(candidate)){found=true;break;} if(!found){expected=candidate;break;} }
            assertEquals(name, expected, scanner.choose(name));
        }
    }
    @Test public void allPrefixesReservedAndSuffixAtStartHandled() {
        Aapt2AliasScanner scanner=new Aapt2AliasScanner(Collections.singleton("$name"));
        scanner.scan("name"); assertEquals("aname",scanner.choose("$name"));
        for(char c='a'; c<='z'; c++) scanner.scan(c+"name");
        assertNull(scanner.choose("$name"));
    }
}
