package Hid4Java;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

public class HidWorkbench {
    private static final int INT_VALUE_MIN_TAG = 0;
    private static final int INT_VALUE_MAX_TAG = 15;
    private static final int INT_VALUE_MIN_TYPE = 0;
    private static final int INT_VALUE_MAX_TYPE = 3;
    private static final int INT_VALUE_MIN_UNITEXP = -8;
    private static final int INT_VALUE_MAX_UNITEXP = 7;
    
    public static final boolean BOOL_VALUE_DEFAULT_DATASIGNED = true;
    public static final int INT_VALUE_DEFAULT_DATALENGTH = 4;
    public static final int INT_VALUE_DEFAULT_DATAVALUE = 0;
    
    public static final String STRING_DISPLAY_DEFAULT_UNKNOWN = "Unknown";
    
    public static final String STRING_DISPLAY_ITEMIDENTIFIER_USAGEPAGE = "Usage page";
    public static final String STRING_DISPLAY_ITEMIDENTIFIER_USAGE = "Usage";
    public static final String STRING_DISPLAY_ITEMIDENTIFIER_USAGEMIN = "Usage minimum";
    public static final String STRING_DISPLAY_ITEMIDENTIFIER_USAGEMAX = "Usage maximum";
    public static final String STRING_DISPLAY_ITEMIDENTIFIER_COL = "Collection";
    public static final String STRING_DISPLAY_ITEMIDENTIFIER_ENDCOL = "End Collection";
    public static final String STRING_DISPLAY_ITEMIDENTIFIER_IN = "Input";
    public static final String STRING_DISPLAY_ITEMIDENTIFIER_OUT = "Output";
    public static final String STRING_DISPLAY_ITEMIDENTIFIER_FEA = "Feature";
    public static final String STRING_DISPLAY_ITEMIDENTIFIER_LOGMIN = "Logical minimum";
    public static final String STRING_DISPLAY_ITEMIDENTIFIER_LOGMAX = "Logical maximum";
    public static final String STRING_DISPLAY_ITEMIDENTIFIER_PHYMIN = "Physical minimum";
    public static final String STRING_DISPLAY_ITEMIDENTIFIER_PHYMAX = "Physical maximum";
    public static final String STRING_DISPLAY_ITEMIDENTIFIER_REPORTID = "Report id";
    public static final String STRING_DISPLAY_ITEMIDENTIFIER_REPORTSIZE = "Report size";
    public static final String STRING_DISPLAY_ITEMIDENTIFIER_REPORTCOUNT = "Report count";
    public static final String STRING_DISPLAY_ITEMIDENTIFIER_UNIT = "Unit";
    public static final String STRING_DISPLAY_ITEMIDENTIFIER_UNITEXP = "Unit Exponent";
    public static final String STRING_DISPLAY_ITEMIDENTIFIER_DESIGINDEX = "Designator index";
    public static final String STRING_DISPLAY_ITEMIDENTIFIER_DESIGMIN = "Designator minimum";
    public static final String STRING_DISPLAY_ITEMIDENTIFIER_DESIGMAX = "Designator maximum";
    public static final String STRING_DISPLAY_ITEMIDENTIFIER_STRINGINDEX = "String index";
    public static final String STRING_DISPLAY_ITEMIDENTIFIER_STRINGMIN = "String minimum";
    public static final String STRING_DISPLAY_ITEMIDENTIFIER_STRINGMAX = "String maximum";
    public static final String STRING_DISPLAY_ITEMIDENTIFIER_PUSH = "Push";
    public static final String STRING_DISPLAY_ITEMIDENTIFIER_POP = "Pop";
    public static final String STRING_DISPLAY_ITEMIDENTIFIER_DELIMITER = "Delimiter";
    
    public static final String STRING_DISPLAY_ITEMTYPE_MAIN = "Main";
    public static final String STRING_DISPLAY_ITEMTYPE_GLOBAL = "Global";
    public static final String STRING_DISPLAY_ITEMTYPE_LOCAL = "Local";
    
    public static final String STRING_DISPLAY_UNITSYSTEMS_NONE = "None";
    public static final String STRING_DISPLAY_UNITSYSTEMS_LINSI = "Linear SI";
    public static final String STRING_DISPLAY_UNITSYSTEMS_ROTSI = "Rotation SI";
    public static final String STRING_DISPLAY_UNITSYSTEMS_LINENG = "Linear english";
    public static final String STRING_DISPLAY_UNITSYSTEMS_ROTENG = "Rotation english";
    
