
package my.usbhidpidtool;

import Hid4Java.HidReportElement;
import static Hid4Java.HidWorkbench.*;
import java.util.ArrayList;
import java.util.StringJoiner;


public class HidElementTableRow extends HidReportElement{
    private final String sElementName;
    private final String sElementType;
    private String sElementUsage = "";
    private int iActualIndent = 0;
    
    public HidElementTableRow(HidElementTableRow pattern){
        super(pattern);
        this.iActualIndent = pattern.iActualIndent;
        this.sElementName = pattern.sElementName;
        this.sElementType = pattern.sElementType;
        this.sElementUsage = pattern.sElementUsage;
    }
    public HidElementTableRow(byte bIdentifier, Integer iDataValue, HidFormatTable hidFormatData){
        super(bIdentifier, iDataValue);
        repairDataErrors(hidFormatData);
        super.pack();
        this.sElementName = buildElementName(hidFormatData);
        this.sElementType = buildElementType(hidFormatData);
        this.sElementUsage = buildElementUsage(hidFormatData, INT_VALUE_DEFAULT_DATAVALUE);
    }
    public HidElementTableRow(ArrayList<Byte> data, HidFormatTable hidFormatData){
        super(data);
        repairDataErrors(hidFormatData);
        super.pack();
        this.sElementName = buildElementName(hidFormatData);
        this.sElementType = buildElementType(hidFormatData);
        this.sElementUsage = buildElementUsage(hidFormatData, INT_VALUE_DEFAULT_DATAVALUE);
    }
    
    private void repairDataErrors(HidFormatTable hidFormatData){
        if (hidFormatData == null) {return;}
        int iMaxLength = hidFormatData.findMaxDataLength(getTag(), getType());
        ArrayList<Byte> dataArray = getByteList();
        if (iMaxLength == 0){
            while(dataArray.size() > 1){dataArray.remove(dataArray.size() - 1);}
            return;
        } else{
            if (dataArray.size() < 2){dataArray.add((byte)INT_VALUE_DEFAULT_DATAVALUE);}
        }
        if (iMaxLength == 1){
            while(dataArray.size() > 2){dataArray.remove(dataArray.size() - 1);}
        }
    }
    private String buildElementType(HidFormatTable hidFormatData){
        String sTempElementType = "";
        if (hidFormatData != null) {sTempElementType = hidFormatData.getTypeName(getTag(), getType());}
        if (sTempElementType.equals("")) {sTempElementType = getItemIdentifier().getType().getDisplayName();}
        return sTempElementType;
    }
    private String buildElementName(HidFormatTable hidFormatData){
        String sTempElementName = "";   
        if (hidFormatData != null) {sTempElementName = hidFormatData.findIdentifierName(getTag(), getType());}
        if (sTempElementName.equals("")){sTempElementName = getItemIdentifier().getDisplayName();}
        return sTempElementName;
    }
    private String buildElementUsage(HidFormatTable hidFormatTable, int iParentUsagePage){
        String sTempElementUsage = "";
        if (hidFormatTable != null) {sTempElementUsage = hidFormatTable.findElementUsage(getTag(), getType(), getValue(), getNestedUsagePage(), iParentUsagePage);}
        if (sTempElementUsage.equals("")){sTempElementUsage = getSimpleElementUsage();}   
        return sTempElementUsage;
    }
    private String getSimpleElementUsage(){
        String sUsageData;
        switch(getItemIdentifier()){
            case logical_minimum:case logical_maximum:case physical_minimum:case physical_maximum:case unit_exponent:
                sUsageData = String.valueOf(getValue());
                break;
            case input:case output:case feature:case unit:case delimiter:
                sUsageData = String.format("0x%08X", getValue());
                break;
            case report_size:case report_count:case designator_index:case designator_minimum:case designator_maximum:
            case string_index:case string_minimum:case string_maximum:case report_id:
                sUsageData = Integer.toUnsignedString(getValue());
                break;
            case end_collection:case push:case pop:
                sUsageData = "";
                break;
            default:
                sUsageData = STRING_DISPLAY_DEFAULT_UNKNOWN;
                break;
        }
        return sUsageData;
    }
    
    public final void setIndent(int iIndent){
        this.iActualIndent = iIndent;
    }
    public final int getIndent(){
        return this.iActualIndent;
    }
    public final void refreshUsageName(HidFormatTable hidFormatData, int iParentUsagePage){
        this.sElementUsage = buildElementUsage(hidFormatData, iParentUsagePage);
    }
    public final String getElementName(){
        return this.sElementName;
    }
    public final String getElementType(){
        return this.sElementType;
    }
    public final String getElementUsage(){
        return this.sElementUsage;
    }
    public final String getBytes(){
        StringJoiner joiner = new StringJoiner(" ", "", "");
        super.getByteList().forEach(b ->{ 
            joiner.add(String.format("%02X", b));
        });
        return joiner.toString();
    }
    public final boolean hasData(){
        return (getElementLength() > 1);
    }
    
    @Override
    public String toString(){
        return String.format("%s - %s - %s - %d / %s", this.sElementName, this.sElementUsage, this.sElementType, getValue(), this.getByteList().toString());
    }
}
