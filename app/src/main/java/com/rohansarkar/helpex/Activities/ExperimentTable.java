package com.rohansarkar.helpex.Activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Pair;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Font.FontFamily;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import com.rohansarkar.helpex.Adapters.GraphListAdapter;
import com.rohansarkar.helpex.Adapters.GraphSelectAdapter;
import com.rohansarkar.helpex.Adapters.NewColumnAdapter;
import com.rohansarkar.helpex.Adapters.TableCellAdapter;
import com.rohansarkar.helpex.Adapters.TableHeaderAdapter;
import com.rohansarkar.helpex.Adapters.TableRowNoAdapter;
import com.rohansarkar.helpex.CustomData.DataExperiment;
import com.rohansarkar.helpex.CustomData.DataRecord;
import com.rohansarkar.helpex.CustomData.DataSelectColumn;
import com.rohansarkar.helpex.DatabaseManagers.DatabaseExperimentManager;
import com.rohansarkar.helpex.DatabaseManagers.DatabaseRecordsManager;
import com.rohansarkar.helpex.R;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Locale;

import Assets.SmartRecyclerView;
import Assets.Util;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;

/**
 * Created by rohan on 11/5/17.
 */
public class ExperimentTable extends AppCompatActivity implements View.OnClickListener{

    private static final String LOG_TAG = "ExperimentTable";
    SmartRecyclerView tableRecyclerView, rowRecyclerView;
    RecyclerView headerRecyclerView;
    RecyclerView.Adapter tableAdapter, headerAdapter, rowAdapter;
    LinearLayoutManager tableLayoutManager, headerLayoutManager, rowLayoutManager;
    RecyclerView.OnScrollListener tableScrollListener;
    FloatingActionButton addRow;
    CoordinatorLayout layout;
    Toolbar toolbar;
    ImageView overflowMenu;
    ImageView headerPadding;
    TextView toolbarTitle;
    RelativeLayout emptyLayout;   //If NumberOfColumns <= 0
    RelativeLayout tableLayout;   //If NumberOfColumns > 0
    ViewGroup headerViewGroup;

    DatabaseExperimentManager detailsManager;
    DatabaseRecordsManager recordsManager;
    DataExperiment experimentDetails;            //Data about Experiment selected.
    ArrayList<String> columnList;               //Contains Column List.
    ArrayList<String> prevColumnList;           //Contains Just Previous Column List. Required while shifting Columns.
    ArrayList<ArrayList<String>> tableData;     //Contains 2D Table Data.
    ArrayList<DataRecord> experimentRecords;              //Gets Record from Database.
    long rowId;                                 //Row ID of the Selected Experiment.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_experiment_table);

