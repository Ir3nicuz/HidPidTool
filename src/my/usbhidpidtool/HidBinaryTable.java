
package my.usbhidpidtool;

import static Hid4Java.HidWorkbench.*;
import java.io.File;
import java.util.ArrayList;
import java.util.ListIterator;

public final class HidBinaryTable extends HidBaseTable {
    private static final String STRING_SEPERATOR_ITEM = " | ";
    
    private final ArrayList<BinaryTableItem> itemList = new ArrayList<>();
    private final String sFalseColumnText = "bFalseText";
    private final String sTrueColumnText = "bTrueText";
    private final String sIndexColumnText = "bBit";
    private final int iHeaderLine = 0;
        
    public HidBinaryTable(String sName, HidTableType type, String sDelimiter, File file) throws RuntimeException{
        super(sName, type, file);
        ArrayList<ArrayList<String>> dataTable = parseSeperatedTextFile(sDelimiter, file);
        decodeBinaryItemTable(dataTable);
    }
    
    public final class BinaryTableItem{
        private final String sFalseText;
        private final String sTrueText;
        private final int iBitPosition;
        
        public BinaryTableItem(String sFalseText, String sTrueText, int iBitPosition){
            this.sFalseText = sFalseText;
            this.sTrueText = sTrueText;
            this.iBitPosition = iBitPosition;
        }
        public final String getRepresentation(int iValue){
            int iState = ((iValue >>> this.iBitPosition) & 0x01);
            if (iState > 0) {
                return sTrueText;
            } else {
                return sFalseText;
            }
        }
        @Override
        public final String toString(){
            return String.format("%s / %s", this.sFalseText, this.sTrueText);
        }
        public final String getFalseText(){
            return this.sFalseText;
        }
        public final String getTrueText(){
            return this.sTrueText;
        }
        public final int getBitPosition(){
            return this.iBitPosition;
        }
    }
    
    // publics
    public final ArrayList<BinaryTableItem> getItemList(){
        return itemList;
    }
    @Override
    public final String findRepresentation(int iValue, int iCondition){
        StringBuilder sRepresentation = new StringBuilder();
        ListIterator itemIterator = itemList.listIterator();
        while(itemIterator.hasNext()){
            BinaryTableItem item = (BinaryTableItem)itemIterator.next();
            sRepresentation.append(item.getRepresentation(iValue));
            if (itemIterator.hasNext()){
                sRepresentation.append(STRING_SEPERATOR_ITEM);
            }
        }
        return sRepresentation.toString();
    }
    
    // privates
    private void decodeBinaryItemTable(ArrayList<ArrayList<String>> dataTable) throws RuntimeException{
        try{
            int iFalseColumn = dataTable.get(iHeaderLine).indexOf(sFalseColumnText);
            int iTrueColumn = dataTable.get(iHeaderLine).indexOf(sTrueColumnText);
            int iIndexColumn = dataTable.get(iHeaderLine).indexOf(sIndexColumnText);
            if(iFalseColumn < 0){throw new NumberFormatException(sFalseColumnText + " not found");}
            if(iTrueColumn < 0){throw new NumberFormatException(sTrueColumnText + " not found");}
            if(iIndexColumn < 0){throw new NumberFormatException(sIndexColumnText + " not found");}
            dataTable.remove(iHeaderLine);
            dataTable.forEach(line -> {
                try{
                    String sIndex = line.get(iIndexColumn);
                    String sFalseText = line.get(iFalseColumn);
                    String sTrueText = line.get(iTrueColumn);
                    if (sIndex.equals("")) {throw new NumberFormatException(String.format("Item %s empty", sIndexColumnText));}
                    int iIndex = Integer.parseInt(sIndex);
                    if (!isRangeValidBitField(iIndex)) {throw new NumberFormatException(String.format("Item %s value %d invalid", sIndexColumnText, iIndex));}
                    itemList.add(new BinaryTableItem(sFalseText, sTrueText, iIndex));
                } catch (RuntimeException ex) {
                    super.addError(String.format("File %s / line %d: Format error: %s", super.getFile().toString(), dataTable.indexOf(line), ex.toString()));
                }
            });
        } catch(RuntimeException ex){
            throw new RuntimeException(String.format("File %s: bad format: %s", super.getFile().toString(),  ex.toString()));
        }
    }
}
