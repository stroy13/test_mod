package com.gmail.heagoo.apkeditor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public final class MyersMatcher {
    public static void match(String[] a, String[] b, List<int[]> out) throws IOException {
        List<int[]> out2;
        int ae = a.length;
        int be = b.length;
        int as = 0;
        int bs = 0;
        while (as < ae && bs < be && a[as].equals(b[bs])) {
            out.add(new int[]{as, bs});
            as++;
            bs++;
        }
        int suffix = 0;
        int ae2 = ae;
        int be2 = be;
        while (as < ae2 && bs < be2 && a[ae2 - 1].equals(b[be2 - 1])) {
            ae2--;
            be2--;
            suffix++;
        }
        int n = ae2 - as;
        int m = be2 - bs;
        if (n <= 0 || m <= 0) {
            out2 = out;
        } else {
            HashSet<String> values = new HashSet<>();
            if (n < m) {
                for (int i = as; i < ae2; i++) {
                    values.add(a[i]);
                }
                int i2 = 0;
                int i3 = bs;
                while (true) {
                    if (i3 >= be2) {
                        break;
                    }
                    if (values.contains(b[i3])) {
                        i2 = 1;
                        break;
                    }
                    i3++;
                }
                if (i2 != 0) {
                    out2 = out;
                    solve(a, as, ae2, b, bs, be2, out2);
                } else {
                    out2 = out;
                }
            } else {
                out2 = out;
                for (int i4 = bs; i4 < be2; i4++) {
                    values.add(b[i4]);
                }
                int i5 = 0;
                int i6 = as;
                while (true) {
                    if (i6 >= ae2) {
                        break;
                    }
                    if (values.contains(a[i6])) {
                        i5 = 1;
                        break;
                    }
                    i6++;
                }
                if (i5 != 0) {
                    solve(a, as, ae2, b, bs, be2, out2);
                }
            }
        }
        for (int i7 = 0; i7 < suffix; i7++) {
            out2.add(new int[]{ae2 + i7, be2 + i7});
        }
    }

    private static void solve(String[] a, int as, int ae, String[] b, int bs, int be, List<int[]> out) throws IOException {
        int n = ae - as;
        int m = be - bs;
        int limit = Math.min(n + m, 1400);
        int off = limit + 1;
        int[] v = new int[(limit * 2) + 3];
        v[off + 1] = 0;
        List<int[]> trace = new ArrayList<>();
        int last = -1;
        int d = 0;
        while (d <= limit) {
            int k = -d;
            while (true) {
                if (k <= d) {
                    int x = (k == (-d) || (k != d && v[(off + k) + (-1)] < v[(off + k) + 1])) ? v[off + k + 1] : v[(off + k) - 1] + 1;
                    int y = x - k;
                    while (x < n && y < m && a[as + x].equals(b[bs + y])) {
                        x++;
                        y++;
                    }
                    v[off + k] = x;
                    if (x >= n && y >= m) {
                        last = d;
                        break;
                    }
                    k += 2;
                } else {
                    break;
                }
            }
            trace.add((int[]) v.clone());
            if (last >= 0) {
                break;
            } else {
                d++;
            }
        }
        if (last < 0) {
            throw new IOException("Слишком много отличий для безопасного построчного сравнения; сравнение по хэшу доступно");
        }
        List<int[]> reverse = new ArrayList<>();
        int x2 = n;
        int y2 = m;
        int d2 = last;
        while (d2 > 0) {
            int[] prev = trace.get(d2 - 1);
            int k2 = x2 - y2;
            int pk = (k2 == (-d2) || (k2 != d2 && prev[(off + k2) + (-1)] < prev[(off + k2) + 1])) ? k2 + 1 : k2 - 1;
            int px = prev[off + pk];
            int py = px - pk;
            while (x2 > px && y2 > py) {
                reverse.add(new int[]{(as + x2) - 1, (bs + y2) - 1});
                x2--;
                y2--;
                n = n;
                m = m;
            }
            x2 = px;
            y2 = py;
            d2--;
            n = n;
            m = m;
        }
        while (x2 > 0 && y2 > 0 && a[(as + x2) - 1].equals(b[(bs + y2) - 1])) {
            reverse.add(new int[]{(as + x2) - 1, (bs + y2) - 1});
            x2--;
            y2--;
        }
        Collections.reverse(reverse);
        out.addAll(reverse);
    }

    private MyersMatcher() {
    }
}
