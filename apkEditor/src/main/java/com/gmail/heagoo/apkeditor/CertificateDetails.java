package com.gmail.heagoo.apkeditor;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAKey;
import java.security.interfaces.ECKey;
import java.security.interfaces.RSAKey;
import java.util.Base64;
import java.util.Locale;
import java.util.zip.CRC32;

/* loaded from: /tmp/apkx/classes5.dex */
public final class CertificateDetails {
    private static String hex(byte[] a) {
        StringBuilder b = new StringBuilder(a.length * 2);
        for (byte x : a) {
            b.append(String.format(Locale.ROOT, "%02x", Integer.valueOf(x & 255)));
        }
        return b.toString();
    }

    private static String digest(String algorithm, byte[] data) {
        try {
            return hex(MessageDigest.getInstance(algorithm).digest(data));
        } catch (Exception e) {
            return "недоступен";
        }
    }

    private static String b64(byte[] data) {
        return Base64.getMimeEncoder(64, new byte[]{10}).encodeToString(data);
    }

    public static String describe(X509Certificate c) throws Exception {
        BigInteger p;
        int i = 0;
        byte[] der = c.getEncoded();
        byte[] signature = c.getSignature();
        CRC32 crc = new CRC32();
        crc.update(der);
        PublicKey key = c.getPublicKey();
        int bits = 0;
        if (key instanceof RSAKey) {
            p = ((RSAKey) key).getModulus();
        } else {
            if (!(key instanceof ECKey)) {
                if ((key instanceof DSAKey) && ((DSAKey) key).getParams() != null) {
                    p = ((DSAKey) key).getParams().getP();
                }
                StringBuilder b = new StringBuilder();
                b.append("Тип сертификата: ").append(c.getType()).append("\nВерсия сертификата: ").append(c.getVersion());
                b.append("\nСерийный номер: ").append(c.getSerialNumber().toString(16)).append(" (hex)");
                b.append("\nВладелец: ").append(c.getSubjectX500Principal().getName());
                b.append("\nИздатель: ").append(c.getIssuerX500Principal().getName());
                b.append("\nДействителен с: ").append(c.getNotBefore()).append("\nДействителен до: ").append(c.getNotAfter());
                b.append("\nАлгоритм подписи сертификата: ").append(c.getSigAlgName());
                b.append("\nПубличный ключ: ").append(key.getAlgorithm());
                if (bits > 0) {
                    b.append(" ").append(bits).append(" бит");
                }
                b.append("\n\nОтпечатки сертификата (DER):\nCRC32: ").append(String.format(Locale.ROOT, "%08x", Long.valueOf(crc.getValue())));
                String[] strArr = {"MD5", "SHA-1", "SHA-224", "SHA-256", "SHA-384", "SHA-512"};
                for (i = 0; i < 6; i++) {
                    String a = strArr[i];
                    b.append("\n").append(a).append(": ").append(digest(a, der));
                }
                b.append("\n\nСертификат (Base64 DER):\n").append(b64(der));
                b.append("\n\nПубличный ключ (Base64 X.509):\n").append(b64(key.getEncoded()));
                b.append("\n\nПодпись сертификата (Base64):\n").append(b64(signature));
                b.append("\nПодпись сертификата (hex):\n").append(hex(signature)).append('\n');
                return b.toString();
            }
            p = ((ECKey) key).getParams().getOrder();
        }
        bits = p.bitLength();
        StringBuilder b2 = new StringBuilder();
        b2.append("Тип сертификата: ").append(c.getType()).append("\nВерсия сертификата: ").append(c.getVersion());
        b2.append("\nСерийный номер: ").append(c.getSerialNumber().toString(16)).append(" (hex)");
        b2.append("\nВладелец: ").append(c.getSubjectX500Principal().getName());
        b2.append("\nИздатель: ").append(c.getIssuerX500Principal().getName());
        b2.append("\nДействителен с: ").append(c.getNotBefore()).append("\nДействителен до: ").append(c.getNotAfter());
        b2.append("\nАлгоритм подписи сертификата: ").append(c.getSigAlgName());
        b2.append("\nПубличный ключ: ").append(key.getAlgorithm());
        if (bits > 0) {
            b2.append(" ").append(bits).append(" бит");
        }
        b2.append("\n\nОтпечатки сертификата (DER):\nCRC32: ").append(String.format(Locale.ROOT, "%08x", Long.valueOf(crc.getValue())));
        String[] strArr2 = {"MD5", "SHA-1", "SHA-224", "SHA-256", "SHA-384", "SHA-512"};
        for (i = 0; i < 6; i++) {
            String a2 = strArr2[i];
            b2.append("\n").append(a2).append(": ").append(digest(a2, der));
        }
        b2.append("\n\nСертификат (Base64 DER):\n").append(b64(der));
        b2.append("\n\nПубличный ключ (Base64 X.509):\n").append(b64(key.getEncoded()));
        b2.append("\n\nПодпись сертификата (Base64):\n").append(b64(signature));
        b2.append("\nПодпись сертификата (hex):\n").append(hex(signature)).append('\n');
        return b2.toString();
    }

    private CertificateDetails() {
    }
}