    public static final String STRING_DISPLAY_UNITNAME_LENGTH = "Length";
    public static final String STRING_DISPLAY_UNITNAME_MASS = "Mass";
    public static final String STRING_DISPLAY_UNITNAME_TIME = "Time";
    public static final String STRING_DISPLAY_UNITNAME_TEMPERATURE = "Temperature";
    public static final String STRING_DISPLAY_UNITNAME_CURRENT = "Current";
    public static final String STRING_DISPLAY_UNITNAME_LUMEN = "Lumen";
    
    // format handling
    public static final ArrayList<Byte> parseDatFormat(BufferedReader reader) throws IOException{
        ArrayList<Byte> data = new ArrayList<>();
        int iByteCharLength = 2;
        String sHexOnlyRegEx = "[^0-9A-Fa-f]";
        String sByte;
        String datString = reader.lines().collect(Collectors.joining());
        if (datString != null) {
            datString = datString.replaceAll(sHexOnlyRegEx, "");
            try{
                while(!datString.isEmpty()){
                    sByte = datString.substring(0, iByteCharLength);
                    datString = datString.substring(iByteCharLength);
                    data.add((byte)Integer.parseInt(sByte, 16));
                }
            } catch(IndexOutOfBoundsException | NumberFormatException ex) {}
        }
        return data;
    }
    public static final String buildDatFormat(ArrayList<Byte> data){
        StringBuilder sData = new StringBuilder();
        data.forEach(b -> {
            sData.append(String.format("%02X ", (b & 0xff)));
        });
        return sData.toString();
    }
    public static final ArrayList<Byte> parseCHeaderFormat(BufferedReader reader) throws IOException{
        ArrayList<Byte> data = new ArrayList<>();
        String sRegexLineSplit = "(//)|(/[*])";
        String sRegexCommentBlockStart = ".*/[*].*";
        String sRegexCommentBlockEnd = ".*[*]/.*";
        String sRegexDataBlockStart = ".*char.*=.*[{].*";
        String sRegexDataBlockEnd = ".*[}][;].*";
        String sRegexByteSplit = "[0][xX]";
        String sRegexRemove = "[,]";
        
        boolean bDataBlockComplete = false;
        boolean bInComment = false;
        boolean bInDataBlock = false;
        
        while(!bDataBlockComplete && reader.ready()){
            String sCompleteLine = reader.readLine();
            if (!bInComment){
                String[] sLineElements = sCompleteLine.split(sRegexLineSplit, 2);
                if (!bInDataBlock){
                    if (sLineElements[0].matches(sRegexDataBlockStart)){bInDataBlock = true;}
                } else {
                    String[] sDataElements= sLineElements[0].split(sRegexByteSplit);
                    for (String sElement : sDataElements){
                        sElement = sElement.replaceAll(sRegexRemove, "").trim();
                        while(!sElement.isEmpty()){
                            int iLength = sElement.length();
                            int iStart = ((iLength > 1) ? (sElement.length() - 2) : 0);
                            iLength = ((iLength > 1) ? 2 : 1);
                            String sByte = sElement.substring(iStart, iLength);
                            sElement = sElement.substring(0, iStart);
                            try{
                                data.add((byte)Integer.parseInt(sByte, 16));
                            }catch(NumberFormatException ex){}
                        }
                    }
                    if (sLineElements[0].matches(sRegexDataBlockEnd)){bDataBlockComplete = true;}
                }
                if (sCompleteLine.matches(sRegexCommentBlockStart)){bInComment = true;}
            } else {
                if(sCompleteLine.matches(sRegexCommentBlockEnd)){bInComment = false;}
            }
        }
        if(data.size() < 1){throw new IOException("No valid data bytes found!");}
        if(!bInDataBlock){throw new IOException("Data block opener not found!");}
        if(!bDataBlockComplete){throw new IOException("Data block closer not found!");}
        return data;
    }
    public static final String buildCHeaderFormat(ArrayList<Byte> data, String sDataBlockName){
        StringBuilder sData = new StringBuilder();
        
        int iExtension = sDataBlockName.lastIndexOf(".");
        if (iExtension > -1){sDataBlockName = sDataBlockName.substring(0, iExtension);}
        sDataBlockName = sDataBlockName.replaceAll("[^0-9a-zA-Z]", "");
        String sDataType = "char";
        String sDataBlockOpener = String.format("%s %s[%d] = {\r\n", sDataType, sDataBlockName, data.size());
        String sDataBlockCloser = "};\r\n";
        String sDataLineStart = "    ";
        String sDataByteFormat = "0x%02X";
        String sDataSpacerFormat = ", ";
        String sCommentFormat = "            // %s\r\n";
        
        var oContainer = new Object(){
            boolean bInGroup = false;
            int iByteCount = 0;
            String sItemName = "";
        };
        
        Iterator<Byte> bItr = data.iterator();
        sData.append(sDataBlockOpener);
        while (bItr.hasNext()){
            Byte bData = bItr.next();
            if (!oContainer.bInGroup){
                oContainer.iByteCount = decodeElementByteCount(bData);
                oContainer.sItemName = HidItemIdentifier.findIdentifier(decodeElementTag(bData), 
                        HidItemType.findType(decodeElementType(bData))).getDisplayName();
                sData.append(String.format((sDataLineStart + sDataByteFormat), (bData & 0xff)));
                if (oContainer.iByteCount > 0){oContainer.bInGroup = true;}
            } else {
                sData.append(String.format((sDataSpacerFormat + sDataByteFormat), (bData & 0xff)));
                oContainer.iByteCount--;
            }
            if (oContainer.iByteCount < 1){
                if (bItr.hasNext()){sData.append(sDataSpacerFormat);}
                sData.append(String.format(sCommentFormat, oContainer.sItemName));
                oContainer.bInGroup = false;
            }
        }
        sData.append(sDataBlockCloser);
        return sData.toString();
    }
    public static final ArrayList<Byte> parseASMIncludeFormat(BufferedReader reader) throws IOException{
        ArrayList<Byte> data = new ArrayList<>();
        String sRegexLineSplit = ";";
        String sRegexDataLineSplit = "(db|dB|Db|DB)";
        String sRegexByteSplit = "[hH]";
        String sRegexRemove = "[,]";
        
        while(reader.ready()){
            String sCompleteLine = reader.readLine();
            String[] sLineElements = sCompleteLine.split(sRegexLineSplit, 2);
            String[] sDataElements = sLineElements[0].split(sRegexDataLineSplit);
            if (sDataElements.length > 1){
                String[] sElements = sDataElements[1].split(sRegexByteSplit);
                for (String sElement : sElements){
                    sElement = sElement.replaceAll(sRegexRemove, "").trim();
                    while(!sElement.isEmpty()){
                        int iLength = sElement.length();
                        int iStart = ((iLength > 1) ? (sElement.length() - 2) : 0);
                        iLength = ((iLength > 1) ? 2 : 1);
                        String sByte = sElement.substring(iStart, iLength);
                        sElement = sElement.substring(0, iStart);
                        try{
                            data.add((byte)Integer.parseInt(sByte, 16));
                        }catch(NumberFormatException ex){}
                    }
                }
            }
        }
        if(data.size() < 1){throw new IOException("No valid data bytes found!");}
        return data;
    }
    public static final String buildASMIncludeFormat(ArrayList<Byte> data){
        StringBuilder sData = new StringBuilder();
        
        String sDataType = "db";
        String sDataLineStart = String.format("    %s ", sDataType);
        String sDataByteFormat = "%02xh";
        String sDataSpacerFormat = ", ";
        String sCommentFormat = "            ; %s\r\n";
        
        var oContainer = new Object(){
            boolean bInGroup = false;
            int iByteCount = 0;
            String sItemName = "";
        };
        
        Iterator<Byte> bItr = data.iterator();
        while (bItr.hasNext()){
            Byte bData = bItr.next();
            if (!oContainer.bInGroup){
                oContainer.iByteCount = decodeElementByteCount(bData);
                oContainer.sItemName = HidItemIdentifier.findIdentifier(decodeElementTag(bData), 
                        HidItemType.findType(decodeElementType(bData))).getDisplayName();
                sData.append(String.format((sDataLineStart + sDataByteFormat), (bData & 0xff)));
                if (oContainer.iByteCount > 0){oContainer.bInGroup = true;}
            } else {
                sData.append(String.format((sDataSpacerFormat + sDataByteFormat), (bData & 0xff)));
                oContainer.iByteCount--;
            }
            if (oContainer.iByteCount < 1){
                sData.append(String.format(sCommentFormat, oContainer.sItemName));
                oContainer.bInGroup = false;
            }
        }
        return sData.toString();
    }
    
