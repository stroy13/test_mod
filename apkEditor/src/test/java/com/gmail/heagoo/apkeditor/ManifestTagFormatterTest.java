package com.gmail.heagoo.apkeditor;

import org.junit.Test;
import static org.junit.Assert.*;

public class ManifestTagFormatterTest {
    @Test public void splitsOnlyTagWhitespaceAndKeepsTextAndQuotes() {
        String xml = "<?xml version=\"1.0\"?>\n<manifest xmlns:android=\"http://x\" package=\"p\">\n"
                + "  <application android:label=\"a  > b\" android:name='x'/><!-- <x a=\"b\"> -->"
                + "<![CDATA[  x  <a x='b'> ]]><x> a   b </x></manifest>";
        String expected = "<?xml version=\"1.0\"?>\n<manifest xmlns:android=\"http://x\"\n    package=\"p\">\n"
                + "  <application\n      android:label=\"a  > b\"\n      android:name='x'/><!-- <x a=\"b\"> -->"
                + "<![CDATA[  x  <a x='b'> ]]><x> a   b </x></manifest>";
        assertEquals(expected, ManifestTagFormatter.format(xml));
        assertEquals(expected, ManifestTagFormatter.format(expected));
    }
    @Test public void keepsSingleAttributeOnTagLine() {
        assertEquals("<uses-permission android:name=\"x\" />", ManifestTagFormatter.format("<uses-permission android:name=\"x\" />"));
        assertEquals("<uses-sdk\n    android:min=\"1\"\n    android:target=\"2\" />", ManifestTagFormatter.format("<uses-sdk android:min=\"1\" android:target=\"2\" />"));
    }
    @Test public void leavesIncompleteTagsAndDtdUntouched() {
        assertEquals("<a x=\"unterminated", ManifestTagFormatter.format("<a x=\"unterminated"));
        assertEquals("<a x=\"v\" broken>", ManifestTagFormatter.format("<a x=\"v\" broken>"));
        String dtd = "<!DOCTYPE a [<!ENTITY e 'x'>]><a x='v' y='z'/>";
        assertEquals(dtd, ManifestTagFormatter.format(dtd));
    }
}
