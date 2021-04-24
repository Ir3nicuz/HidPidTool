
package my.usbhidpidtool;

import static Hid4Java.HidWorkbench.*;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.ListIterator;

public final class HidSelectorTable extends HidBaseTable{
    private final ArrayList<SelectorTableItem> itemList = new ArrayList<>();
    private final String sNameColumnText = "bMeaningText";
    private final String sMinIndexColumnText = "bMeaningValueMin";
    private final String sMaxIndexColumnText = "bMeaningValueMax";
    private final String sSelectableColumnText = "bSelectable";
    private final String sConditionIndexColumnText = "bConditionTableValue";
    private final int iHeaderLine = 0;
    private final File conditionFile;
        
    public HidSelectorTable(String sName, HidTableType type, String sDelimiter, File file, File conditionFile) throws RuntimeException{
        super(sName, type, file);
        if (type == HidTableType.conditionalSelector){
            if(!Files.exists(conditionFile.toPath())){throw new RuntimeException("Conditional table file not found!");}
            this.conditionFile = conditionFile;
        } else {
            this.conditionFile = null;
        }
        ArrayList<ArrayList<String>> dataTable = parseSeperatedTextFile(sDelimiter, file);
        decodeSelectorItemTable(dataTable);
    }
    
    public final class SelectorTableItem{
        private final String sName;
        private final int iMinIndex;
        private final Integer iMaxIndex;
        private final boolean bSelectable;
        private final Integer iConditionIndex;
        
        public SelectorTableItem(String sName, int iMinIndex, Integer iMaxIndex, boolean bSelectable, Integer iConditionIndex){
            this.sName = sName;
            this.iMinIndex = iMinIndex;
            this.iMaxIndex = iMaxIndex;
            this.bSelectable = bSelectable;
            this.iConditionIndex = iConditionIndex;
        }
        @Override
        public final String toString(){
            return String.format("%s / %04X / %04X / %b / %04X", this.sName, this.iMinIndex, this.iMaxIndex, this.bSelectable, this.iConditionIndex);
        }
        public final String getName(){
            return this.sName;
        }
        public final int getMinIndex(){
            return this.iMinIndex;
        }
        public final Integer getMaxIndex(){
            return this.iMaxIndex;
        }
        public final boolean isSelectable(){
            return this.bSelectable;
        }
        public final Integer getConditionIndex(){
            return this.iConditionIndex;
        }
    }
    
    // publics
    @Override
    public final String findRepresentation(int iValue, int iCondition){
        String sRepresentation = STRING_DISPLAY_DEFAULT_UNKNOWN;
        ListIterator itemIterator = itemList.listIterator();
        boolean bConditnal = (super.getType() == HidTableType.conditionalSelector);
        boolean bFound = false;
        while(itemIterator.hasNext() && (bFound == false)){
            SelectorTableItem item = (SelectorTableItem)itemIterator.next();
            int iMinIndex = item.getMinIndex();
            Integer iMaxIndex = item.getMaxIndex();
            if (bConditnal){
                Integer iConditionalValue = item.getConditionIndex();
                    if (iConditionalValue != null) {
                        if (iMaxIndex == null){
                            bFound = ((iConditionalValue == iCondition) && (iValue == iMinIndex));
                        } else {
                            bFound = ((iConditionalValue == iCondition) && ((iValue >= iMinIndex) && (iValue <= iMaxIndex)));
                        }
                    }    
                } else {
                    if (iMaxIndex == null){
                        bFound = (iValue == iMinIndex);
                    } else {
                        bFound = ((iValue >= iMinIndex) && (iValue <= iMaxIndex));
                    }
            }
            if (bFound) {
                sRepresentation = item.getName();
            }
        }
        return sRepresentation;
    }
    public final ArrayList<SelectorTableItem> getItemList(){
        return itemList;
    }
    public final ArrayList<SelectorTableItem> getItemList(int iParentIndex){
        ArrayList<SelectorTableItem> list = new ArrayList<>();
        itemList.forEach(item -> {
            if (item.getConditionIndex() == iParentIndex){
                list.add(item);
            }
        });
        return list;
    }
    public final File getConditionFile(){
        return this.conditionFile;
    }
    public final int findIndex(String sItemName){
        Integer iId = findEntryId(sItemName);
        return ((iId != null) ? itemList.get(iId).getMinIndex() : INT_VALUE_DEFAULT_DATAVALUE);
    }
    public final int findIndex(String sItemName, int iConditionIndex){
        Integer iId = findEntryId(sItemName, iConditionIndex);
        return ((iId != null) ? itemList.get(iId).getMinIndex() : INT_VALUE_DEFAULT_DATAVALUE);
    }
    public final String findItemName(int iIndex){
        Integer iId = findEntryId(iIndex);
        return ((iId != null) ? itemList.get(iId).getName() : STRING_DISPLAY_DEFAULT_UNKNOWN);
    }
    public final String findItemName(int iIndex, int iConditionIndex){
        Integer iId = findEntryId(iIndex, iConditionIndex);
        return ((iId != null) ? itemList.get(iId).getName() : STRING_DISPLAY_DEFAULT_UNKNOWN);
    }
    
