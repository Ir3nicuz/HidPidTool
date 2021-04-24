
package Hid4Java;

import Hid4Java.HidWorkbench.*;
import static Hid4Java.HidWorkbench.*;
import java.util.ArrayList;
import java.util.List;

public class HidReport{
    private final ArrayList<HidReportElement> elementList = new ArrayList<>();
    
    public final int getElementCount(){
        return elementList.size();
    }
    public final int getLength(){
        int iLength = 0;
        elementList.stream().map(element -> element.getElementLength()).reduce(iLength, Integer::sum);
        return iLength;
    }
    public final void clear(){
        elementList.clear();
    }
    public final boolean add(int iIndex, HidReportElement element){
        boolean bResult = false;
        try{
            elementList.add(((iIndex == -1) ? 0: iIndex), element);
            bResult = true;
        } catch(IndexOutOfBoundsException ex){
            if (iIndex == elementList.size()) {
                add(element);
                bResult = true;
            }
        }
        return bResult;
    }
    public final boolean replace(int iIndex, HidReportElement element){
        boolean bResult = false;
        try{
            if (iIndex == -1) {
                elementList.add(0, element);
            } else {
                elementList.set(iIndex, element);
            }
            bResult = true;
        } catch(IndexOutOfBoundsException ex){
            if (iIndex == elementList.size()) {
                add(element);
                bResult = true;
            }
        }
        return bResult;
    }
    public final void add(HidReportElement element){
        elementList.add(element);
    }
    public final HidReportElement remove(int iIndex){
        try{
            return elementList.remove(iIndex);
        }catch(IndexOutOfBoundsException ex){
            return null;
        }
    }
    public final int remove(List<Integer> indexList){
        var oContainer = new Object(){int iResult = 0;};
        if (indexList != null){
            indexList.forEach(iIndex -> {
                try{
                    elementList.remove(iIndex.intValue());
                    oContainer.iResult++;
                }catch(IndexOutOfBoundsException | NullPointerException ex){}
            });
        }
        return oContainer.iResult;
    }
    public final boolean remove(HidReportElement element){
        return elementList.remove(element);
    }
    public final int removeAll(){
        int iCount = 0;
        while(!elementList.isEmpty()){
            elementList.remove(0);
            iCount++;
        }
        return iCount;
    }
    public final ArrayList<HidReportElement> getList(){
        return this.elementList;
    }  
    public final int moveIndexUp(List<Integer> indexList){
        var oContainer = new Object(){int iResult = 0;};
        if (indexList != null){
            indexList.forEach(iIndex -> {
                try{
                    HidReportElement temp = elementList.get(iIndex + 1);
                    elementList.set(iIndex + 1, elementList.get(iIndex));
                    elementList.set(iIndex, temp);
                    oContainer.iResult++;
                }catch(IndexOutOfBoundsException | NullPointerException ex){}
            });
        }
        return oContainer.iResult;
    }
    public final int moveIndexDown(List<Integer> indexList){
        var oContainer = new Object(){int iResult = 0;};
        if (indexList != null){
            indexList.forEach(iIndex -> {
                try{
                    HidReportElement temp = elementList.get(iIndex - 1);
                    elementList.set(iIndex - 1, elementList.get(iIndex));
                    elementList.set(iIndex, temp);
                    oContainer.iResult++;
                }catch(IndexOutOfBoundsException | NullPointerException ex){}
            });
        }
        return oContainer.iResult;
    }
    public final HidReportElement getElement(int iElement){
        try {
            return elementList.get(iElement);
        } catch(IndexOutOfBoundsException ex){
            return null;
        }
    }
    public final int findParentUsagePageValue(int iElement){
        int iResult = INT_VALUE_DEFAULT_DATAVALUE;
        HidReportElement element;
        for (int iCounter = iElement - 1; iCounter >= 0;iCounter--){
            if (iCounter < elementList.size()){
                element = elementList.get(iCounter);
                if (element.getItemIdentifier() == HidItemIdentifier.usage_page){
                    return element.getValue();
                }
            }
        }
        return iResult;
    }
}
