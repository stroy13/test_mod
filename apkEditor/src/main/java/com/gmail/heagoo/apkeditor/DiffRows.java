package com.gmail.heagoo.apkeditor;

import com.gmail.heagoo.apkeditor.ComparisonCore;
import java.util.ArrayList;
import java.util.List;

public final class DiffRows {
    public static List<ComparisonCore.Row> withContext(List<ComparisonCore.Row> rows, int context) {
        if (context < 0) {
            throw new IllegalArgumentException();
        }
        boolean[] keep = new boolean[rows.size()];
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).status != ComparisonCore.Status.SAME) {
                for (int j = Math.max(0, i - context); j <= Math.min(rows.size() - 1, i + context); j++) {
                    keep[j] = true;
                }
            }
        }
        List<ComparisonCore.Row> result = new ArrayList<>();
        boolean gap = false;
        for (int i2 = 0; i2 < rows.size(); i2++) {
            if (keep[i2]) {
                if (gap && !result.isEmpty()) {
                    result.add(new ComparisonCore.Row(0, 0, "⋯", "⋯", ComparisonCore.Status.SAME));
                }
                gap = false;
                result.add(rows.get(i2));
            } else {
                gap = true;
            }
        }
        return result;
    }

    private DiffRows() {
    }
}