    // privates
    private Integer findEntryId(String sItemName){
        for (int i = 0;i < itemList.size();i++){
            SelectorTableItem item = itemList.get(i);
            if (item.getName().equals(sItemName)){
                return  i;
            }
        }
        return null;
    }
    private Integer findEntryId(String sItemName, int iConditionIndex){
        for (int i = 0;i < itemList.size();i++){
            SelectorTableItem item = itemList.get(i);
            if ((item.getConditionIndex() == iConditionIndex) && item.getName().equals(sItemName)){
                return  i;
            }
        }
        return null;
    }
    private Integer findEntryId(int iIndex){
        for (int i = 0;i < itemList.size();i++){
            SelectorTableItem item = itemList.get(i);
            if (item.getMinIndex() == iIndex){
                return  i;
            }
        }
        return null;
    }
    private Integer findEntryId(int iIndex, int iConditionIndex){
        for (int i = 0;i < itemList.size();i++){
            SelectorTableItem item = itemList.get(i);
            if ((item.getConditionIndex() == iConditionIndex) && (item.getMinIndex() == iIndex)){
                return  i;
            }
        }
        return null;
    }
    private void decodeSelectorItemTable(ArrayList<ArrayList<String>> dataTable) throws RuntimeException{
        try{
            boolean bConditional = (super.getType() == HidTableType.conditionalSelector);
            int iNameColumn = dataTable.get(iHeaderLine).indexOf(sNameColumnText);
            int iMinIndexColumn = dataTable.get(iHeaderLine).indexOf(sMinIndexColumnText);
            int iMaxIndexColumn = dataTable.get(iHeaderLine).indexOf(sMaxIndexColumnText);
            int iSelectableColumn = dataTable.get(iHeaderLine).indexOf(sSelectableColumnText);
            var oContainer = new Object(){int iConditionIndexColumn = -1;};
            if(iNameColumn < 0){throw new NumberFormatException(sNameColumnText + " not found");}
            if(iMinIndexColumn < 0){throw new NumberFormatException(sMinIndexColumnText + " not found");}
            if(iMaxIndexColumn < 0){throw new NumberFormatException(sMaxIndexColumnText + " not found");}
            if(iSelectableColumn < 0){throw new NumberFormatException(sSelectableColumnText + " not found");}
            if (bConditional){
                oContainer.iConditionIndexColumn = dataTable.get(iHeaderLine).indexOf(sConditionIndexColumnText);
                if(oContainer.iConditionIndexColumn < 0){throw new NumberFormatException(sConditionIndexColumnText + " not found");}
            }
            dataTable.remove(iHeaderLine);
            dataTable.forEach(line -> {
                try{
                    String sName = line.get(iNameColumn);
                    String sMinIndex = line.get(iMinIndexColumn);
                    String sMaxIndex = line.get(iMaxIndexColumn);
                    String sSelectable = line.get(iSelectableColumn);
                    String sConditionIndex = "";
                    if (bConditional){
                        sConditionIndex = line.get(oContainer.iConditionIndexColumn);
                    }
                    if (sName.equals("")) {throw new NumberFormatException(String.format("Item %s empty", sNameColumnText));}
                    if (sMinIndex.equals("")) {throw new NumberFormatException(String.format("Item %s empty", sMinIndexColumnText));}
                    int iMinIndex = Integer.parseInt(sMinIndex, 16);
                    if (!isRangeValidTableId(iMinIndex)) {throw new NumberFormatException(String.format("Item %s value %d invalid", sMinIndexColumnText, iMinIndex));}
                    Integer iMaxIndex = null;
                    if (!sMaxIndex.equals("")) {
                        iMaxIndex = Integer.parseInt(sMaxIndex, 16);
                        if (!isRangeValidTableId(iMaxIndex)) {throw new NumberFormatException(String.format("Item %s value %d invalid", sMaxIndexColumnText, iMaxIndex));}
                    }
                    boolean bSelectable = Boolean.valueOf(sSelectable);
                    Integer iConditionIndex = null;
                    if (bConditional){
                        iConditionIndex = Integer.parseInt(sConditionIndex, 16);
                        if (!isRangeValidTableId(iConditionIndex)) {throw new NumberFormatException(String.format("Item %s value %d invalid", 
                                sConditionIndexColumnText, iConditionIndex));}
                    }
                    itemList.add(new SelectorTableItem(sName, iMinIndex, iMaxIndex, bSelectable, iConditionIndex));
                } catch (RuntimeException ex) {
                    super.addError(String.format("File %s / line %d: Format error: %s", super.getFile().toString(), dataTable.indexOf(line), ex.toString()));
                }
            });
        } catch(RuntimeException ex){
            throw new RuntimeException(String.format("File %s: bad format: %s", super.getFile().toString(),  ex.toString()));
        }
    }
}
