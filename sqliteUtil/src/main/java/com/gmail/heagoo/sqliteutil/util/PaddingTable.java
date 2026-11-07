package com.gmail.heagoo.sqliteutil.util;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import com.gmail.heagoo.sqliteutil.CustomScrollView;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class PaddingTable implements View.OnClickListener, View.OnTouchListener {
    private boolean bShowWholeTable;
    private int bgColor;
    private ArrayList<String> columnNames;
    private Context ctx;
    private int headerBgColor;
    private TableRow headerRow;
    private int headerRowHeight;
    private int headerRowWidth;
    private int headerTextColor;
    private int hoverColor;
    private ImageView paddingImage;
    private ITableRowClicked rowClickInterface;
    private TableRow.LayoutParams rowLayoutParam;
    private CustomScrollView scrollView;
    private int scrollViewHeight;
    private int scrollViewWidth;
    private int seperateColor;
    private int seperateLineHeight;
    private View[] seperateLines;
    private List<ArrayList<String>> tableData;
    private TableLayout.LayoutParams tableLayoutParam;
    private TableRow[] tableRows;
    private TableLayout tableView;
    private int textColor;

    public interface ITableRowClicked {
        void tableRowClicked(int i, boolean z);
    }

    public PaddingTable(Context ctx, CustomScrollView scrollView, TableLayout tableView, ITableRowClicked rowClickInterface) {
        this(ctx, scrollView, tableView, rowClickInterface, false);
    }

    public PaddingTable(Context ctx, CustomScrollView scrollView, TableLayout tableView, ITableRowClicked rowClickInterface, boolean isDark) {
        this.bgColor = -1;
        this.textColor = -15195082;
        this.hoverColor = -1511944;
        this.seperateColor = -3355444;
        this.headerTextColor = -1;
        this.headerBgColor = -13214523;
        this.ctx = ctx;
        this.scrollView = scrollView;
        this.tableView = tableView;
        this.rowClickInterface = rowClickInterface;
        if (isDark) {
            this.bgColor = -14998222;
            this.textColor = -1182984;
            this.hoverColor = -14009776;
            this.seperateColor = -13352107;
            this.headerTextColor = -1;
            this.headerBgColor = -13214523;
        }
    }

    public void setTableHeaderNames(ArrayList<String> columnNames) {
        this.columnNames = columnNames;
    }

    public void setTableData(List<ArrayList<String>> tableData) {
        this.tableData = tableData;
    }

    public void prepareTable() {
        this.tableLayoutParam = new TableLayout.LayoutParams(-2, -2);
        this.rowLayoutParam = new TableRow.LayoutParams(-2, -2);
        this.rowLayoutParam.setMargins(8, 0, 8, 0);
        int rowNum = this.tableData.size();
        int colNum = this.columnNames.size();
        this.tableRows = new TableRow[rowNum];
        TextView[] textTvs = new TextView[colNum];
        this.seperateLines = new View[rowNum];
        for (int i = 0; i < rowNum; i++) {
            List<String> rowData = this.tableData.get(i);
            this.tableRows[i] = new TableRow(this.ctx);
            this.tableRows[i].setId(i);
            for (int j = 0; j < colNum; j++) {
                textTvs[j] = new TextView(this.ctx);
                textTvs[j].setTextColor(this.textColor);
                textTvs[j].setText(rowData.get(j));
            }
            for (int j2 = 0; j2 < colNum; j2++) {
                this.tableRows[i].addView(textTvs[j2], j2, this.rowLayoutParam);
            }
            this.seperateLines[i] = new View(this.ctx);
            this.seperateLines[i].setBackgroundColor(this.seperateColor);
        }
    }

    public void showSearchResult(List<ArrayList<String>> data) {
        int rowNum = data.size();
        int colNum = this.columnNames.size();
        for (int i = 0; i < rowNum; i++) {
            ArrayList<String> rowData = data.get(i);
            for (int col = 0; col < colNum; col++) {
                TextView tv = (TextView) this.tableRows[i].getChildAt(col);
                tv.setText(rowData.get(col));
            }
            this.tableRows[i].setVisibility(0);
            this.seperateLines[i].setVisibility(0);
        }
        for (int i2 = rowNum; i2 < this.tableData.size(); i2++) {
            this.tableRows[i2].setVisibility(8);
            this.seperateLines[i2].setVisibility(8);
        }
        this.bShowWholeTable = false;
    }

    public void drawTable() {
        this.tableView.removeAllViews();
        this.bShowWholeTable = true;
        this.headerRow = new TableRow(this.ctx);
        for (int j = 0; j < this.columnNames.size(); j++) {
            TextView textTv = new TextView(this.ctx);
            textTv.setTextColor(this.headerTextColor);
            textTv.setText(this.columnNames.get(j));
            this.headerRow.addView(textTv, this.rowLayoutParam);
        }
        this.headerRow.setBackgroundColor(this.headerBgColor);
        this.tableView.addView(this.headerRow, this.tableLayoutParam);
        TableRow.LayoutParams rowParam = new TableRow.LayoutParams(-1, -2);
        TableRow.LayoutParams lineLayout = new TableRow.LayoutParams(-1, 1);
        for (int i = 0; i < this.tableData.size(); i++) {
            this.tableView.addView(this.tableRows[i], (i * 2) + 1, rowParam);
            this.tableRows[i].setOnClickListener(this);
            this.tableRows[i].setOnTouchListener(this);
            this.tableView.addView(this.seperateLines[i], (i * 2) + 2, lineLayout);
        }
        setupSizeObserver();
    }

    private void setupSizeObserver() {
        if (this.scrollView == null) {
        }
    }

    public void padding() {
        if (this.headerRowHeight == 0) {
            this.headerRowHeight = 32;
        }
        if (this.headerRowWidth < this.scrollViewWidth) {
            this.paddingImage.getLayoutParams().width = this.scrollViewWidth - this.headerRowWidth;
        }
        int rowNum = this.tableData.size();
        int remainHeight = (this.scrollViewHeight - (this.headerRowHeight * (rowNum + 1))) - (this.seperateLineHeight * rowNum);
        int paddingRows = remainHeight / this.headerRowHeight;
        for (int i = 0; i < paddingRows; i++) {
            TableRow row = new TableRow(this.ctx);
            row.addView(new TextView(this.ctx));
            this.tableView.addView(row);
        }
    }

    @Override
    public void onClick(View v) {
        int index = v.getId();
        if (this.rowClickInterface != null) {
            this.rowClickInterface.tableRowClicked(index, this.bShowWholeTable);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction();
        if (action == 0) {
            v.setBackgroundColor(this.hoverColor);
        } else if (action == 1) {
            v.setBackgroundColor(this.bgColor);
            v.performClick();
        } else if ((action & 1) != 0 || (action & 4) != 0) {
            v.setBackgroundColor(this.bgColor);
        }
        return true;
    }

    private static class MyHandler extends Handler {
        private WeakReference<PaddingTable> ref;

        public MyHandler(PaddingTable table) {
            this.ref = new WeakReference<>(table);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    PaddingTable table = this.ref.get();
                    if (table != null) {
                        table.padding();
                        break;
                    }
                    break;
            }
        }
    }
}
