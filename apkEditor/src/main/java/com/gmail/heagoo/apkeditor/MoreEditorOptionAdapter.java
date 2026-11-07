package com.gmail.heagoo.apkeditor;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.Pair;
import java.util.ArrayList;
import java.util.List;

class MoreEditorOptionAdapter extends BaseAdapter {
    static final int CMD_CODE_SNIPPET = 6;
    static final int CMD_COLORPAD = 1;
    static final int CMD_COMMENT_LINES = 5;
    static final int CMD_DELETE_LINES = 2;
    static final int CMD_HELP = 4;
    static final int CMD_HTML = 0;
    static final int CMD_SETTINGS = 3;
    static final int CMD_TO_JAVA = 7;
    private Context ctx;
    private List<Integer> commands = new ArrayList();
    private List<Pair<Integer, Integer>> optionResIds = new ArrayList();

    MoreEditorOptionAdapter(Context ctx, String filePath) {
        this.ctx = ctx;
        if (TextEditBase.isSmali(filePath) || TextEditBase.isXml(filePath)) {
            this.optionResIds.add(new Pair<>(-1, Integer.valueOf(R.string.html)));
            this.commands.add(0);
        }
        this.optionResIds.add(new Pair<>(Integer.valueOf(R.drawable.ic_colorpad), Integer.valueOf(R.string.colorpad)));
        this.commands.add(1);
        if (TextEditBase.isSmali(filePath)) {
            this.optionResIds.add(new Pair<>(-1, Integer.valueOf(R.string.code_snippet)));
            this.commands.add(Integer.valueOf(CMD_CODE_SNIPPET));
        }
        this.optionResIds.add(new Pair<>(-1, Integer.valueOf(R.string.delete_lines)));
        this.commands.add(2);
        if (TextEditBase.isSmali(filePath)) {
            this.optionResIds.add(new Pair<>(-1, Integer.valueOf(R.string.comment_lines)));
            this.commands.add(5);
            this.optionResIds.add(new Pair<>(Integer.valueOf(R.drawable.ic_java), Integer.valueOf(R.string.java_code)));
            this.commands.add(Integer.valueOf(CMD_TO_JAVA));
        }
        this.optionResIds.add(new Pair<>(Integer.valueOf(R.drawable.ic_setting), Integer.valueOf(R.string.settings)));
        this.commands.add(3);
        this.optionResIds.add(new Pair<>(-1, Integer.valueOf(R.string.help)));
        this.commands.add(4);
    }

    public int getOptions() {
        return this.optionResIds.size();
    }

    public int getCommandByPosition(int position) {
        if (position < this.commands.size()) {
            return this.commands.get(position).intValue();
        }
        return -1;
    }

    @Override
    public int getCount() {
        return this.optionResIds.size();
    }

    @Override
    public Object getItem(int position) {
        return this.optionResIds.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(this.ctx).inflate(R.layout.item_more_option, parent, false);
            holder = new ViewHolder();
            holder.image = (ImageView) convertView.findViewById(R.id.menu_icon);
            holder.title = (TextView) convertView.findViewById(R.id.menu_title);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        Pair<Integer, Integer> data = this.optionResIds.get(position);
        if (((Integer) data.first).intValue() <= 0) {
            holder.image.setImageBitmap(null);
        } else {
            holder.image.setImageResource(((Integer) data.first).intValue());
        }
        holder.title.setText(((Integer) data.second).intValue());
        return convertView;
    }

    private static class ViewHolder {
        public ImageView image;
        public TextView title;

        private ViewHolder() {
        }
    }
}
