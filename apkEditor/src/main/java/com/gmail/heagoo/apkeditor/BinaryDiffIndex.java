package com.gmail.heagoo.apkeditor;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;

/* loaded from: /tmp/apkx/classes5.dex */
public final class BinaryDiffIndex {
    public static final int PAGE_ROWS = 128;
    public static final int RECORD_BYTES = 42;
    public static final int ROW_BYTES = 16;

    public static final class Row {
        public final byte[] left;
        public final long offset;
        public final byte[] right;

        Row(long o, byte[] a, byte[] b) {
            this.offset = o;
            this.left = a;
            this.right = b;
        }

        public boolean differs(int i) {
            return i >= this.left.length || i >= this.right.length || this.left[i] != this.right[i];
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:86:0x002e, code lost:
    
        throw new java.io.InterruptedIOException("Сравнение отменено");
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static long build(InputStream first, InputStream second, File index, BooleanSupplier cancelled) throws IOException {
        File file;
        Throwable th;
        Throwable e = null;
        long offset;
        byte[] a = new byte[65536];
        byte[] b = new byte[65536];
        int i = 16;
        byte[] zero = new byte[16];
        long offset2 = 0;
        long count = 0;
        try {
            file = index;
            try {
                DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file), 65536));
                while (true) {
                    if (cancelled != null) {
                        try {
                            if (cancelled.getAsBoolean()) {
                                break;
                            }
                        } catch (Throwable th2) {
                            th = th2;
                            offset = offset2;
                            try {
                                out.close();
                                if (th instanceof IOException) {
                                        throw (IOException) th;
                                    } else if (th instanceof RuntimeException) {
                                        throw (RuntimeException) th;
                                    } else {
                                        throw new IOException(th);
                                    }
                            } catch (Throwable th3) {
                                try {
                                    th.addSuppressed(th3);
                                    if (th instanceof IOException) {
                                        throw (IOException) th;
                                    } else if (th instanceof RuntimeException) {
                                        throw (RuntimeException) th;
                                    } else {
                                        throw new IOException(th);
                                    }
                                } catch (IOException | RuntimeException eX) {
                                    file.delete();
                                    throw eX;
                                }
                            }
                        }
                    }
                    try {
                        int na = fill(first, a);
                        try {
                            int nb = fill(second, b);
                            int n = Math.max(na, nb);
                            if (n == 0) {
                                try {
                                    out.close();
                                    return count;
                                } catch (IOException e2) {
                                    e = e2;
                                    file.delete();
                                    throw e;
                                } catch (RuntimeException e3) {
                                    e = e3;
                                    file.delete();
                                    throw e;
                                }
                            }
                            int p = 0;
                            while (p < n) {
                                int na2 = na;
                                int la = Math.max(0, Math.min(i, na2 - p));
                                offset = offset2;
                                try {
                                    int lb = Math.max(0, Math.min(16, nb - p));
                                    boolean different = la != lb;
                                    int i2 = 0;
                                    while (!different && i2 < la) {
                                        int i3 = i2;
                                        different = a[p + i2] != b[p + i2];
                                        i2 = i3 + 1;
                                    }
                                    if (different) {
                                        out.writeLong(offset + p);
                                        out.writeByte(la);
                                        out.writeByte(lb);
                                        if (la > 0) {
                                            out.write(a, p, la);
                                        }
                                        out.write(zero, 0, 16 - la);
                                        if (lb > 0) {
                                            out.write(b, p, lb);
                                        }
                                        out.write(zero, 0, 16 - lb);
                                        count++;
                                    }
                                    p += 16;
                                    na = na2;
                                    offset2 = offset;
                                    i = 16;
                                } catch (Throwable th4) {
                                    th = th4;
                                    out.close();
                                    if (th instanceof IOException) {
                                        throw (IOException) th;
                                    } else if (th instanceof RuntimeException) {
                                        throw (RuntimeException) th;
                                    } else {
                                        throw new IOException(th);
                                    }
                                }
                            }
                            long offset3 = offset2;
                            long offset4 = n;
                            offset2 = offset3 + offset4;
                            i = 16;
                        } catch (Throwable th5) {
                            th = th5;
                            offset = offset2;
                            th = th;
                            out.close();
                            if (th instanceof IOException) {
                                        throw (IOException) th;
                                    } else if (th instanceof RuntimeException) {
                                        throw (RuntimeException) th;
                                    } else {
                                        throw new IOException(th);
                                    }
                        }
                    } catch (Throwable th6) {
                        th = th6;
                    }
                }
            } catch (IOException | RuntimeException e4) {
                e = e4;
            }
        } catch (Throwable e5) {
            e = e5;
            file = index;
        }
        if (e != null) {
            file.delete();
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            throw (RuntimeException) e;
        }
        return count;
    }

    private static int fill(InputStream in, byte[] data) throws IOException {
        int r;
        int n = 0;
        if (in == null) {
            return 0;
        }
        while (n < data.length && (r = in.read(data, n, data.length - n)) >= 0) {
            if (r == 0) {
                int one = in.read();
                if (one < 0) {
                    break;
                }
                data[n] = (byte) one;
                n++;
            } else {
                n += r;
            }
        }
        return n;
    }

    public static List<Row> readPage(RandomAccessFile in, long start, int maximum) throws IOException {
        RandomAccessFile randomAccessFile = in;
        if (start < 0 || maximum < 0 || maximum > 128) {
            throw new IOException("Некорректная страница");
        }
        long length = randomAccessFile.length();
        if (length % 42 != 0) {
            throw new IOException("Повреждён индекс");
        }
        long count = length / 42;
        if (start > count) {
            throw new IOException("Некорректная позиция");
        }
        randomAccessFile.seek(42 * start);
        int n = (int) Math.min(maximum, count - start);
        List<Row> rows = new ArrayList<>(n);
        int i = 0;
        while (i < n) {
            long off = randomAccessFile.readLong();
            int a = randomAccessFile.readUnsignedByte();
            int b = randomAccessFile.readUnsignedByte();
            if (a > 16 || b > 16) {
                throw new IOException("Повреждён индекс");
            }
            byte[] aa = new byte[16];
            byte[] bb = new byte[16];
            randomAccessFile.readFully(aa);
            randomAccessFile.readFully(bb);
            rows.add(new Row(off, Arrays.copyOf(aa, a), Arrays.copyOf(bb, b)));
            i++;
            randomAccessFile = in;
            n = n;
        }
        return rows;
    }

    private BinaryDiffIndex() {
    }
}
