package com.gmail.heagoo.apkeditor;

import android.content.Context;
import com.gmail.heagoo.apkeditor.base.R;

class ModificationCategory {
    public int addedFiles;
    public int modifiedFiles;
    public String name;
    public int removedFiles;

    public ModificationCategory(String _name, int add, int removed, int modified) {
        this.name = _name;
        this.addedFiles = add;
        this.removedFiles = removed;
        this.modifiedFiles = modified;
    }

    public int getItems() {
        if (this.addedFiles < 0 && this.removedFiles < 0 && this.modifiedFiles < 0) {
            return 1;
        }
        if (this.addedFiles == 0 && this.removedFiles == 0 && this.modifiedFiles == 0) {
            return 1;
        }
        int items = 0;
        if (this.addedFiles > 0) {
            items = 0 + 1;
        }
        if (this.removedFiles > 0) {
            items++;
        }
        if (this.modifiedFiles > 0) {
            return items + 1;
        }
        return items;
    }

    public String getItemDescription(Context ctx, int index) {
        if (this.addedFiles < 0 && this.removedFiles < 0 && this.modifiedFiles < 0) {
            return ctx.getString(R.string.str_modified);
        }
        if (this.addedFiles == 0 && this.removedFiles == 0 && this.modifiedFiles == 0) {
            return ctx.getString(R.string.str_not_modified);
        }
        int curIndex = -1;
        if (this.addedFiles > 0) {
            curIndex = (-1) + 1;
        }
        if (curIndex == index) {
            String fmt = ctx.getString(R.string.str_num_added_file);
            return String.format(fmt, Integer.valueOf(this.addedFiles));
        }
        if (this.removedFiles > 0) {
            curIndex++;
        }
        if (curIndex == index) {
            String fmt2 = ctx.getString(R.string.str_num_removed_file);
            return String.format(fmt2, Integer.valueOf(this.removedFiles));
        }
        if (this.modifiedFiles > 0) {
            curIndex++;
        }
        if (curIndex == index) {
            String fmt3 = ctx.getString(R.string.str_num_modified_file);
            return String.format(fmt3, Integer.valueOf(this.modifiedFiles));
        }
        return null;
    }
}