    // file handling
    public static final ArrayList<ArrayList<String>> parseSeperatedTextFile(String sDelimiter, File file) throws RuntimeException{
        ArrayList<ArrayList<String>> dataTable = new ArrayList<>();
        try{
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                while(reader.ready()){
                    ArrayList<String> line = new ArrayList<>(Arrays.asList(reader.readLine().split(sDelimiter)));
                    dataTable.add(line);
                }
            }
        } catch(IOException | PatternSyntaxException ex){
            throw new RuntimeException(String.format("File read error: %s", ex.toString()));
        }
        return dataTable;
    }
    public static final ArrayList<ArrayList<Byte>> decodeReportFile(File sourceFile) throws IOException,SecurityException,IllegalArgumentException{
        String sFileName = sourceFile.getName();
        String sFileExtension;
        try{
            int iDotIndex = sFileName.lastIndexOf(".");
            sFileExtension = sFileName.substring(iDotIndex + 1);
        } catch(IndexOutOfBoundsException ex){
            throw new IllegalArgumentException(String.format("File type not readable: %s", sFileName));
        }
        return inflateByteStreamToElementList(SupportedFileType.decode(sFileExtension, sourceFile));
    }
    public static final void encodeReportFile(File targetFile, ArrayList<HidReportElement> elementList) throws IOException,SecurityException,IllegalArgumentException {
        String sFileName = targetFile.getName();
        String sFileExtension;
        try{
            int iDotIndex = sFileName.lastIndexOf(".");
            sFileExtension = sFileName.substring(iDotIndex + 1);
        }catch(IndexOutOfBoundsException ex){
            throw new IllegalArgumentException(String.format("File type not writeable: %s", sFileName));
        }
        SupportedFileType.encode(sFileExtension, targetFile, flattenElementListToByteStream(elementList));
    }
    public static final ArrayList<ArrayList<Byte>> inflateByteStreamToElementList(ArrayList<Byte> data){
        ArrayList<ArrayList<Byte>> resultingList = new ArrayList<>();
        Queue<Byte> stackedData = new LinkedList<>(data);
        while(!stackedData.isEmpty()){
            byte bPreByte = stackedData.poll();
            int iElementByteCount = decodeElementByteCount(bPreByte);
            ArrayList<Byte> elementData = new ArrayList<>();
            elementData.add(bPreByte);
            while (iElementByteCount > 0) {
                elementData.add(stackedData.poll());
                iElementByteCount--;
            }
            resultingList.add(elementData);
        }
        return resultingList;
    }
    public static final ArrayList<Byte> flattenElementListToByteStream(ArrayList<HidReportElement> elementList){
        ArrayList<Byte> report = new ArrayList<>();
        elementList.forEach(element ->{
            report.addAll(element.getByteList());
        }); 
        return report;
    }
    
    // data handling
    public static final int decodeElementByteCount(byte bPreByte){
        int iByteCount = (bPreByte & 0x03);
        if (iByteCount == 3) {iByteCount = 4;}
        return iByteCount;
    }
    public static final int decodeElementType(byte bPreByte){
        return ((bPreByte >> 2) & 0x03);
    }
    public static final int decodeElementTag(byte bPreByte){
        return ((bPreByte >> 4) & 0x0F);
    }
    public static final byte encodeTag(int iTag){
        return (byte)((iTag << 4) & 0xf0); 
    }
    public static final byte encodeType(int iType){
        return (byte)((iType << 2) & 0x0c); 
    }
    public static final byte encodeByteCount(int iByteCount){
        if (iByteCount > 2) {iByteCount = 3;}
        if (iByteCount < 0) {iByteCount = 0;}
        return (byte)(iByteCount & 0x03); 
    }
    public static final byte encodePreByte(int iTag, int iType, int iByteCount){
        byte bTag = encodeTag(iTag);
        byte bType = encodeType(iType);
        byte bCount = encodeByteCount(iByteCount);
        return (byte)((bTag | bType | bCount) & 0xff);
    }
    public static final byte removeByteCount(byte bPreByte){
        return (byte)((bPreByte >> 2) & 0x3f);
    }
    public static final byte addByteCount(byte bPreByte, int iByteCount){
        return (byte)(((bPreByte << 2) & 0xfc) | encodeByteCount(iByteCount));
    }
    public static final boolean isRangeValidTag(int iTag){
        return ((iTag >= INT_VALUE_MIN_TAG) && (iTag <= INT_VALUE_MAX_TAG));
    }
    public static final boolean isRangeValidType(int iType){
        return ((iType >= INT_VALUE_MIN_TYPE) && (iType <= INT_VALUE_MAX_TYPE));
    }
    public static final boolean isRangeValidByteCount(int iByteCount){
        return (((iByteCount >= 0) && (iByteCount <= 2)) || (iByteCount == 4));
    }
    public static final boolean isRangeValidValue(long iValue, int iByteCount, boolean bSigned) throws IllegalArgumentException{
        long iMaxValue = (long)Math.pow(256, iByteCount);
        long iSignCorrection = (bSigned ? (iMaxValue / 2) : 0);
        long iLowerLimit = 0 - iSignCorrection;
        long iUpperLimit = iMaxValue - iSignCorrection - 1;
        return ((iValue >= iLowerLimit) && (iValue <= iUpperLimit));
    }
    public static final int decodeNestedUsagePage(int iCombinedUsage){
        return Short.toUnsignedInt((short)((iCombinedUsage >>> 16)));
    }
    public static final int decodeNestedUsage(int iCombinedUsage){
        return Short.toUnsignedInt((short)(iCombinedUsage));
    }
    public static final int encodeNestedUsagePage(int iUsage, int iUsagePage){
        return (iUsage & 0x0000ffff) | ((iUsagePage << 16) & 0xffff0000);
    }
    public static final long toUnsignedValue(long iValue, int iByteCount){
        switch (iByteCount){
            case 1:return Byte.toUnsignedLong((byte)iValue);
            case 2:return Short.toUnsignedLong((short)iValue);
            case 4:return Integer.toUnsignedLong((int)iValue);
            default:return INT_VALUE_DEFAULT_DATAVALUE;
        }
    }
    
    // Tables
    public static enum HidItemIdentifier{
        unknown(STRING_DISPLAY_DEFAULT_UNKNOWN, null, HidItemType.unknown),
        usage_page(STRING_DISPLAY_ITEMIDENTIFIER_USAGEPAGE, 0, HidItemType.global),
        usage(STRING_DISPLAY_ITEMIDENTIFIER_USAGE, 0, HidItemType.local),
        usage_minimum(STRING_DISPLAY_ITEMIDENTIFIER_USAGEMIN, 1, HidItemType.local),
        usage_maximum(STRING_DISPLAY_ITEMIDENTIFIER_USAGEMAX, 2, HidItemType.local),
        collection(STRING_DISPLAY_ITEMIDENTIFIER_COL, 10, HidItemType.main),
        end_collection(STRING_DISPLAY_ITEMIDENTIFIER_ENDCOL, 12, HidItemType.main),
        input(STRING_DISPLAY_ITEMIDENTIFIER_IN, 8, HidItemType.main),
        output(STRING_DISPLAY_ITEMIDENTIFIER_OUT, 9, HidItemType.main),
        feature(STRING_DISPLAY_ITEMIDENTIFIER_FEA, 11, HidItemType.main),
        logical_minimum(STRING_DISPLAY_ITEMIDENTIFIER_LOGMIN, 1, HidItemType.global),
        logical_maximum(STRING_DISPLAY_ITEMIDENTIFIER_LOGMAX, 2, HidItemType.global),
        physical_minimum(STRING_DISPLAY_ITEMIDENTIFIER_PHYMIN, 3, HidItemType.global),
        physical_maximum(STRING_DISPLAY_ITEMIDENTIFIER_PHYMAX, 4, HidItemType.global),
        report_id(STRING_DISPLAY_ITEMIDENTIFIER_REPORTID, 8, HidItemType.global),
        report_size(STRING_DISPLAY_ITEMIDENTIFIER_REPORTSIZE, 7, HidItemType.global),
        report_count(STRING_DISPLAY_ITEMIDENTIFIER_REPORTCOUNT, 9, HidItemType.global),
        unit(STRING_DISPLAY_ITEMIDENTIFIER_UNIT, 6, HidItemType.global),
        unit_exponent(STRING_DISPLAY_ITEMIDENTIFIER_UNITEXP, 5, HidItemType.global),
        designator_index(STRING_DISPLAY_ITEMIDENTIFIER_DESIGINDEX, 3, HidItemType.local),
        designator_minimum(STRING_DISPLAY_ITEMIDENTIFIER_DESIGMIN, 4, HidItemType.local),
        designator_maximum(STRING_DISPLAY_ITEMIDENTIFIER_DESIGMAX, 5, HidItemType.local),
        string_index(STRING_DISPLAY_ITEMIDENTIFIER_STRINGINDEX, 7, HidItemType.local),
        string_minimum(STRING_DISPLAY_ITEMIDENTIFIER_STRINGMIN, 8, HidItemType.local),
        string_maximum(STRING_DISPLAY_ITEMIDENTIFIER_STRINGMAX, 9, HidItemType.local),
        push(STRING_DISPLAY_ITEMIDENTIFIER_PUSH, 10, HidItemType.global),
        pop(STRING_DISPLAY_ITEMIDENTIFIER_POP, 11, HidItemType.global),
        delimiter(STRING_DISPLAY_ITEMIDENTIFIER_DELIMITER, 10, HidItemType.local);
        
        private final Integer iTag;
        private final HidItemType type;
        private final String sDisplayName;
        
        private HidItemIdentifier(String sDisplayName, Integer iTag, HidItemType type){
            this.sDisplayName = sDisplayName;
            this.iTag = iTag;
            this.type = type;
        }
        
        public static final HidItemIdentifier findIdentifier(int iTag, HidItemType type){
            HidItemIdentifier tag = HidItemIdentifier.unknown;
            for (HidItemIdentifier temp : HidItemIdentifier.values()) {
                if (( temp.iTag != null) && (temp.iTag == iTag) && (temp.type == type)){
                    tag = temp;
                    break;
                }
            }
            return tag;
        }
        public final Integer getTag(){
            return this.iTag;
        }
        public final HidItemType getType(){
            return this.type;
        }
        public final byte getByteIdentifier(){
            byte bResult = 0;
            Integer iTempTag = this.iTag;
            if (iTempTag != null) {
                bResult |= (byte)((iTempTag << 4) & 0xf0);
            }
            Integer iTempType = this.type.getType();
            if (iTempType != null){
                bResult |= ((iTempType << 2) & 0x0c);
            }
            return bResult;
        }
        public final String getDisplayName(){
            return this.sDisplayName;
        }
    }
    public static enum HidItemType{
        unknown(STRING_DISPLAY_DEFAULT_UNKNOWN, null),
        main(STRING_DISPLAY_ITEMTYPE_MAIN, 0),
        global(STRING_DISPLAY_ITEMTYPE_GLOBAL, 1),
        local(STRING_DISPLAY_ITEMTYPE_LOCAL, 2);
        
        private final Integer iType;
        private final String sDisplayName;
        
        private HidItemType(String sDisplayName, Integer iType){
            this.sDisplayName = sDisplayName;
            this.iType = iType;
        }
        
        public final String getDisplayName(){
            return this.sDisplayName;
        }
        public final Integer getType(){
            return this.iType;
        }
        public static final HidItemType findType(Integer iValue){
            HidItemType type = HidItemType.unknown;
            for (HidItemType temp : HidItemType.values()) {
                if (Objects.equals(temp.getType(), iValue)){
                    type = temp;
                    break;
                }
            }
            return type;
        }
    }
    public static enum SupportedFileType{
        cHeader("C Header File (.h)", "h"),
        asmInclude("Assembler Include File (.inc)", "inc"),
        intelDat("Intel LAVA Data File (.dat)", "dat");
        
        private final String sExtension;
        private final String sDescription;
        
        private SupportedFileType(String sDescription, String sExtension){
            this.sDescription = sDescription;
            this.sExtension = sExtension;
        }
        
        public final String getExtension(){
            return this.sExtension;
        }
        public final String getDescription(){
            return this.sDescription;
        }
        public static final SupportedFileType getType(String sExtension){
            for (SupportedFileType type : SupportedFileType.values()){
                if (Objects.equals(type.getExtension(), sExtension)){
                    return type;
                }
            }
            return null;
        }
        public static final ArrayList<Byte> decode(String sExtension, File sourceFile) throws IOException,SecurityException,IllegalArgumentException {
            try (BufferedReader reader = new BufferedReader(new FileReader(sourceFile))) {
                switch(SupportedFileType.getType(sExtension)){
                    case intelDat:return parseDatFormat(reader);
                    case cHeader:return parseCHeaderFormat(reader);
                    case asmInclude:return parseASMIncludeFormat(reader);
                    default:throw new IllegalArgumentException(String.format("File type not supported: %s", sourceFile.getName()));
                }
            }
        }
        public static final void encode(String sExtension, File targetFile, ArrayList<Byte> data) throws IOException,SecurityException,IllegalArgumentException {
            String sData;
            switch(SupportedFileType.getType(sExtension)){
                case intelDat:sData = buildDatFormat(data);break;
                case cHeader:sData = buildCHeaderFormat(data, targetFile.getName());break;
                case asmInclude:sData = buildASMIncludeFormat(data);break;
                default:throw new IllegalArgumentException(String.format("File type not supported: %s", targetFile.getName()));
            }
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetFile))) {
                writer.write(sData);
            }
        }

    }
    public static enum HidUnitSystems{
        none(STRING_DISPLAY_UNITSYSTEMS_NONE, (byte)0),
        linearSI(STRING_DISPLAY_UNITSYSTEMS_LINSI, (byte)1),
        rotationSI(STRING_DISPLAY_UNITSYSTEMS_ROTSI, (byte)2),
        linearEng(STRING_DISPLAY_UNITSYSTEMS_LINENG, (byte)3),
        rotationEng(STRING_DISPLAY_UNITSYSTEMS_ROTENG, (byte)4);
        
        private final String sDisplayName;
        private final byte bValue;
        
        private HidUnitSystems(String sDisplayName, byte bValue){
            this.sDisplayName = sDisplayName;
            this.bValue = bValue;
        }
        public final String getDisplayName(){
            return this.sDisplayName;
        }
        private byte getValue(){
            return this.bValue;
        }
        public static byte findSystemValue(String sDisplayName){
            for (HidUnitSystems system : HidUnitSystems.values()){
                if (system.getDisplayName().equals(sDisplayName)) {
                    return system.getValue();
                }
            }
            return HidUnitSystems.none.getValue();
        }
        public static String findSystemName(int iValue){
            iValue &= 0x0f;
            for (HidUnitSystems system : HidUnitSystems.values()){
                if (system.getValue() == iValue) {
                    return system.getDisplayName();
                }
            }
            return HidUnitSystems.none.getDisplayName();
        }
        public static boolean isRangeValidExp(int iValue){
            return ((iValue >= INT_VALUE_MIN_UNITEXP) && (iValue <= INT_VALUE_MAX_UNITEXP));
        }
        private static byte decodeUnit(int iValue, int iNibble) {
            iValue = ((iValue >> (4 * iNibble)) & 0x0f);
            if (((iValue >> 3) & 0x01) != 0){iValue |= 0xf0;}
            return (byte)iValue;
        }
        public static byte decodeLength(int iValue) {
            return decodeUnit(iValue, 1);
        }
        public static byte decodeMass(int iValue) {
            return decodeUnit(iValue, 2);
        }
        public static byte decodeTime(int iValue) {
            return decodeUnit(iValue, 3);
        }
        public static byte decodeTemperature(int iValue) {
            return decodeUnit(iValue, 4);
        }
        public static byte decodeCurrent(int iValue) {
            return decodeUnit(iValue, 5);
        }
        public static byte decodeLumen(int iValue) {
            return decodeUnit(iValue, 6);
        }
        public static int buildUnit(byte bSystem, byte bLength, byte bMass, byte bTime, byte bTemp, byte bCurrent, byte bLumen){
            int iResult = 0;
            iResult |= ((bSystem & 0x0f) | 
                    ((bLength & 0x0f) << 4) | 
                    ((bMass & 0x0f) << 8) | 
                    ((bTime & 0x0f) << 12) | 
                    ((bTemp & 0x0f) << 16) | 
                    ((bCurrent & 0x0f) << 20) | 
                    ((bLumen & 0x0f) << 24));
            return iResult;
        }
    }
}
