
package my.usbhidpidtool;

import Hid4Java.HidReport;
import Hid4Java.HidReportElement;
import static Hid4Java.HidWorkbench.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HidElementTable extends HidReport {
    private final ArrayList<String> fileLoadErrors = new ArrayList<>();
    private String sName = "New";
    private File file = null;
    private boolean bUnstored = true;
    private final HidFormatTable hidFormatTable;
    
    public HidElementTable(HidFormatTable hidFormatTable) {
        this.hidFormatTable = hidFormatTable;
    }
    public HidElementTable(File sourceFile, HidFormatTable hidFormatTable) throws IOException,SecurityException,NullPointerException {
        this.hidFormatTable = hidFormatTable;
        load(sourceFile);
    }
    
    public final void load(File sourceFile) throws IOException,SecurityException,IllegalArgumentException {
        ArrayList<ArrayList<Byte>> elementList = decodeReportFile(sourceFile);
        this.fileLoadErrors.clear();
        super.clear();
        elementList.forEach(element -> {
            try{
                super.add(new HidElementTableRow(element, hidFormatTable));
            }catch(IllegalArgumentException ex){
                fileLoadErrors.add(ex.toString());
            }
        });
        notifyTableChange();
        this.file = sourceFile;
        this.sName = sourceFile.getName();
    }
    public final File save() throws IOException,IllegalArgumentException{
        if (this.file == null) {throw new IOException("No filename specified");}
        save(this.file);
        return this.file;
    }
    public final void save(File targetFile) throws IOException,IllegalArgumentException{
        encodeReportFile(targetFile, super.getList());
        this.bUnstored = false;
        this.sName = targetFile.getName();
        this.file = targetFile;
    }
    public final String getName(){
        return this.sName;
    }
    public final File getFile(){
        return this.file;
    }
    public final int size(){
        return super.getElementCount();
    }
    
    public final ArrayList<String> getLoadErrors(){
        return fileLoadErrors;
    }
    public final boolean hasError(){
        return !fileLoadErrors.isEmpty();
    }
    public final boolean isUnstored(){
        return this.bUnstored;
    }
    public final void addRow(int iIndex, HidElementTableRow row){
        try{
            super.add(iIndex, row);
        } catch(IndexOutOfBoundsException ex){
            super.add(row);
        }
        notifyTableChange();
    }
    public final void addRow(HidElementTableRow row){
        super.add(row);
        notifyTableChange();
    }
    public final int removeRows(List<Integer> indexList){
        int iResult = super.remove(indexList);
        if (iResult > 0){notifyTableChange();}
        return iResult;
    }
    public final int removeAllRows(){
        int iResult = super.removeAll();
        if (iResult > 0){notifyTableChange();}
        return iResult;
    }
    public final int moveRowsUp(List<Integer> indexList){
        int iResult = super.moveIndexUp(indexList);
        if (iResult > 0){notifyTableChange();}
        return iResult;
    }
    public final int moveRowsDown(List<Integer> indexList){
        int iResult = super.moveIndexDown(indexList);
        if (iResult > 0){notifyTableChange();}
        return iResult;
    }
    public final HidElementTableRow getElementRow(int iElement){
        HidReportElement row = super.getElement(iElement);
        return (row != null) ? (HidElementTableRow)row : null;
    }
    public final int addElementRowsAt(int iRow, ArrayList<HidElementTableRow> rowList){
        var oContainer = new Object(){int iRowCount = 0;};
        rowList.forEach(row -> {
            if (super.add((iRow + oContainer.iRowCount), row)){
                oContainer.iRowCount++;
            }
        });
        if (oContainer.iRowCount > 0){notifyTableChange();}
        return oContainer.iRowCount;
    }
    public final boolean addElementRowAt(int iRow, HidElementTableRow row){
        boolean bResult = super.add(iRow, row);
        if (bResult){notifyTableChange();}
        return bResult;
    }
    public final boolean replaceElementRowAt(int iRow, HidElementTableRow row){
        boolean bResult = super.replace(iRow, row);
        if (bResult){notifyTableChange();}
        return bResult;
    }
    
    private void notifyTableChange(){
        refreshIntend();
        refreshElementUsage();
        this.bUnstored = true;
    }
    private void refreshIntend(){
        var oWrapper = new Object(){int iActualIndent = 0;};
        getList().forEach(element -> {
            HidElementTableRow row = (HidElementTableRow)element;
            if (row.decreasesIndent()) {oWrapper.iActualIndent--;}
            row.setIndent(oWrapper.iActualIndent);
            if (row.increasesIndent()) {oWrapper.iActualIndent++;} 
        });
    }
    private void refreshElementUsage(){
        var oWrapperObject = new Object(){int iParentUsagePage = INT_VALUE_DEFAULT_DATAVALUE;};
        getList().forEach(element -> {
            switch(element.getItemIdentifier()){
                case usage_page:oWrapperObject.iParentUsagePage = element.getValue();break;
                case usage:case usage_minimum:case usage_maximum:
                    HidElementTableRow row = (HidElementTableRow)element;
                    row.refreshUsageName(hidFormatTable, oWrapperObject.iParentUsagePage);
                    break;
                default:break;
            }
        });
    }
}
