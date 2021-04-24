
package Hid4Java;

import Hid4Java.HidWorkbench.*;
import static Hid4Java.HidWorkbench.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class HidReportElement{
    private final int iTag;
    private final int iType;
    private final HidItemIdentifier identifier;
    private final ArrayList<Byte> dataArray = new ArrayList<>();
    private final int iValue;
    private final HidReportElement nestedUsagePage;
    
    public HidReportElement(HidReportElement pattern){
        this.iTag = pattern.iTag;
        this.iType = pattern.iType;
        this.identifier = pattern.identifier;
        this.iValue = pattern.iValue;
        this.dataArray.addAll(pattern.dataArray);
        this.nestedUsagePage = calcNestedUsagePage();
    }
    public HidReportElement(byte bPreByte, Integer iData) throws IllegalArgumentException{
        this.iTag = decodeElementTag(bPreByte);
        this.iType = decodeElementType(bPreByte);
        this.identifier = HidItemIdentifier.findIdentifier(iTag, HidItemType.findType(iType));
        this.dataArray.add(bPreByte);
        fillArray(iData);
        validateData();
        repairDataErrors();
        this.iValue = calcValue();
        this.nestedUsagePage = calcNestedUsagePage();
        pack();
    }
    public HidReportElement(ArrayList<Byte> data) throws IllegalArgumentException{
        validateArray(data);
        byte bPreByte = (byte)data.get(0);
        this.iTag = decodeElementTag(bPreByte);
        this.iType = decodeElementType(bPreByte);
        this.identifier = HidItemIdentifier.findIdentifier(iTag, HidItemType.findType(iType));
        this.dataArray.addAll(data);
        validateData();
        repairDataErrors();
        this.iValue = calcValue();
        this.nestedUsagePage = calcNestedUsagePage();
        pack();
    }
    
    private void validateArray(ArrayList<Byte> data){
        if ((data == null) || (data.isEmpty())){throw new IllegalArgumentException("Null Data detected");}
        data.forEach(dataByte ->{
            if (dataByte == null){throw new IllegalArgumentException("Null data entry detected");}
        });
        
    }
    private void fillArray(Integer iData){
        if (iData != null) {
            byte[] bytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(iData).array();
            for (byte tempByte : bytes){
                this.dataArray.add(tempByte);
            }
        }
    }
    private void validateData() throws IllegalArgumentException{
        if (!isRangeValidTag(this.iTag)){throw new IllegalArgumentException("Tag invalid");}
        if (!isRangeValidType(this.iType)){throw new IllegalArgumentException("Type invalid");}
    }
    private void repairDataErrors() {
        while(this.dataArray.size() > 5){this.dataArray.remove(this.dataArray.size() - 1);}
        if (this.dataArray.size() == 4) {this.dataArray.remove(3);}
    }
    private int calcValue(){
        int iTempValue = 0;
        int iElementSize = this.dataArray.size();
        int iOffset = 1;
        if (iElementSize > iOffset) {
            int iDataSize = iElementSize - iOffset;
            byte[] temp = new byte[iDataSize];
            for (int iCount = 0; iCount < iDataSize; iCount++){
                temp[iCount] = this.dataArray.get(iCount + iOffset);
            }
            ByteBuffer buffer = ByteBuffer.wrap(temp).order(ByteOrder.LITTLE_ENDIAN);
            if(iDataSize >= 4){
                iTempValue = buffer.getInt();
            } else if (iDataSize >= 2) {
                iTempValue = buffer.getShort();
            } else {
                iTempValue = buffer.get();
            }
        }
        return iTempValue;
    }
    private HidReportElement calcNestedUsagePage(){
        HidReportElement tempNestedUsagePage = null;
        switch(this.identifier){
            case usage:
            case usage_minimum:
            case usage_maximum:
                int iNestedUsagePage = decodeNestedUsagePage(this.iValue);
                if (iNestedUsagePage != 0) {
                    tempNestedUsagePage = new HidReportElement(HidItemIdentifier.usage_page.getByteIdentifier(), iNestedUsagePage);
                }
                break;
            default:break;
        }
        return tempNestedUsagePage;
    }
    private void optimize(){
        switch (this.dataArray.size()){
            case 5:
                if(isDataBlockReduceable((short)(((this.dataArray.get(4) & 0xff) << 8) | (this.dataArray.get(3) & 0xff)), this.dataArray.get(2))){
                    this.dataArray.remove(4);
                    this.dataArray.remove(3);
                } else{
                    break;
                }
            case 3:
                if(isDataBlockReduceable(this.dataArray.get(2), this.dataArray.get(1))){
                    this.dataArray.remove(2);
                }
                break;
            default:break;
        }
    }
    private boolean isDataBlockReduceable(int iDataBlockToReduce, int iNextlowerDataBlock){
        boolean bReduceable = false;
        if (iNextlowerDataBlock >= 0){
            if (iDataBlockToReduce == 0) {
                bReduceable = true;
            }
        } else {
            if (iDataBlockToReduce == -1) {
                bReduceable = true;
            }
        }
        return bReduceable;
    }
    private void calcByteCount(){
        byte bPreByte = this.dataArray.get(0);
        bPreByte &= 0xfc;
        byte bByteCount = encodeByteCount(dataArray.size() - 1);
        this.dataArray.set(0, (byte)(bPreByte | bByteCount));
    }
    
    public final void pack(){
        optimize();
        calcByteCount();
    }
    public final boolean hasNestedUsagePage(){
        return (this.nestedUsagePage != null);
    }
    public final boolean decreasesIndent(){
        return (this.identifier == HidItemIdentifier.end_collection);
    }
    public final boolean increasesIndent(){
        return (this.identifier == HidItemIdentifier.collection);
    }
    public final int getElementLength(){
        return this.dataArray.size();
    }
    public final int getValue(){
        return this.iValue;
    }
    public final int getTag(){
        return this.iTag;
    }
    public final int getType(){
        return this.iType;
    }
    public final HidReportElement getNestedUsagePage(){
        return this.nestedUsagePage;
    }
    public final HidItemIdentifier getItemIdentifier(){
        return this.identifier;
    }
    public final ArrayList<Byte> getByteList(){
        return dataArray;
    }
}