        init();
        getData();
        setLayoutVisibility();
        setToolbar();
        setHeaderRecyclerView(columnList);
        setRecyclerView(tableData, columnList);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        detailsManager.close();
        recordsManager.close();
    }

    private void getData(){
        //Get Row ID of Experiment.
        rowId = getIntent().getLongExtra("rowId", -1);
        //Get Details of the Experiment.
        experimentDetails = detailsManager.getExperimentDetails(rowId);
        columnList = getColumnList(experimentDetails.columnNames);
        Log.d(LOG_TAG, "ColumnList Size : " + columnList.size());

        //Get experimentRecords of the experiment.
        experimentRecords = recordsManager.getRecords((int) experimentDetails.experimentID);
        tableData.clear();
        for (int i=0; i< experimentRecords.size(); i++){
            tableData.add(Util.splitString(experimentRecords.get(i).record, "~"));
        }

        addEmptyRow();
    }

    private void setRecyclerView(ArrayList<ArrayList<String>> tableData, ArrayList<String> columnList){
        //Crash if Span == 0
        if(columnList.size() <=0)
            tableLayoutManager = new GridLayoutManager(this, -1);
        else
            tableLayoutManager = new GridLayoutManager(this, columnList.size());

        tableRecyclerView.setLayoutManager(tableLayoutManager);
        tableRecyclerView.setHasFixedSize(true);

        tableAdapter = new TableCellAdapter(experimentDetails, experimentRecords, tableData, columnList, recordsManager,  this);
        tableRecyclerView.computedWidth = getTableWidth();
        tableRecyclerView.setAdapter(tableAdapter);

        rowAdapter = new TableRowNoAdapter(tableData.size(), this);
        rowRecyclerView.computedWidth = (int) getResources().getDimension(R.dimen.custom_table_serial_number_width);
        rowRecyclerView.setAdapter(rowAdapter);
    }

    private void setHeaderRecyclerView(ArrayList<String> columnList){
        headerAdapter = new TableHeaderAdapter(columnList, this);
        headerRecyclerView.setAdapter(headerAdapter);
    }

    /*
    **Initializations.
    * */

    private void init(){
        tableRecyclerView = (SmartRecyclerView) findViewById(R.id.rvTable);
        rowRecyclerView = (SmartRecyclerView) findViewById(R.id.rvTableNo);
        layout= (CoordinatorLayout) findViewById(R.id.clExperimentTable);
        emptyLayout = (RelativeLayout) findViewById(R.id.rlEmptyLayout);
        tableLayout = (RelativeLayout) findViewById(R.id.rlTableLayout);
        headerPadding = (ImageView) findViewById(R.id.ivHeaderPadding);
        addRow = (FloatingActionButton) findViewById(R.id.fabAddRow);

        rowLayoutManager = new LinearLayoutManager(this);
        rowLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rowRecyclerView.setLayoutManager(rowLayoutManager);
        rowRecyclerView.setHasFixedSize(true);

        columnList = new ArrayList<>();
        prevColumnList = new ArrayList<>();
        tableData = new ArrayList<>();
        experimentRecords = new ArrayList<>();

        emptyLayout.setOnClickListener(this);
        addRow.setOnClickListener(this);

        detailsManager = new DatabaseExperimentManager(this);
        detailsManager.open();
        recordsManager = new DatabaseRecordsManager(this);
        recordsManager.open();

        //Table Header
        headerViewGroup = (ViewGroup) findViewById(R.id.vTableHeader);
        headerRecyclerView = (RecyclerView) headerViewGroup.findViewById(R.id.rvTableRow);
        headerLayoutManager = new LinearLayoutManager(this);
        headerLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        headerRecyclerView.setLayoutManager(headerLayoutManager);
        headerRecyclerView.setHasFixedSize(true);

        //Table Scroll Listener.
        tableScrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                //For null parent issue when View  is recycled.
                View currentFocus = getCurrentFocus();
                if(currentFocus != null)
                    currentFocus.clearFocus();

                if(recyclerView == tableRecyclerView){
                    //Remove ScrollListener from other RecyclerView before programatically Scrolling.
                    //Else, will end up in LOOP.
                    rowRecyclerView.removeOnScrollListener(this);
                    //Scroll other RecyclerView
                    rowRecyclerView.scrollBy(dx, dy);
                    //Put the Listener back.
                    rowRecyclerView.addOnScrollListener(this);
                }
                else if(recyclerView == rowRecyclerView){
                    //Comments same as above.
                    tableRecyclerView.removeOnScrollListener(this);
                    tableRecyclerView.scrollBy(dx, dy);
                    tableRecyclerView.addOnScrollListener(this);
                }
            }
        };
        tableRecyclerView.addOnScrollListener(tableScrollListener);
        rowRecyclerView.addOnScrollListener(tableScrollListener);
    }

    private void setToolbar(){
        toolbar = (Toolbar) findViewById(R.id.tbExperimentTable);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        final Drawable upArrow = getResources().getDrawable(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        upArrow.setColorFilter(Color.parseColor("#dddddd"), PorterDuff.Mode.SRC_ATOP);
        getSupportActionBar().setHomeAsUpIndicator(upArrow);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        overflowMenu = (ImageView) findViewById(R.id.ivOverflowMenu);
        overflowMenu.setOnClickListener(this);

        toolbarTitle = (TextView) findViewById(R.id.tvToolbarTitle);
        toolbarTitle.setText(experimentDetails.title);
    }

    @Override
    public void onClick(final View view) {
        switch(view.getId()){
            case R.id.ivOverflowMenu:
                //Inflate and Show Toolbar popup.
                PopupMenu toolbarMenu = new PopupMenu(this, overflowMenu);
                toolbarMenu.getMenuInflater().inflate(R.menu.popup_experiment_table, toolbarMenu.getMenu());
                toolbarMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        if(menuItem.getItemId() == R.id.popup_add_columns){
                            launchNewColumnDialog();
                        }
                        else if(menuItem.getItemId() == R.id.popup_plot_graph){
                            launchPlotGraphDialog();
                        }
                        else if(menuItem.getItemId() == R.id.popup_export_details){
                            showExportOptions();
                        }
                        return false;
                    }
                });
                toolbarMenu.show();
                break;

            case R.id.rlEmptyLayout:
                launchNewColumnDialog();
                break;

            case R.id.fabAddRow:
                addEmptyRow();
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home){
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    //Dialog For New Experiment
    private void setNewExperimentRecyclerView(Dialog dialog, RecyclerView dialogRecyclelrView, RecyclerView.Adapter dialogAdapter,
                                 ArrayList<String> columnNames, RelativeLayout dialogLayout){
        dialogAdapter = new NewColumnAdapter(columnNames, dialog.getContext(), dialogLayout, tableRecyclerView);
        dialogRecyclelrView.setAdapter(dialogAdapter);
    }

    private void launchNewColumnDialog(){
        final Dialog dialog= new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_new_columns);

        WindowManager.LayoutParams lp= new WindowManager.LayoutParams();
        Window window= dialog.getWindow();
        lp.copyFrom(window.getAttributes());
        lp.width= WindowManager.LayoutParams.MATCH_PARENT;
        lp.height= WindowManager.LayoutParams.MATCH_PARENT;
        window.setAttributes(lp);

        final ArrayList<String> columnNames = new ArrayList<>();
        for(int i=0; i<columnList.size(); i++){
            columnNames.add(columnList.get(i));
        }

        final EditText columnName = (EditText) dialog.findViewById(R.id.etColumnName);
        final RelativeLayout dialogLayout= (RelativeLayout) dialog.findViewById(R.id.rlNewColumn);
        Button addColumn = (Button) dialog.findViewById(R.id.bAddColumn);
        Button createNewExperiment= (Button) dialog.findViewById(R.id.bNewExperiment);
        ImageView back = (ImageView) dialog.findViewById(R.id.ivBack);

        columnName.requestFocus();
        showKeyboard();

        final RecyclerView dialogRecylerView= (RecyclerView) dialog.findViewById(R.id.rvNewExperiment);
        LinearLayoutManager dialogLayoutManager = new LinearLayoutManager(dialog.getContext());
        dialogRecylerView.setLayoutManager(dialogLayoutManager);
        final RecyclerView.Adapter detailsAdapter = null;
        setNewExperimentRecyclerView(dialog, dialogRecylerView, detailsAdapter, columnNames, dialogLayout);


        //Button Listener here.
        View.OnClickListener addColumnListener= new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(columnName.getText().toString().trim().length()>0){
                    columnNames.add(columnName.getText().toString());
                    setNewExperimentRecyclerView(dialog, dialogRecylerView, detailsAdapter, columnNames, dialogLayout);
                    dialogRecylerView.scrollToPosition(columnNames.size()-1);
                    columnName.setText("");
                }
            }
        };

        View.OnClickListener createNewExperimentListener= new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateColumns(columnNames);
                dialog.dismiss();
            }
        };

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                hideKeyboard();
            }
        });

        View.OnClickListener backListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        };

        //Buttons here.
        addColumn.setOnClickListener(addColumnListener);
        createNewExperiment.setOnClickListener(createNewExperimentListener);
        back.setOnClickListener(backListener);
        dialog.show();
    }

    //Dialog For Plot Graph
    private void setPlotGraphRecyclerView(RecyclerView dialogRecyclelrView, RecyclerView.Adapter dialogAdapter,
                                 ArrayList<Pair<String, String>> graphList, RecyclerView.Adapter graphAdapter,
                                 ArrayList<DataSelectColumn> columnNames, LinearLayout graphRecyclerViewLayout,
                                 RecyclerView graphRecyclerView, TextView emptyLayout, TextView hint){
        dialogAdapter = new GraphSelectAdapter(columnNames, graphList, graphAdapter, graphRecyclerViewLayout,
                graphRecyclerView, emptyLayout, hint, this);
        dialogRecyclelrView.setAdapter(dialogAdapter);
    }

    private void launchPlotGraphDialog(){
        final Dialog dialog= new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_graph_column);

        WindowManager.LayoutParams lp= new WindowManager.LayoutParams();
        Window window= dialog.getWindow();
        lp.copyFrom(window.getAttributes());
        lp.width= WindowManager.LayoutParams.MATCH_PARENT;
        lp.height= WindowManager.LayoutParams.MATCH_PARENT;
        window.setAttributes(lp);

        //Add Array for Horizontal ReyclerView.
        final ArrayList<DataSelectColumn> columnName = new ArrayList<>();
        for(int i=0; i<columnList.size(); i++){
            columnName.add(new DataSelectColumn(columnList.get(i)));
        }
        final ArrayList<Pair<String,String>> graphList = new ArrayList<>();

        Button done = (Button) dialog.findViewById(R.id.bDone);
        ImageView back = (ImageView) dialog.findViewById(R.id.ivBack);
        TextView emptyLayout = (TextView) dialog.findViewById(R.id.tvEmptyLayout);
        LinearLayout graphRecyclerViewLayout= (LinearLayout) dialog.findViewById(R.id.llGraphRecyclerView);
        TextView hint = (TextView) dialog.findViewById(R.id.tvHint);
        graphRecyclerViewLayout.setVisibility(View.GONE);

        //For displaying list of Graphs that'll be plotted.
        final RecyclerView graphRecyclerView= (RecyclerView) dialog.findViewById(R.id.rvGraphList);
        LinearLayoutManager graphLayoutManager = new LinearLayoutManager(dialog.getContext());
        graphRecyclerView.setLayoutManager(graphLayoutManager);
        final RecyclerView.Adapter graphAdapter = new GraphListAdapter(graphList, graphRecyclerViewLayout, emptyLayout, this);
        graphRecyclerView.setAdapter(graphAdapter);

        //For adding columnList to GraphList.
        final RecyclerView columnRecyclerView= (RecyclerView) dialog.findViewById(R.id.rvGraphColumn);
        LinearLayoutManager columnLayoutManager = new LinearLayoutManager(dialog.getContext());
        columnLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        columnRecyclerView.setLayoutManager(columnLayoutManager);
        final RecyclerView.Adapter columnAdapter = null;
        setPlotGraphRecyclerView(columnRecyclerView, columnAdapter, graphList, graphAdapter, columnName,
                graphRecyclerViewLayout, graphRecyclerView, emptyLayout, hint);

        //Button Listener here.
        View.OnClickListener doneListener= new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(graphList.size() > 0){
                    Intent iPlotGraph = new Intent(view.getContext(), PlotGraph.class);

                    //Attach Selected Graph Data.
                    iPlotGraph.putExtra(Util.EXPERIMENT_ID, rowId);
                    iPlotGraph.putExtra(Util.GRAPH_LIST_SIZE, graphList.size());
                    for(int i=0; i<graphList.size(); i++){
                        iPlotGraph.putExtra(Util.GRAPH_LIST + i, graphList.get(i).first + "~" + graphList.get(i).second);
                    }

                    startActivity(iPlotGraph);
                    dialog.dismiss();
                }
                else{
                    showToast("No Graphs to  be Plotted.");
                }
            }
        };

        View.OnClickListener backListener= new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        };

        //Add listeners here..
        done.setOnClickListener(doneListener);
        back.setOnClickListener(backListener);
        dialog.show();
        hideKeyboard();
    }

    private void showToast(String message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    //APIs for Soft Keyboard.
    private void showKeyboard(){
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
    }
    private void hideKeyboard(){
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(tableRecyclerView.getWindowToken(), 0);
    }

    //Database APIs
    private void updateColumns(ArrayList<String> columnNames){
        experimentDetails.columnNames = getColumnString(columnNames);
        prevColumnList = columnList;
        columnList = columnNames;
        Log.d(LOG_TAG, "columnNames: " + experimentDetails.columnNames + "columnNames.size : " + columnNames.size());
        detailsManager.updateEntry(experimentDetails);
        //Show Table if Columns present in  table.
        setLayoutVisibility();
        //Update the table.
        refreshTable();
    }

    //Updates the Table.
    private void refreshTable(){
        ArrayList<ArrayList<String>> tempTableData = new ArrayList<>(tableData.size());

        for(int i=0; i<columnList.size(); i++){
            if(tempTableData.isEmpty()) {
                //First Element of Each Row.
                if (prevColumnList.contains(columnList.get(i))) {
                    //Move the Column Data.
                    //Get position of Column in Prev Table Arrangement.
                    int columnPos = prevColumnList.indexOf(columnList.get(i));
                    ArrayList<String> data = null;
                    //Copy Prev Column Data to new Position.
                    for (int j = 0; j < tableData.size(); j++) {
                        data = new ArrayList<>();
                        data.add(tableData.get(j).get(columnPos));
                        tempTableData.add(data);
                    }
                } else {
                    //Initialise Empty Values.
                    ArrayList<String> data = null;
                    for (int j = 0; j < tableData.size(); j++) {
                        data = new ArrayList<>();
                        data.add("");
                        tempTableData.add(data);
                    }
                }
            }
            else {
                //For  Rest of elements.
                if (prevColumnList.contains(columnList.get(i))) {
                    int columnPos = prevColumnList.indexOf(columnList.get(i));
                    //Copy Prev Values to new Position.
                    for (int j = 0; j < tableData.size(); j++)
                        tempTableData.get(j).add(tableData.get(j).get(columnPos));
                }
                else {
                    //Assign Empty Values
                    for (int j = 0; j < tableData.size(); j++)
                        tempTableData.get(j).add("");
                }
            }
        }

        //Free Memory & Update UI.
        tableData.clear();
        tableData = tempTableData;
        setRecyclerView(tableData, columnList);
        setHeaderRecyclerView(columnList);

        //Update Database for Table  Data.
    }

    //String Slicing for Columns
    private ArrayList<String> getColumnList(String columnNames){
        ArrayList<String> columns = new ArrayList<>();

        //Empty Columns.
        if(columnNames.trim().length() <=0)
            return columns;

        //Slicing ColumnList String.
        String[] columnArray = columnNames.split("~");

        for(int i=0; i<columnArray.length; i++){
            columns.add(columnArray[i]);
        }
        return columns;
    }
    private String getColumnString(ArrayList<String> columnNames){
        String columnString = "";

        if(columnNames.size()==1){
            columnString = columnNames.get(0);
        }
        else if(columnNames.size() > 1){
            columnString = columnNames.get(0);
            for(int i=1; i<columnNames.size(); i++){
                columnString += "~" + columnNames.get(i);
            }
        }

        return columnString;
    }

    //If Table has Columns -> Show Table.
    //Else show  options for adding Columns.
    private void setLayoutVisibility(){
        if(columnList.size()>0){
            tableLayout.setVisibility(View.VISIBLE);
            rowRecyclerView.setVisibility(View.VISIBLE);
            headerPadding.setVisibility(View.VISIBLE);
            emptyLayout.setVisibility(View.GONE);
        }
        else{
            tableLayout.setVisibility(View.GONE);
            rowRecyclerView.setVisibility(View.GONE);
            headerPadding.setVisibility(View.GONE);
            emptyLayout.setVisibility(View.VISIBLE);
        }
    }

    private int getTableWidth(){
        float sp = (getResources().getDimension(R.dimen.custom_table_cell_width) * columnList.size());
        return (int)sp;
    }

    //Adds Empty row to Table.
    private void addEmptyRow(){
        ArrayList<String> data = new ArrayList<>();
        for (int i=0; i<columnList.size(); i++)
            data.add("");
        tableData.add(data);
        experimentRecords.add(new DataRecord(experimentDetails.experimentID, Util.getString(data, "~")));
        setRecyclerView(tableData, columnList);
    }

    //Create PDF File & Save Data.
    private void exportTableAsPdf() {
        if (!createFolder()) {
            showToast("Unable to export PDF file. Storage permission error.");
            return;
        }

        String root = Environment.getExternalStorageDirectory().toString() + File.separator +
                getString(R.string.app_name) + File.separator + experimentDetails.title.replace(" ", "");

        String pdfPath = root + File.separator + experimentDetails.title.replace(" ", "") + ".pdf";

        Document doc = new Document();
        PdfWriter docWriter = null;

        try {
            //Custom Fonts for PDF
            Font fontTitle = new Font(FontFamily.TIMES_ROMAN, 30, Font.BOLD, new BaseColor(100, 100, 100));
            Font fontTableHeading = new Font(FontFamily.TIMES_ROMAN, 20, Font.BOLD, new BaseColor(0, 0, 0));
            Font fontTableContent = new Font(FontFamily.TIMES_ROMAN, 16);

            //file path
            docWriter = PdfWriter.getInstance(doc, new FileOutputStream(pdfPath));

            //Add metadata
            doc.addAuthor(getString(R.string.app_name));
            doc.addCreationDate();
            doc.addProducer();
            doc.addCreator(getString(R.string.app_name));
            doc.addTitle(experimentDetails.title);
            doc.setPageSize(PageSize.A4);

            doc.open();

            //Paragraph = Heading + Table.
            Paragraph pdfContent = new Paragraph(experimentDetails.title, fontTitle);
            pdfContent.setAlignment(Element.ALIGN_CENTER);

            //Add table
            PdfPTable table = new PdfPTable(columnList.size());
            // set table width a percentage of the page width
            table.setWidthPercentage(90f);

            //Add Column Headings
            for(int i=0; i<columnList.size(); i++){
                insertCell(table, columnList.get(i), Element.ALIGN_CENTER, 1, fontTableHeading);
            }

            //Add Table Data
            for (int i = 0; i < tableData.size(); i++) {
                //Add non-empty rows only.
                if(!Util.isEmptyRow(experimentRecords.get(i).record)) {
                    for (int j = 0; j < columnList.size(); j++) {
                        insertCell(table, tableData.get(i).get(j), Element.ALIGN_CENTER, 1, fontTableContent);
                    }
                }
            }

            pdfContent.add(table);

            //Add the paragraph to the document
            doc.add(pdfContent);

            //Add Image.
            for(int i=1; i<4; i++){
                Bitmap bmp = null;
                if (i==1)
                    bmp = BitmapFactory.decodeResource(getResources(), R.drawable.img1);
                else if(i==2)
                    bmp = BitmapFactory.decodeResource(getResources(), R.drawable.img2);
                else if(i==3)
                    bmp = BitmapFactory.decodeResource(getResources(), R.drawable.img3);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
                Image signature;
                signature = Image.getInstance(stream.toByteArray());
                signature.setAlignment(Element.ALIGN_MIDDLE);
                signature.scalePercent(100f);
                signature.scaleAbsoluteWidth(350);
                signature.scaleAbsoluteHeight(350);
                doc.add(signature);
            }

            showToast("Data Successfully Exported to " + getString(R.string.app_name) + File.separator +
                    experimentDetails.title.replace(" ", "") + File.separator + experimentDetails.title.replace(" ", "") + ".pdf");

        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (doc != null) {
                doc.close();
            }
            if (docWriter != null) {
                docWriter.close();
            }
        }
    }

    private void insertCell(PdfPTable table, String text, int align, int colSpan, Font font){
        PdfPCell cell = new PdfPCell(new Phrase(text.trim(), font));
        cell.setHorizontalAlignment(align);
        cell.setColspan(colSpan);

        if(text.trim().equalsIgnoreCase("")){
            cell.setMinimumHeight(10f);
        }
        table.addCell(cell);

    }

    //Creates Excel File & Save Data.
    private void exportTableAsExcel(){
        if(!createFolder()){
            showToast("Unable to export Excel file. Storage permission error.");
            return;
        }

        File excelDirectory = new File(Environment.getExternalStorageDirectory().toString() + File.separator +
                getString(R.string.app_name) +  File.separator + experimentDetails.title.replace(" ", ""));

        try {
            File file = new File(excelDirectory, experimentDetails.title.replace(" ","") + ".xls");
            WorkbookSettings wbSettings = new WorkbookSettings();
            wbSettings.setLocale(new Locale("en", "EN"));
            WritableWorkbook workbook;
            workbook = Workbook.createWorkbook(file, wbSettings);
            //Excel sheet name. 0 represents first sheet
            WritableSheet sheet = workbook.createSheet(experimentDetails.title.replace(" ", ""), 0);
            // column and row
            for(int i=0; i<columnList.size(); i++){
                sheet.addCell(new Label(i,0,columnList.get(i)));
            }

            for(int i=0; i<tableData.size(); i++){
                //Add non-empty rows only.
                if(!Util.isEmptyRow(experimentRecords.get(i).record)) {
                    for (int j = 0; j < columnList.size(); j++) {
                        sheet.addCell(new Label(j, i + 1, tableData.get(i).get(j)));
                    }
                }
            }

            //Closing Workbook
            workbook.write();
            workbook.close();
            showToast("Data Successfully Exported to " + getString(R.string.app_name) + File.separator +
                    experimentDetails.title.replace(" ", "") + File.separator + experimentDetails.title.replace(" ", "") + ".xls");
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    //Creates CSV File & Save Data.
    private void exportTableAsCsv(){
        if(!createFolder()){
            showToast("Unable to export CSV file. Storage permission error.");
            return;
        }

        File fileDirectory = new File(Environment.getExternalStorageDirectory().toString() + File.separator +
                getString(R.string.app_name) +  File.separator + experimentDetails.title.replace(" ", ""));

        try {
            File csvFile = new File(fileDirectory, experimentDetails.title.replace(" ","") + ".csv");
            FileWriter csvWriter = new FileWriter(csvFile);
            BufferedWriter csvBuffer = new BufferedWriter(csvWriter);

            //Write column names.
            csvBuffer.write(columnList.get(0));
            for(int i=1; i<columnList.size(); i++){
                csvBuffer.write("," + columnList.get(i));
            }
            csvBuffer.newLine();

            for(int i=0; i<tableData.size(); i++){
                //Add non-empty rows only.
                if(!Util.isEmptyRow(experimentRecords.get(i).record)) {
                    csvBuffer.write(tableData.get(i).get(0));
                    for (int j = 1; j < columnList.size(); j++) {
                        csvBuffer.write("," + tableData.get(i).get(j));
                    }
                    csvBuffer.newLine();
                }
            }

            //Flush data.
            csvBuffer.flush();
            showToast("Data successfully exported to "  + getString(R.string.app_name) + File.separator +
                    experimentDetails.title.replace(" ","") + File.separator + experimentDetails.title.replace(" ","") + ".csv");
        }
        catch(Exception e){
            e.printStackTrace();
            showToast("Unable to export CSV file.");
        }
    }

    //Checks & Creates Folder for saving Files for this experiment.
    private boolean createFolder(){
        File folder = new File(Environment.getExternalStorageDirectory().toString() + "/" + getString(R.string.app_name));

        boolean folderCreated = true;
        if(!folder.exists()){
            folderCreated = folder.mkdir();
        }

        if(!folderCreated)
            return false;

        File innerFolder = new File(Environment.getExternalStorageDirectory().toString() + File.separator +
                getString(R.string.app_name) +  File.separator + experimentDetails.title.replace(" ", ""));

        if(!innerFolder.exists()){
            folderCreated = innerFolder.mkdir();
        }
        return folderCreated;
    }

    //Displays Export Options
    private void showExportOptions(){
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.drawable.icon_export_grey);
        builder.setTitle("Export Table as : ");

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.select_dialog_singlechoice);
        adapter.add("PDF");
        adapter.add("Excel");
        adapter.add("CSV");

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                switch (i){
                    case 0:
                        exportTableAsPdf();
                        break;
                    case 1:
                        exportTableAsExcel();
                        break;
                    case 2:
                        exportTableAsCsv();
                        break;
                }
            }
        });
        builder.show();
    }
}
