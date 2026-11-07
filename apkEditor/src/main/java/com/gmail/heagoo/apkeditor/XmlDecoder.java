package com.gmail.heagoo.apkeditor;

import brut.androlib.AndrolibException;
import brut.androlib.res.data.ResPackage;
import brut.androlib.res.decoder.AXmlResourceParser;
import brut.androlib.res.decoder.ResAttrDecoder;
import brut.androlib.res.decoder.ResStreamDecoder;
import brut.androlib.res.xml.ResXmlEncoders;
import brut.util.FastXmlSerializer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class XmlDecoder implements ResStreamDecoder {
    private AXmlResourceParser par;
    IReferenceDecoder refDecoder;
    private static final float[] RADIX_MULTS = {0.00390625f, 3.051758E-5f, 1.192093E-7f, 4.656613E-10f};
    private static final String[] DIMENSION_UNITS = {"px", "dip", "sp", "pt", "in", "mm", "", ""};
    private static final String[] FRACTION_UNITS = {"%", "%p", "", "", "", "", "", ""};
    private boolean apkProtected = false;
    private FastXmlSerializer ser = new FastXmlSerializer();

    public interface IReferenceDecoder {
        String getAttributeById(int i);

        String getResReference(int i);
    }

    public XmlDecoder(IReferenceDecoder refDecoder, ResPackage resPackage) {
        this.refDecoder = refDecoder;
        ResAttrDecoder attrDecoder = new ResAttrDecoder();
        attrDecoder.setCurrentPackage(resPackage);
        this.par = new AXmlResourceParser();
        this.par.setAttrDecoder(attrDecoder);
    }

    private static String getPackage(int id) {
        if ((id >>> 24) == 1) {
            return "android:";
        }
        return "";
    }

    public static float complexToFloat(int complex) {
        return (complex & (-256)) * RADIX_MULTS[(complex >> 4) & 3];
    }

    public void decode(InputStream input, OutputStream output) throws AndrolibException {
        try {
            this.ser.setOutput(output, "utf-8");
            this.par.setInput(input, "utf-8");
            while (this.par.nextToken() != 1) {
                try {
                    int eventType = this.par.getEventType();
                    event(this.par, eventType);
                } catch (Exception e) {
                    throw new AndrolibException(e.getMessage());
                }
            }
            this.ser.endDocument();
        } catch (Exception e2) {
            throw new AndrolibException(e2.getMessage());
        }
    }

    public String decodeManifest(InputStream input, OutputStream output) throws AndrolibException {
        try {
            this.ser.setOutput(output, "utf-8");
            this.par.setInput(input, "utf-8");
            while (this.par.nextToken() != 1) {
                try {
                    int eventType = this.par.getEventType();
                    if (eventType == 2) {
                        String curName = this.par.getName();
                        if ("manifest".equals(curName)) {
                            startTagEvent("package");
                        } else {
                            startTagEvent();
                        }
                    } else {
                        event(this.par, eventType);
                    }
                } catch (Exception e) {
                    throw new AndrolibException(e.getMessage());
                }
            }
            this.ser.flush();
            return null;
        } catch (Exception e2) {
            throw new AndrolibException(e2.getMessage());
        }
    }

    private boolean parseManifest(XmlPullParser pp) throws AndrolibException {
        for (int i = 0; i < pp.getAttributeCount(); i++) {
            pp.getAttributeName(i);
        }
        return true;
    }

    private void startTagEvent() throws XmlPullParserException, IOException {
        this.ser.writeStartTag(this.par);
        int attrCount = this.par.getAttributeCount();
        if (attrCount >= 2) {
            String[] nss = new String[attrCount];
            String[] names = new String[attrCount];
            String[] values = new String[attrCount];
            for (int i = 0; i < attrCount; i++) {
                nss[i] = this.par.getAttributeNamespace(i);
                names[i] = this.par.getAttributeName(i);
                values[i] = this.par.getAttributeValue(i);
            }
            this.ser.attributesToSort(nss, names, values);
            return;
        }
        for (int i2 = 0; i2 < attrCount; i2++) {
            this.ser.attribute(this.par.getAttributeNamespace(i2), this.par.getAttributeName(i2), this.par.getAttributeValue(i2));
        }
    }

    private String startTagEvent(String attrName) throws XmlPullParserException, IOException {
        String ret = null;
        this.ser.writeStartTag(this.par);
        int attrCount = this.par.getAttributeCount();
        if (attrCount >= 2) {
            String[] nss = new String[attrCount];
            String[] names = new String[attrCount];
            String[] values = new String[attrCount];
            for (int i = 0; i < attrCount; i++) {
                nss[i] = this.par.getAttributeNamespace(i);
                names[i] = this.par.getAttributeName(i);
                values[i] = this.par.getAttributeValue(i);
                if (attrName.equals(names[i])) {
                    ret = values[i];
                }
            }
            this.ser.attributesToSort(nss, names, values);
        } else {
            for (int i2 = 0; i2 < attrCount; i2++) {
                String name = this.par.getAttributeName(i2);
                String value = this.par.getAttributeValue(i2);
                if (attrName.equals(name)) {
                    ret = value;
                }
                this.ser.attribute(this.par.getAttributeNamespace(i2), name, value);
            }
        }
        return ret;
    }

    public void event(AXmlResourceParser pp, int eventType) throws XmlPullParserException, IOException {
        switch (eventType) {
            case 0:
                this.ser.startDocument(pp.getInputEncoding(), (Boolean) null);
                break;
            case 1:
                this.ser.endDocument();
                break;
            case 2:
                this.ser.writeStartTag(pp);
                for (int i = 0; i < pp.getAttributeCount(); i++) {
                    this.ser.attribute(pp.getAttributeNamespace(i), pp.getAttributeName(i), pp.getAttributeValue(i));
                }
                break;
            case 3:
                this.ser.endTag(pp.getNamespace(), pp.getName());
                break;
            case 4:
                this.ser.text(ResXmlEncoders.encodeAsXmlValue(pp.getText()));
                break;
            case 5:
                this.ser.cdsect(pp.getText());
                break;
            case 6:
                this.ser.entityRef(pp.getName());
                break;
            case 7:
                String s = pp.getText();
                this.ser.ignorableWhitespace(s);
                break;
            case 8:
                this.ser.processingInstruction(pp.getText());
                break;
            case 9:
                this.ser.comment(pp.getText());
                break;
            case 10:
                this.ser.docdecl(pp.getText());
                break;
        }
    }

    public void setApkProtected(boolean b) {
        this.apkProtected = b;
        this.par.setApkProtected(this.apkProtected);
    }
}
