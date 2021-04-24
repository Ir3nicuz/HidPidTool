
package my.usbhidpidtool;

import static Hid4Java.HidWorkbench.*;
import java.io.File;
import java.util.ArrayList;

public final class HidUnitTable extends HidBaseTable{
    private final ArrayList<UnitTableItem> itemList = new ArrayList<>();
    private final String sNameColumnText = "bQuickUnitText";
    private final String sValueColumnText = "bQuickUnitValue";
    private final int iHeaderLine = 0;
        
    public HidUnitTable(String sName, HidTableType type, String sDelimiter, File file) throws RuntimeException{
        super(sName, type, file);
        ArrayList<ArrayList<String>> dataTable = parseSeperatedTextFile(sDelimiter, file);
        decodeUnitItemTable(dataTable);
    }
    
    public final class UnitTableItem{
        private final String sName;
        private final int iValue;
        
        public UnitTableItem(String sName, int iValue){
            this.sName = sName;
            this.iValue = iValue;
        }
        @Override
        public final String toString(){
            return String.format("%s / %08X", this.sName, this.iValue);
        }
        public final String getName(){
            return this.sName;
        }
        public final int getValue(){
            return this.iValue;
        }
    }
    
    public final ArrayList<UnitTableItem> getItemList(){
        return itemList;
    }
    public final int findItemValue(String sItemName){
        Integer iId = findEntryId(sItemName);
        return (iId != null) ? itemList.get(iId).getValue() : INT_VALUE_DEFAULT_DATAVALUE;
    }
    public final String findItemName(int iValue){
        Integer iId = findEntryId(iValue);
        return (iId != null) ? itemList.get(iId).getName() : null;
    }
    @Override
    public final String findRepresentation(int iValue, int iCondition){
        String sRepresentation;
        Integer iId = findEntryId(iValue);
        if (iId != null){
            sRepresentation = itemList.get(iId).getName();
        } else {
            sRepresentation = String.format("0x%08X", iValue);
        }
        return sRepresentation;
    }
    
    private Integer findEntryId(String sItemName){
        for (int i = 0;i < itemList.size();i++){
            UnitTableItem item = itemList.get(i);
            if (item.getName().equals(sItemName)){
                return  i;
            }
        }
        return null;
    } 
    private Integer findEntryId(int iValue){
        for (int i = 0;i < itemList.size();i++){
            UnitTableItem item = itemList.get(i);
            if (item.getValue() == iValue){
                return  i;
            }
        }
        return null;
    }
    private void decodeUnitItemTable(ArrayList<ArrayList<String>> dataTable) throws RuntimeException{
        try{
            int iNameColumn = dataTable.get(iHeaderLine).indexOf(sNameColumnText);
            int iValueColumn = dataTable.get(iHeaderLine).indexOf(sValueColumnText);
            if(iNameColumn < 0){throw new NumberFormatException(sNameColumnText + " not found");}
            if(iValueColumn < 0){throw new NumberFormatException(sValueColumnText + " not found");}
            dataTable.remove(iHeaderLine);
            dataTable.forEach(line -> {
                try{
                    String sName = line.get(iNameColumn);
                    String sValue = line.get(iValueColumn);
                    if (sName.equals("")) {throw new NumberFormatException(String.format("Item %s empty", sNameColumnText));}
                    if (sValue.equals("")) {throw new NumberFormatException(String.format("Item %s empty", sValueColumnText));}
                    int iValue = Integer.parseInt(sValue, 16);
                    if (iValue < 0) {throw new NumberFormatException(String.format("Item %s value %d negative", sValueColumnText, iValue));}
                    itemList.add(new UnitTableItem(sName, iValue));
                } catch (RuntimeException ex) {
                    super.addError(String.format("File %s / line %d: Format error: %s", super.getFile().toString(), dataTable.indexOf(line), ex.toString()));
                }
            });
        } catch(RuntimeException ex){
            throw new RuntimeException(String.format("File %s: bad format: %s", super.getFile().toString(),  ex.toString()));
        }
    }
}