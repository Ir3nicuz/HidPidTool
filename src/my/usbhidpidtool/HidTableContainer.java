
package my.usbhidpidtool;

import java.awt.Color;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public class HidTableContainer extends JScrollPane {
    private static final String STRING_TITLE_COLUMN_NUMBER = "Nr";
    private static final String STRING_TITLE_COLUMN_ITEMNAME = "Name";
    private static final String STRING_TITLE_COLUMN_ITEMVALUE = "Value";
    private static final String STRING_TITLE_COLUMN_ITEMTYPE = "Type";
    private static final String STRING_TITLE_COLUMN_BYTEVALUE = "Hex-Bytes";
    
    private static final Color COLOR_BACKGROUND = Color.white;
    
    private static final int INT_WIDTH_COLUMN_NUMBER = 30;
    private static final int INT_WIDTH_COLUMN_ITEMNAME = 200;
    private static final int INT_WIDTH_COLUMN_ITEMVALUE = 600;
    private static final int INT_WIDTH_COLUMN_ITEMTYPE = 50;
    private static final int INT_WIDTH_COLUMN_BYTEVALUE = 100;
    
    private final HidElementTable dataTable;
    private final JTable visualTable;
    private final DefaultTableModel model;
    
    public HidTableContainer(KeyListener keylistener, MouseListener mouseListener, HidFormatTable hidFormatTable) {
        super();
        this.visualTable = new JTable();
        this.visualTable.addKeyListener(keylistener);
        this.visualTable.addMouseListener(mouseListener);
        this.setViewportView(visualTable);
        this.model = (DefaultTableModel)visualTable.getModel();
        this.model.setColumnIdentifiers(new Object[]{
            STRING_TITLE_COLUMN_NUMBER, STRING_TITLE_COLUMN_ITEMNAME, STRING_TITLE_COLUMN_ITEMVALUE
            , STRING_TITLE_COLUMN_ITEMTYPE, STRING_TITLE_COLUMN_BYTEVALUE});
        this.visualTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        this.visualTable.getColumn(STRING_TITLE_COLUMN_NUMBER).setMinWidth(INT_WIDTH_COLUMN_NUMBER);
        this.visualTable.getColumn(STRING_TITLE_COLUMN_ITEMNAME).setMinWidth(INT_WIDTH_COLUMN_ITEMNAME);
        this.visualTable.getColumn(STRING_TITLE_COLUMN_ITEMVALUE).setMinWidth(INT_WIDTH_COLUMN_ITEMVALUE);
        this.visualTable.getColumn(STRING_TITLE_COLUMN_ITEMTYPE).setMinWidth(INT_WIDTH_COLUMN_ITEMTYPE);
        this.visualTable.getColumn(STRING_TITLE_COLUMN_BYTEVALUE).setMinWidth(INT_WIDTH_COLUMN_BYTEVALUE);
        this.visualTable.setDefaultEditor(Object.class, null);
        this.visualTable.setBackground(COLOR_BACKGROUND);
        this.dataTable = new HidElementTable(hidFormatTable);
    }
    public HidTableContainer(KeyListener keylistener, MouseListener mouseListener, HidFormatTable hidFormatTable, File sourceFile) 
            throws IOException,SecurityException,IllegalArgumentException{
        super();
        this.visualTable = new JTable();
        this.visualTable.addKeyListener(keylistener);
        this.visualTable.addMouseListener(mouseListener);
        this.setViewportView(visualTable);
        this.model = (DefaultTableModel)visualTable.getModel();
        this.model.setColumnIdentifiers(new Object[]{
            STRING_TITLE_COLUMN_NUMBER, STRING_TITLE_COLUMN_ITEMNAME, STRING_TITLE_COLUMN_ITEMVALUE
            , STRING_TITLE_COLUMN_ITEMTYPE, STRING_TITLE_COLUMN_BYTEVALUE});
        this.visualTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        this.visualTable.getColumn(STRING_TITLE_COLUMN_NUMBER).setMinWidth(INT_WIDTH_COLUMN_NUMBER);
        this.visualTable.getColumn(STRING_TITLE_COLUMN_ITEMNAME).setMinWidth(INT_WIDTH_COLUMN_ITEMNAME);
        this.visualTable.getColumn(STRING_TITLE_COLUMN_ITEMVALUE).setMinWidth(INT_WIDTH_COLUMN_ITEMVALUE);
        this.visualTable.getColumn(STRING_TITLE_COLUMN_ITEMTYPE).setMinWidth(INT_WIDTH_COLUMN_ITEMTYPE);
        this.visualTable.getColumn(STRING_TITLE_COLUMN_BYTEVALUE).setMinWidth(INT_WIDTH_COLUMN_BYTEVALUE);
        this.visualTable.setDefaultEditor(Object.class, null);
        this.visualTable.setBackground(COLOR_BACKGROUND);
        this.dataTable = new HidElementTable(hidFormatTable);
        this.load(sourceFile);
    }
    
    public final void load(File sourceFile) throws IOException,SecurityException,IllegalArgumentException{
        this.dataTable.load(sourceFile);
        refreshTable(0);
    }
    public final void save(File targetFile) throws IOException,IllegalArgumentException{
        this.dataTable.save(targetFile);
    }
    public final File save() throws IOException,IllegalArgumentException{
        return this.dataTable.save();
    }
    public final void refreshTable(int iIndexOffset){
        int[] iRows = visualTable.getSelectedRows();
        model.setRowCount(0);
        StringBuilder sNameIndented = new StringBuilder();
        this.dataTable.getList().forEach(element -> {
            HidElementTableRow row = (HidElementTableRow)element;
            sNameIndented.setLength(0);
            for (int iIndent = row.getIndent(); iIndent > 0;iIndent--){sNameIndented.append("    ");}
            sNameIndented.append(row.getElementName());
            model.addRow(new Object[]{model.getRowCount() + 1, sNameIndented.toString(), row.getElementUsage(), row.getElementType(), row.getBytes()});
        });
        int iLastRow = visualTable.getRowCount() - 1;
        for (int iRow : iRows) {
            iRow += iIndexOffset;
            if (iRow < 0) {iRow = 0;}else if(iRow > iLastRow){iRow = iLastRow;}
            visualTable.getSelectionModel().addSelectionInterval(iRow, iRow);
        }
    }
    public final String getTableName(){
        return this.dataTable.getName();
    }
    public final boolean isUnstored(){
        return this.dataTable.isUnstored();
    }
    public final boolean hasError(){
        return this.dataTable.hasError();
    }
    public final ArrayList<String> getErrors(){
        return this.dataTable.getLoadErrors();
    }
    public final File getFile(){
        return dataTable.getFile();
    }
    public final int removeSelectedRows(){
        int iResult = dataTable.removeRows(arrayToSortedList(visualTable.getSelectedRows(), true));
        if (iResult > 0){refreshTable(0);}
        return iResult;
    }
    public final int removeAllRows(){
        int iResult = dataTable.removeAllRows();
        if (iResult > 0){refreshTable(0);}
        return iResult;
    }
    public final int moveSelectedRowsUp(){
        int iResult = dataTable.moveRowsDown(arrayToSortedList(visualTable.getSelectedRows(), false));
        if (iResult > 0){refreshTable(-1);}
        return iResult;
    }
    public final int moveSelectedRowsDown(){
        int iResult = dataTable.moveRowsUp(arrayToSortedList(visualTable.getSelectedRows(), true));
        if (iResult > 0){refreshTable(1);}
        return iResult;
    }
    public final ArrayList<HidElementTableRow> getSelectedElementRows(){
        ArrayList<HidElementTableRow> rowList = new ArrayList<>();
        List<Integer> indexList = arrayToSortedList(visualTable.getSelectedRows(), false);
        indexList.stream().map(iRow -> dataTable.getElementRow(iRow)).filter(row -> (row != null)).forEachOrdered(row -> {
            rowList.add(row);
        });
        return rowList;
    }
    public final int insertElementRowsAtSelection(ArrayList<HidElementTableRow> rowList, boolean bAfter){
        int iRow = getInsertIndex(bAfter);
        int iRowCount = dataTable.addElementRowsAt(iRow, rowList);    
        if (iRowCount > 0){refreshTable((bAfter ? iRowCount : 0));}
        return iRowCount;
    }
    public final boolean insertElementRowAtSelection(HidElementTableRow row, boolean bAfter){
        int iRow = getInsertIndex(bAfter);
        boolean bResult = dataTable.addElementRowAt(iRow, row);    
        if (bResult){refreshTable((bAfter ? 1 : 0));}
        return bResult;
    }
    public final int changeElementRowsAtSelection(HidElementEditor editor){
        var oContainer = new Object(){int iResult = 0;};
        List<Integer> rowList = arrayToSortedList(visualTable.getSelectedRows(), false);
        rowList.forEach(iRow -> {
            HidElementTableRow row = dataTable.getElementRow(iRow);
            if (row.hasData()) {
                if (editor.showChangeDialog(row, dataTable.findParentUsagePageValue(iRow)) == HidElementEditor.CONFIRMED) {
                    dataTable.replaceElementRowAt(iRow, editor.getCreatedRow());
                    oContainer.iResult++;
                }
            }
        });
        if (oContainer.iResult > 0){refreshTable(0);}
        return oContainer.iResult;
    }
    public final int findParentUsagePageValue(boolean bAfter){
        int iRow = getInsertIndex(bAfter);
        return dataTable.findParentUsagePageValue(iRow);
    }
    
    private List<Integer> arrayToSortedList(int[] array, boolean bReverse){
        List<Integer> list = Arrays.stream(array).boxed().collect(Collectors.toList());
        if(bReverse){
            Collections.sort(list, Collections.reverseOrder());
        }else{
            Collections.sort(list);
        }
        return list;
    }
    private int getInsertIndex(boolean bAfter){
        List<Integer> indexList = arrayToSortedList(visualTable.getSelectedRows(), bAfter);
        int iRow;
        if (indexList.size() > 0){
            iRow = indexList.get(0) + (bAfter ? 1 : 0);
        } else {
            iRow = (bAfter ? dataTable.size() : 0);
        }
        return iRow;
    }
}
