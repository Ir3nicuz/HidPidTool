
package my.usbhidpidtool;

import Hid4Java.HidReportElement;
import static Hid4Java.HidWorkbench.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public final class HidFormatTable extends HidBaseTable{
    private static final String STRING_NAME_BASETABLE = "items";
    private final ArrayList<FormatTableItem> itemList = new ArrayList<>();
    private final String sIdentifierNameColumnText = "sIdentifierName";
    private final String sTypeNameColumnText = "sTypeName";
    private final String sTagValueColumnText = "bTagValue";
    private final String sTypeValueColumnText = "bTypeValue";
    private final String sTableFileNameColumnText = "sTableFileName";
    private final String sTableTypeColumnText = "eTableType";
    private final String sConditionFileNameColumnText = "sConditionFileName";
    private final String sDataSignedColumnText = "bDataSigned";
    private final String sDataMaxBytesColumnText = "bDataMaxBytes";
    private final int iHeaderLine = 0;
    
    public HidFormatTable(String sDelimiter, Path rootPath, File file) throws RuntimeException{
        super(STRING_NAME_BASETABLE, HidTableType.format, file);
        ArrayList<ArrayList<String>> dataTable = parseSeperatedTextFile(sDelimiter, Paths.get(rootPath.toString(), file.toString()).toFile());
        decodeFormatTable(dataTable, rootPath, sDelimiter);
    }
    
    public final class FormatTableItem{
        private final String sIdentifierName;
        private final String sTypeName;
        private final int iTagValue;
        private final int iTypeValue;
        private final HidBaseTable subTable;
        private final boolean bSigned;
        private final int iMaxDataLength;
        
        public FormatTableItem(String sIdentifierName, String sTypeName, int iTagValue, int iTypeValue, 
                HidBaseTable subTable, boolean bSigned, int iMaxDataLength){
            this.sIdentifierName = sIdentifierName;
            this.sTypeName = sTypeName;
            this.iTagValue = iTagValue;
            this.iTypeValue = iTypeValue;
            this.subTable = subTable;
            this.bSigned = bSigned;
            this.iMaxDataLength = iMaxDataLength;
        }
        
        @Override
        public final String toString(){
            return String.format("%s / %s / %02X", this.sIdentifierName, this.sTypeName, (this.iTagValue + this.iTypeValue));
        }
        public final String getIdentifierName(){
            return this.sIdentifierName;
        }
        public final String getTypeName(){
            return this.sTypeName;
        }
        public final int getTagValue(){
            return this.iTagValue;
        }
        public final int getTypeValue(){
            return this.iTypeValue;
        }
        public final HidBaseTable getSubTable(){
            return this.subTable;
        }
        public final boolean isSigned(){
            return this.bSigned;
        }
        public final int getMaxDataLength(){
            return this.iMaxDataLength;
        }
    }
    
    @Override
    public final String findRepresentation(int iValue, int iCondition){
        return STRING_DISPLAY_DEFAULT_UNKNOWN;
    }
    
    private void decodeFormatTable(ArrayList<ArrayList<String>> dataTable, Path fileRootPath, String sDelimiter) throws RuntimeException{
        try{
            int iIdentifierNameColumn = dataTable.get(iHeaderLine).indexOf(sIdentifierNameColumnText);
            int iTypeNameColumn = dataTable.get(iHeaderLine).indexOf(sTypeNameColumnText);
            int iTagValueColumn = dataTable.get(iHeaderLine).indexOf(sTagValueColumnText);
            int iTypeValueColumn = dataTable.get(iHeaderLine).indexOf(sTypeValueColumnText);
            int iTableFileNameColumnText = dataTable.get(iHeaderLine).indexOf(sTableFileNameColumnText);
            int iTableTypeColumnText = dataTable.get(iHeaderLine).indexOf(sTableTypeColumnText);
            int iConditionFileNameColumnText = dataTable.get(iHeaderLine).indexOf(sConditionFileNameColumnText);
            int iDataSignedColumnText = dataTable.get(iHeaderLine).indexOf(sDataSignedColumnText);
            int iDataMaxBytesColumnText = dataTable.get(iHeaderLine).indexOf(sDataMaxBytesColumnText);
            if(iIdentifierNameColumn < 0){throw new NumberFormatException(sIdentifierNameColumnText + " not found");}
            if(iTypeNameColumn < 0){throw new NumberFormatException(sTypeNameColumnText + " not found");}
            if(iTagValueColumn < 0){throw new NumberFormatException(sTagValueColumnText + " not found");}
            if(iTypeValueColumn < 0){throw new NumberFormatException(sTypeValueColumnText + " not found");}
            if(iTableFileNameColumnText < 0){throw new NumberFormatException(sTableFileNameColumnText + " not found");}
            if(iTableTypeColumnText < 0){throw new NumberFormatException(sTableTypeColumnText + " not found");}
            if(iConditionFileNameColumnText < 0){throw new NumberFormatException(sConditionFileNameColumnText + " not found");}
            if(iDataSignedColumnText < 0){throw new NumberFormatException(sDataSignedColumnText + " not found");}
            if(iDataMaxBytesColumnText < 0){throw new NumberFormatException(sDataMaxBytesColumnText + " not found");}
            dataTable.remove(iHeaderLine);
            dataTable.forEach(line -> {
                try{
                    String sIdentifierName = line.get(iIdentifierNameColumn);
                    String sTypeName = line.get(iTypeNameColumn);
                    String sTagValue = line.get(iTagValueColumn);
                    String sTypeValue = line.get(iTypeValueColumn);
                    String sTableFileName  = line.get(iTableFileNameColumnText);
                    String sTableType = line.get(iTableTypeColumnText);
                    String sConditionFileName  = line.get(iConditionFileNameColumnText);
                    String sDataSigned = line.get(iDataSignedColumnText);
                    String sDataMaxBytes = line.get(iDataMaxBytesColumnText);
                    if (sIdentifierName.equals("")) {throw new NumberFormatException(String.format("Item %s empty", sIdentifierNameColumnText));}
                    if (sTypeName.equals("")) {throw new NumberFormatException(String.format("Item %s empty", sTypeNameColumnText));}
                    if (sTagValue.equals("")) {throw new NumberFormatException(String.format("Item %s empty", sTagValueColumnText));}
                    if (sTypeValue.equals("")) {throw new NumberFormatException(String.format("Item %s empty", sTypeValueColumnText));}
                    if (sDataSigned.equals("")) {throw new NumberFormatException(String.format("Item %s empty", sDataSignedColumnText));}
                    if (sDataMaxBytes.equals("")) {throw new NumberFormatException(String.format("Item %s empty", sDataMaxBytesColumnText));}
                    int iTagValue = Integer.parseInt(sTagValue, 16);
                    if (!isRangeValidTag(iTagValue)) {throw new NumberFormatException(String.format("Item %s value %d invalid", sTagValueColumnText, iTagValue));}
                    int iTypeValue = Integer.parseInt(sTypeValue, 16);
                    if (!isRangeValidType(iTypeValue)) {throw new NumberFormatException(String.format("Item %s value %d invalid", sTypeValueColumnText, iTypeValue));}
                    HidBaseTable table = null;
                    if (!sTableFileName.equals("") || !sTableType.equals("") || !sConditionFileName.equals("")){
                        table = createTable(sIdentifierName, sTableType, sDelimiter, 
                                Paths.get(fileRootPath.toString(), sTableFileName).toFile(), Paths.get(fileRootPath.toString(), sConditionFileName).toFile());
                        super.addError(table.getErrors());
                    }
                    boolean bDataSigned = Boolean.parseBoolean(sDataSigned);
                    int iDataMaxBytes = Integer.parseInt(sDataMaxBytes);
                    if (!isRangeValidByteCount(iDataMaxBytes)) {throw new NumberFormatException(String.format("Item %s value %d invalid", sDataMaxBytesColumnText, iDataMaxBytes));}
                    itemList.add(new FormatTableItem(sIdentifierName, sTypeName, iTagValue, iTypeValue, table, bDataSigned, iDataMaxBytes));
                } catch (RuntimeException ex) {
                    super.addError(String.format("File %s / line %d: Format error: %s", super.getFile().toString(), dataTable.indexOf(line), ex.toString()));
                }
            });
        } catch(RuntimeException ex){
            throw new RuntimeException(String.format("File %s: bad format: %s", super.getFile().toString(),  ex.toString()));
        }
    }
    private Integer findEntryId(int iTag, int iType){      
        for (int i = 0;i < itemList.size();i++){
            FormatTableItem item = itemList.get(i);
            if ((item.getTagValue() == iTag) && (item.getTypeValue() == iType)){
                return i;
            }
        }
        return null;
    }
    private Integer findEntryId(String sIdentifierName){       
        for (int i = 0;i < itemList.size();i++){
            FormatTableItem item = itemList.get(i);
            if (item.getIdentifierName().equals(sIdentifierName)){
                return  i;
            }
        }
        return null;
    }
    private Integer findEntryId(File tableFile){       
        for (int i = 0;i < itemList.size();i++){
            FormatTableItem item = itemList.get(i);
            HidBaseTable table = item.getSubTable();
            if ((table != null) && table.getFile().equals(tableFile)){
                return  i;
            }
        }
        return null;
    }
    private boolean isSigned(Integer iId){
        return (iId != null) ? itemList.get(iId).bSigned : BOOL_VALUE_DEFAULT_DATASIGNED;
    }
    private HidBaseTable findTable(Integer iId){
        return (iId != null) ? itemList.get(iId).getSubTable() : null;
    }
    private int findMaxDataLength(Integer iId){
        if (iId == null){return INT_VALUE_DEFAULT_DATALENGTH;}
        return itemList.get(iId).getMaxDataLength();
    }
    
    public final String getTypeName(int iTag, int iType){
        Integer iId = findEntryId(iTag, iType);
        return (iId != null) ? itemList.get(iId).getTypeName() : STRING_DISPLAY_DEFAULT_UNKNOWN;
    }
    public final String findIdentifierName(int iTag, int iType){
        Integer iId = findEntryId(iTag, iType);
        return (iId != null) ? itemList.get(iId).getIdentifierName() : STRING_DISPLAY_DEFAULT_UNKNOWN;
    }
    public final boolean isDataNeeding(byte bIdentifier){
        Integer iId = findEntryId(decodeElementTag(bIdentifier), decodeElementType(bIdentifier));
        return ((iId != null) ? (itemList.get(iId).getMaxDataLength() > 0) : true);
    }
    public final int findMaxDataLength(int iTag, int iType){
        Integer iId = findEntryId(iTag, iType);
        return findMaxDataLength(iId);
    }
    public final String findElementUsage(int iTag, int iType, int iValue, HidReportElement nestedUsagePage, int iParentUsagePage){
        String sUsageDisplay = "";
        Integer iId = findEntryId(iTag, iType);
        HidBaseTable table = findTable(iId);
        if (table != null) {
            if (nestedUsagePage != null){
                Integer iParentId = findEntryId(nestedUsagePage.getTag(), nestedUsagePage.getType());
                HidBaseTable parentTable = findTable(iParentId);
                String sNestedUsagePage = (parentTable != null) ? parentTable.findRepresentation(nestedUsagePage.getValue(), 0) : STRING_DISPLAY_DEFAULT_UNKNOWN;
                String sUsage = table.findRepresentation((int)(iValue & 0xffff), nestedUsagePage.getValue());
                sUsageDisplay = String.format("Nested UsagePage: %s / Usage: %s", sNestedUsagePage, sUsage);
            } else {
                sUsageDisplay = table.findRepresentation(iValue, iParentUsagePage);
            }
        } else {
            int iDataLength = findMaxDataLength(iId);
            if (iDataLength != 0) {
                if(isSigned(iId)){
                    sUsageDisplay = String.valueOf(iValue);
                } else {
                    switch(findMaxDataLength(iId)){
                        case 1:sUsageDisplay = String.valueOf(Byte.toUnsignedInt((byte)iValue));break;
                        case 2:sUsageDisplay = String.valueOf(Short.toUnsignedInt((short)iValue));break;
                        default:sUsageDisplay = Integer.toUnsignedString(iValue);break;
                    }
                }
            }
        }
        return sUsageDisplay;
    }
    public final ArrayList<String> getIdentifierNameList(){
        ArrayList<String> nameList = new ArrayList<>();
        itemList.forEach(item -> {
            nameList.add(item.getIdentifierName());
        });
        return nameList;
    }
    public final ArrayList<HidBaseTable> getSubTables(){
        ArrayList<HidBaseTable> subTables = new ArrayList<>();
        itemList.forEach(item -> {
            HidBaseTable table = item.getSubTable();
            if (table != null){
                subTables.add(table);
            }
        });
        return subTables;
    }
    public final byte findIdentifier(String sIdentifierName){
        Integer iId = findEntryId(sIdentifierName);
        if (iId != null) {
            int iTag = itemList.get(iId).getTagValue();
            int iType = itemList.get(iId).getTypeValue();
            return encodePreByte(iTag, iType, 0);
        }
        return INT_VALUE_DEFAULT_DATAVALUE;
    }
    public final HidBaseTable findTableByFile(File file){
        Integer iId = findEntryId(file);
        return ((iId != null) ? itemList.get(iId).getSubTable() : null);
    }
    public final boolean findSignedState(int iTag, int iType){
        Integer iId = findEntryId(iTag, iType);
        return isSigned(iId);
    }
}
