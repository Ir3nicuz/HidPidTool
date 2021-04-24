
package my.usbhidpidtool;

import java.io.File;
import java.util.ArrayList;

public abstract class HidBaseTable{
    private static final int INT_VALUE_MIN_TABLEID = 0;
    private static final int INT_VALUE_MAX_TABLEID = 65535;
    private static final int INT_VALUE_MIN_BITFIELD = 0;
    private static final int INT_VALUE_MAX_BITFIELD = 31;
    
    private final ArrayList<String> errorList = new ArrayList<>();
    private final File file;
    private final HidTableType type;
    private final String sName;
    
    public HidBaseTable(String sName, HidTableType type, File file){
        this.type = type;
        this.file = file;
        this.sName = sName;
    }
    
    public static final HidBaseTable createTable(String sName, String sTableType, String sDelimiter, File file, File conditionFile){
        HidBaseTable tempTable = null;
        try{
            HidTableType tempType = HidTableType.valueOf(sTableType);
            try{
                switch(tempType){
                    case binary:tempTable = new HidBinaryTable(sName, tempType, sDelimiter, file);break;
                    case selector:case conditionalSelector:
                        tempTable = new HidSelectorTable(sName, tempType, sDelimiter, file, conditionFile);break;
                    case unit:tempTable = new HidUnitTable(sName, tempType, sDelimiter, file);break;
                    default:throw new RuntimeException("Unsupported TableType " + tempType.name());
                }
            }catch(RuntimeException ex){
                throw new RuntimeException(String.format("Table creation error: %s", ex.toString()));
            }                
        } catch(IllegalArgumentException | NullPointerException ex){
            throw new RuntimeException(String.format("Table type %s unsupported: %s", sTableType, ex.toString()));
        }
        return tempTable;
    }
    public static enum HidTableType{
        conditionalSelector,
        selector,
        binary,
        unit,
        format;
    }
    
    public abstract String findRepresentation(int iValue, int iCondition);
    public final boolean isConditinal(){
        return (this.type == HidTableType.conditionalSelector);
    }
    public final ArrayList<String> getErrors(){
        return errorList;
    }
    public final boolean hasError(){
        return !errorList.isEmpty();
    }
    public final void addError(String sError){
        errorList.add(sError);
    }
    public final void addError(ArrayList<String> errors){
        errorList.addAll(errors);
    }
    public final File getFile(){
        return this.file;
    }
    public final HidTableType getType(){
        return this.type;
    }
    public final String getName(){
        return this.sName;
    }
    public static final boolean isRangeValidTableId(int iTableId){
        return ((iTableId >= INT_VALUE_MIN_TABLEID) && (iTableId <= INT_VALUE_MAX_TABLEID));
    }
    public static final boolean isRangeValidBitField(int iBit){
        return ((iBit >= INT_VALUE_MIN_BITFIELD) && (iBit <= INT_VALUE_MAX_BITFIELD));
    }
}
