
package my.usbhidpidtool;

import static Hid4Java.HidWorkbench.*;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import my.usbhidpidtool.HidBaseTable.HidTableType;
import my.usbhidpidtool.HidBinaryTable.*;

public class HidElementEditor extends JDialog implements ActionListener{
    public static final int ABORTED = 0;
    public static final int CONFIRMED = 1;
    
    private static final String STRING_TEXT_BUTTON_CONFIRM = "Confirm";
    private static final String STRING_TEXT_BUTTON_ABORT = "Abort";
    private static final String STRING_TITLE_DIALOG_CHANGE = "Change";
    private static final String STRING_TITLE_DIALOG_CREATE = "Create";
    private static final String STRING_TITLE_DIALOG_MANUAL = "Manual Entry";
    
    private int iResult = ABORTED;
    
    private HidElementTableRow createdRow = null;
    private final Frame parent;
    private final HidElementEditor dialog;
    private final HidFormatTable formatTable;
    
    private final CardLayout cards = new CardLayout();
    private final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    private final JPanel editPanel = new JPanel(cards);
    private final JButton confirmButton = new JButton(STRING_TEXT_BUTTON_CONFIRM);
    private final JButton abortButton = new JButton(STRING_TEXT_BUTTON_ABORT);
    
    public HidElementEditor(Frame parent, HidFormatTable table) throws IllegalArgumentException{
        super(parent, ModalityType.APPLICATION_MODAL);
        this.dialog = this;
        this.parent = parent;
        this.formatTable = table;
        
        if (table == null){throw new IllegalArgumentException("HidFormatTable is null!");}
        
        // Layouts / comp adding
        this.setLayout(new BorderLayout());
        buttonPanel.add(confirmButton);
        buttonPanel.add(abortButton);
        this.add(buttonPanel, BorderLayout.SOUTH);
        this.add(editPanel, BorderLayout.CENTER);
        
        // creation
        createEditPanels(table);
        
        // Listener
        confirmButton.addActionListener(this.dialog);
        abortButton.addActionListener(this.dialog);
    }
    
    // publics
    public final int showCreationDialog(String sIdentifierName, int iParentUsagePage){
        this.iResult = ABORTED;
        sIdentifierName = (sIdentifierName != null ? sIdentifierName : STRING_TITLE_DIALOG_MANUAL);
        setTitle(String.format("%s: %s", STRING_TITLE_DIALOG_CREATE, sIdentifierName));
        prepareEditPanel(null, sIdentifierName, iParentUsagePage);
        showDialog();
        return this.iResult;
    }
    public final int showChangeDialog(HidElementTableRow oldRow, int iParentUsagePage){
        this.iResult = ABORTED;
        if (oldRow != null) {
            setTitle(String.format("%s: %s", STRING_TITLE_DIALOG_CHANGE, oldRow.toString()));
            String sIdentifierName = STRING_TITLE_DIALOG_MANUAL;
            if (this.formatTable != null) {
                sIdentifierName = this.formatTable.findIdentifierName(oldRow.getTag(), oldRow.getType());
            }
            prepareEditPanel(oldRow, sIdentifierName, iParentUsagePage);
            showDialog();
        }
        return this.iResult;
    }
    public final HidElementTableRow getCreatedRow(){
        return this.createdRow;
    }
    
    // privates
    private void showDialog(){
        pack();
        revalidate();
        setLocationRelativeTo(this.parent);
        setVisible(true);
    }
    private void closeDialog(int iResult){
        this.iResult = iResult;
        if (this.iResult == CONFIRMED) {
            DisplayPanel panel = findActiveDisplayPanel();
            if (panel.computeValue()){
                this.createdRow = new HidElementTableRow(panel.computeIdentifier(), panel.getValue(), this.formatTable);
                dispose();
            }
        } else {
            dispose();
        }
    }
    private DisplayPanel findActiveDisplayPanel(){
        for (Component comp : editPanel.getComponents()){
            if (comp.isVisible()){
                return (DisplayPanel)comp;
            }
        }
        return null;
    } 
    private void prepareEditPanel(HidElementTableRow oldRow, String sIdentifierName, int iParentUsagePage){
        cards.show(editPanel, STRING_TITLE_DIALOG_MANUAL);
        if (sIdentifierName != null) {
            cards.show(editPanel, sIdentifierName);
        }
        DisplayPanel panel = findActiveDisplayPanel();
        panel.prepare(oldRow, sIdentifierName, iParentUsagePage);
    }
    private void createEditPanels(HidFormatTable formatTable){
        editPanel.removeAll();
        editPanel.add(new ManualDisplayPanel(), STRING_TITLE_DIALOG_MANUAL);
        formatTable.getSubTables().forEach(subTable ->{
            switch(subTable.getType()){
                case binary:editPanel.add(new BinaryDisplayPanel((HidBinaryTable)subTable), subTable.getName());break;
                case selector:
                case conditionalSelector:editPanel.add(new SelectorDisplayPanel((HidSelectorTable)subTable), subTable.getName());break;
                case unit:editPanel.add(new UnitDisplayPanel((HidUnitTable)subTable), subTable.getName());break;
                default:break;
            }
        });
    }
    
    // events
    @Override
    public void actionPerformed(ActionEvent evt) {
        Object source = evt.getSource();
        if (source.equals(confirmButton)){
            closeDialog(CONFIRMED);
        } else if (source.equals(abortButton)) {
            closeDialog(ABORTED);
        }
    }
    
    // display items
    private abstract class DisplayPanel extends JPanel{
        public final Color COLOR_ITEMBORDER = Color.black;
        
        private String sPanelName = "";
        private int bManualTag = INT_VALUE_DEFAULT_DATAVALUE;
        private int bManualType = INT_VALUE_DEFAULT_DATAVALUE;
        private int iValue = INT_VALUE_DEFAULT_DATAVALUE;
        private final ArrayList<LastValue> lastValueTable = new ArrayList<>();
        
        private DisplayPanel(){
            super();
        }
        
        // Publics
        public void prepare(HidElementTableRow oldRow, String sIdentifierName, int iParentUsagePage){
            this.sPanelName = sIdentifierName;
            setValue((oldRow != null) ? oldRow.getValue() : null);
            prepare(iParentUsagePage, oldRow);
        }
        public abstract void prepare(int iParentUsagePage, HidElementTableRow oldRow);
        public abstract boolean compute();
        @Override
        public final String getName(){
            return this.sPanelName;
        }
        public final int getValue(){
            return this.iValue;
        }
        public void setValue(Integer iValue){
            if (iValue == null) {
                iValue = getLastValue(this.sPanelName);
            }
            this.iValue = iValue;
        }
        public void setManualIdentifier(byte bIdentifier){
            this.bManualTag = decodeElementTag(bIdentifier);
            this.bManualType = decodeElementType(bIdentifier);
        }
        public final boolean computeValue(){
            boolean bResult = compute();
            if (bResult){setLastValue();}
            return bResult;
        }
        public final byte computeIdentifier(){
            if (isManual()) {
                return encodePreByte(this.bManualTag, this.bManualType, 0);
            } else {
                return formatTable.findIdentifier(sPanelName);
            }
        }
        public final boolean isManual(){
            return ((this.sPanelName == null) || this.sPanelName.equals(STRING_TITLE_DIALOG_MANUAL));
        }
        
        // last value storing
        private class LastValue{
            public final String sIdentifierName;
            public int iLastValue;

            public LastValue(String sIdentifierName, int iLastValue){
                this.sIdentifierName = sIdentifierName;
                this.iLastValue = iLastValue;
            }
        }
        private Integer findLastValueEntry(String sIdentifier){
            for (int iIndex = 0; iIndex < lastValueTable.size();iIndex++){
                if (lastValueTable.get(iIndex).sIdentifierName.equals(sIdentifier)){
                    return iIndex;
                }
            }
            return null;
        }
        private void setLastValue(){
            Integer iIndex = findLastValueEntry(this.sPanelName);
            if (iIndex != null){
                lastValueTable.get(iIndex).iLastValue = this.iValue;
            } else {
                lastValueTable.add(new LastValue(this.sPanelName, INT_VALUE_DEFAULT_DATAVALUE));
            }
        }
        private int getLastValue(String sIdentifier){
            Integer iIndex = findLastValueEntry(sIdentifier);
            if (iIndex != null){
                return lastValueTable.get(iIndex).iLastValue;
            } else {
                return INT_VALUE_DEFAULT_DATAVALUE;
            }
        }
    }
    private class ManualDisplayPanel extends DisplayPanel{
        private final String STRING_MESSAGE_TITLE_FORMATERROR = "Format error!";
        private final String STRING_MESSAGE_BODY_OUTOFRANGE = "Value not in expected range!";
        
        private final String STRING_LABEL_IDENTIFIEREDITOR = "Identifier";
        private final String STRING_LABEL_DATAEDITOR = "Data";
        private final String STRING_LABEL_SIGNEDINDICATOR = "Signed state";
        private final String STRING_LABEL_BYTECOUNTINDICATOR = "Max bytecount";
        private final String STRING_LABEL_DECIMALSELECTOR = "Input interpretes decimal";
        private final String STRING_LABEL_HEXSELECTOR = "Input interpretes hex";
        
        private final String STRING_STATE_SIGNED = "Signed";
        private final String STRING_STATE_UNSIGNED = "Unsigned";
        
        private final ManualDisplayItem identifierEditor = new ManualDisplayItem(STRING_LABEL_IDENTIFIEREDITOR);
        private final ManualDisplayItem dataEditor = new ManualDisplayItem(STRING_LABEL_DATAEDITOR);
        
        public ManualDisplayPanel(){
            super();
            
            // init
            setLayout(new GridBagLayout());
            identifierEditor.setByteCount(1);
            identifierEditor.setSigned(false);
            
            // adding
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.anchor = GridBagConstraints.PAGE_START;
            constraints.weightx = 1;
            constraints.gridwidth = 1;
            constraints.gridheight = 1;
            constraints.gridy = 0;
            constraints.weighty = 0.001;
            add(identifierEditor, constraints);
            constraints.gridy = 1;
            add(dataEditor, constraints);
            constraints.gridy = 2;
            constraints.weighty = 1;
            add(Box.createVerticalGlue(), constraints);
        }

        @Override
        public final void prepare(int iParentUsagePage, HidElementTableRow oldRow) {
            byte bIdentifier = formatTable.findIdentifier(getName());
            int iTag = decodeElementTag(bIdentifier);
            int iType = decodeElementType(bIdentifier);
            
            dataEditor.setByteCount(formatTable.findMaxDataLength(iTag, iType));
            dataEditor.setSigned(formatTable.findSignedState(iTag, iType));
            
            identifierEditor.setValue(removeByteCount(bIdentifier));
            dataEditor.setValue(getValue());
            
            identifierEditor.setEnabled(isManual());
        }
        @Override
        public final boolean compute(){
            try {
                if (isManual()){setManualIdentifier(addByteCount((byte)identifierEditor.getValue(), 0));}
                setValue(dataEditor.getValue());
                return true;
            } catch(RuntimeException ex){
                JOptionPane.showMessageDialog(parent, ex.getMessage(), STRING_MESSAGE_TITLE_FORMATERROR, JOptionPane.OK_OPTION + JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        private class ManualDisplayItem extends JPanel implements ActionListener{
            private final JLabel signedLabel = new JLabel(String.format("%s: ", STRING_LABEL_SIGNEDINDICATOR));
            private final JLabel signedDisplay = new JLabel();
            private final JLabel bytecountLabel = new JLabel(String.format("%s: ", STRING_LABEL_BYTECOUNTINDICATOR));
            private final JLabel bytecountDisplay = new JLabel();
            private final JRadioButton decimalSelect = new JRadioButton(STRING_LABEL_DECIMALSELECTOR);
            private final JRadioButton hexSelect = new JRadioButton(STRING_LABEL_HEXSELECTOR);
            private final JTextField dataInput = new JTextField();
            
            private final ManualDisplayItem item;
            private boolean bSigned = false;
            private int iByteCount = 0;
            private boolean bHex = true;
            
            public ManualDisplayItem(String sLabelName){
                super();
                this.item = this;

                // component init
                setLayout(new GridBagLayout());
                setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(COLOR_ITEMBORDER), sLabelName));
                decimalSelect.setSelected(!this.bHex);
                hexSelect.setSelected(this.bHex);
                
                // adding
                GridBagConstraints constraints = new GridBagConstraints();
                constraints.gridheight = 1;
                constraints.gridwidth = 1;
                constraints.weightx = 1;
                constraints.ipady = 3;
                constraints.anchor = GridBagConstraints.LINE_START;
                constraints.fill = GridBagConstraints.HORIZONTAL;
                constraints.gridy = 0;
                add(signedLabel, constraints);
                add(signedDisplay, constraints);
                constraints.gridy = 1;
                add(bytecountLabel, constraints);
                add(bytecountDisplay, constraints);
                constraints.gridwidth = 2;
                constraints.gridy = 2;
                add(decimalSelect, constraints);
                constraints.gridy = 3;
                add(hexSelect, constraints);
                constraints.gridy = 4;
                add(dataInput, constraints);
                
                // events
                decimalSelect.addActionListener(this.item);
                hexSelect.addActionListener(this.item);
                dataInput.addActionListener(this.item);
            }
            
            public final int getValue() throws RuntimeException{
                long iValue = toValue(dataInput.getText());
                if (!isRangeValidValue(iValue, this.iByteCount, this.bSigned)){
                    throw new IllegalArgumentException(STRING_MESSAGE_BODY_OUTOFRANGE);
                }
                return (int)iValue;
            }
            
            public void setValue(int iValue){
                dataInput.setText(toDisplay(iValue));
            }
            public final void setByteCount(int iByteCount){
                this.iByteCount = iByteCount;
                refreshByteCountDisplay();
            }
            public final void setSigned(boolean bSigned){
                this.bSigned = bSigned;
                refreshSignedDisplay();
            }
            @Override
            public void actionPerformed(ActionEvent evt) {
                Object source = evt.getSource();
                if (source.equals(decimalSelect) || source.equals(hexSelect)) {
                    try{
                        long iValue = toValue(dataInput.getText());
                        this.bHex = !this.bHex;
                        dataInput.setText(toDisplay(iValue));
                    }catch(RuntimeException ex){
                        JOptionPane.showMessageDialog(parent, ex.getMessage(), STRING_MESSAGE_TITLE_FORMATERROR, 
                                JOptionPane.OK_OPTION + JOptionPane.ERROR_MESSAGE);
                    }
                    hexSelect.setSelected(this.bHex);
                    decimalSelect.setSelected(!this.bHex);
                } else if (source.equals(dataInput)){
                    closeDialog(CONFIRMED);
                }
            }
            @Override
            public final void setEnabled(boolean bEnabled){
                super.setEnabled(bEnabled);
                signedLabel.setEnabled(bEnabled);
                signedDisplay.setEnabled(bEnabled);
                bytecountLabel.setEnabled(bEnabled);
                bytecountDisplay.setEnabled(bEnabled);
                decimalSelect.setEnabled(bEnabled);
                hexSelect.setEnabled(bEnabled);
                dataInput.setEnabled(bEnabled);
            }
            
            private String toDisplay(long iValue){
                String sResult;
                if (this.bHex){
                    sResult = String.format("%08X", iValue);
                    sResult = sResult.substring(sResult.length() - this.iByteCount * 2);
                } else {
                    if(this.bSigned){
                        sResult = String.valueOf(iValue);
                    } else{
                        sResult = Long.toUnsignedString(toUnsignedValue(iValue, this.iByteCount));
                    }
                }
                return sResult;
            }
            private long toValue(String sDisplay) throws NumberFormatException {
                long iResult;
                if (this.bHex){
                    iResult = Long.parseUnsignedLong(sDisplay, 16);
                    if (this.bSigned) {iResult = (int)iResult;}
                } else {
                    iResult = Long.parseLong(sDisplay, 10);
                }
                return iResult;
            }
            private void refreshByteCountDisplay(){
                bytecountDisplay.setText(String.valueOf(this.iByteCount));
            }
            private void refreshSignedDisplay(){
                if (this.bSigned){
                    signedDisplay.setText(STRING_STATE_SIGNED);
                }else{
                    signedDisplay.setText(STRING_STATE_UNSIGNED);
                }
            }
        }
    }
    private class BinaryDisplayPanel extends DisplayPanel{
        public BinaryDisplayPanel(HidBinaryTable binaryTable){
            super();
            
            // init
            setLayout(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            
            // adding
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.anchor = GridBagConstraints.FIRST_LINE_START;
            constraints.gridheight = 1;
            constraints.gridwidth = 1;
            constraints.weightx = 0.25;
            constraints.weighty = 0.001;
            var oContainer = new Object(){int iCount = 0;};
            binaryTable.getItemList().forEach(item ->{
                constraints.gridx = oContainer.iCount % 4;
                constraints.gridy = oContainer.iCount / 4;
                oContainer.iCount++;
                add(new BinaryDisplayItem(item), constraints);
            });
            constraints.weighty = 1;
            add(Box.createVerticalGlue(), constraints);
        }
        @Override
        public final void prepare(int iParentUsagePage, HidElementTableRow oldRow) {
            int iValue = getValue();
            for (Component comp : getComponents()){
                if (comp instanceof BinaryDisplayItem) {
                    ((BinaryDisplayItem)comp).setState(iValue);
                }
            }
        }
        @Override
        public final boolean compute(){
            int iValue = 0;
            for (Component comp : getComponents()){
                if (comp instanceof BinaryDisplayItem) {
                    iValue |= ((BinaryDisplayItem)comp).getState();
                }
            }
            setValue(iValue);
            return true;
        }
        
        private class BinaryDisplayItem extends JPanel implements ActionListener{
            private final String STRING_TITLE_ITEMBORDER = "Bit";

            private final BinaryDisplayItem displayItem;

            private final int iBitPosition;

            private final JRadioButton falseButton;
            private final JRadioButton trueButton;
            
            private boolean bState = false;
            
            public BinaryDisplayItem(BinaryTableItem item){
                super();
                this.displayItem = this;
                this.iBitPosition = item.getBitPosition();

                // component init
                setLayout(new GridLayout(2, 1));
                setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(COLOR_ITEMBORDER), 
                        String.format("%s: %d", STRING_TITLE_ITEMBORDER, this.iBitPosition)));
                falseButton = new JRadioButton(item.getFalseText());
                trueButton = new JRadioButton(item.getTrueText());
                add(falseButton);
                add(trueButton);

                // events init
                falseButton.addActionListener(this.displayItem);
                trueButton.addActionListener(this.displayItem);
            }

            @Override
            public void actionPerformed(ActionEvent evt) {
                this.bState = !this.bState;
                refreshDisplayState();
            }
            public final int getState(){
                int iResult = (bState ? 1 : 0);
                return (iResult << this.iBitPosition);
            }
            public final void setState(int iValue){
                this.bState = (((iValue >>> this.iBitPosition) & 0x01) > 0);
                refreshDisplayState();
            }
            private void refreshDisplayState(){
                falseButton.setSelected(!this.bState);
                trueButton.setSelected(this.bState);
            }
        }
    }
    private class SelectorDisplayPanel extends DisplayPanel implements MouseListener,ActionListener{
        private final String STRING_LABEL_PARENTLIST = "Usagen Page";
        private final String STRING_LABEL_SELECTLIST = "Usage";
        private final String STRING_LABEL_USEPARENTBOX = "Nested Usage Page";
        
        private final SelectorPanelItem conditionalList = new SelectorPanelItem(STRING_LABEL_PARENTLIST);
        private final SelectorPanelItem selectList = new SelectorPanelItem(STRING_LABEL_SELECTLIST);
        private final JCheckBox useNestedCheckBox = new JCheckBox(STRING_LABEL_USEPARENTBOX);
        
        private final SelectorDisplayPanel panel;
        private final HidSelectorTable table;
        private final HidSelectorTable parentTable;
        private int iExternalParentUsagePage = INT_VALUE_DEFAULT_DATAVALUE;
        
        public SelectorDisplayPanel(HidSelectorTable selectorTable){
            super();
            this.panel = this;
            this.table = selectorTable;
            
            // list preparation
            boolean bConditional = this.table.isConditinal();
            conditionalList.setVisible(bConditional);
            useNestedCheckBox.setVisible(bConditional);
            if (!bConditional){
                this.parentTable = null;
                selectorTable.getItemList().forEach(item -> {
                    selectList.add(item.getName());
                });
            } else {
                HidBaseTable parent = formatTable.findTableByFile(selectorTable.getConditionFile());
                if (parent.getType() == HidTableType.selector){
                    this.parentTable = (HidSelectorTable)parent;
                    this.parentTable.getItemList().forEach(item -> {
                        conditionalList.add(item.getName());
                    });
                } else {
                    this.parentTable = null;
                }
            }
            
            // component adding
            setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
            JPanel listPanel = new JPanel();
            listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.LINE_AXIS));
            listPanel.add(conditionalList);
            listPanel.add(selectList);
            add(listPanel);
            useNestedCheckBox.setAlignmentX(Component.CENTER_ALIGNMENT);
            add(useNestedCheckBox);
            
            // events
            selectList.addMouseListener(this.panel);
            conditionalList.addMouseListener(this.panel);
            useNestedCheckBox.addActionListener(this.panel);
        }

        @Override
        public final void prepare(int iParentUsagePage, HidElementTableRow oldRow) {
            this.iExternalParentUsagePage = iParentUsagePage;
            if (this.table.isConditinal()){
                boolean bUseNested = (oldRow != null) ? oldRow.hasNestedUsagePage() : false;
                useNestedCheckBox.setSelected(bUseNested);
                conditionalList.setEnabled(bUseNested);
                int iParentIndex;
                if (bUseNested && (oldRow != null)){
                    iParentIndex = oldRow.getNestedUsagePage().getValue();
                } else {
                    iParentIndex = this.iExternalParentUsagePage;
                }
                conditionalList.setSelection(this.parentTable.findItemName(iParentIndex));
                refreshSelectBox();
                selectList.setSelection(this.table.findItemName(getValue(), iParentIndex));
            } else {
                selectList.setSelection(this.table.findItemName(getValue()));
            }
        }
        @Override
        public final boolean compute(){
            int iValue; 
            if (table.isConditinal()){
                int iParentIndex = this.parentTable.findIndex(conditionalList.getSelection());
                iValue = this.table.findIndex(selectList.getSelection(), iParentIndex);
                if (useNestedCheckBox.isSelected()){
                    iValue = encodeNestedUsagePage(iValue, iParentIndex);
                }
            } else {
                iValue = this.table.findIndex(selectList.getSelection());
            }
            setValue(iValue);
            return true;
        }
        @Override
        public void actionPerformed(ActionEvent evt) {
            Object source = evt.getSource();
            if (source.equals(useNestedCheckBox)) {
                boolean bSelected = useNestedCheckBox.isSelected();
                conditionalList.setEnabled(bSelected);
                if (!bSelected) {
                    conditionalList.setSelection(this.parentTable.findItemName(this.iExternalParentUsagePage));
                    refreshSelectBox();
                    selectList.setSelection(this.table.findItemName(getValue(), this.iExternalParentUsagePage));
                }
            }
        }
        @Override
        public void mouseClicked(MouseEvent evt) {
            Object source = evt.getSource();
            if (source.equals(selectList.getList())){
                if (evt.getClickCount() % 2 == 0){
                    closeDialog(CONFIRMED);
                }
            }  else if (source.equals(conditionalList.getList())) {
                int iParentIndex = this.parentTable.findIndex(conditionalList.getSelection());
                refreshSelectBox();
                selectList.setSelection(this.table.findItemName(INT_VALUE_DEFAULT_DATAVALUE, iParentIndex));
            }
        }
        @Override
        public void mousePressed(MouseEvent evt) {}
        @Override
        public void mouseReleased(MouseEvent evt) {}
        @Override
        public void mouseEntered(MouseEvent evt) {}
        @Override
        public void mouseExited(MouseEvent evt) {}
        
        private void refreshSelectBox(){
            int iParentIndex = this.parentTable.findIndex(conditionalList.getSelection());
            selectList.clear();
            this.table.getItemList(iParentIndex).forEach(item -> {
                if (item.isSelectable()) {
                    selectList.add(item.getName());
                }
            });
        }
        private class SelectorPanelItem extends JScrollPane{
            private final JList<String> list = new JList<>();
            private final DefaultListModel<String> listModel = new DefaultListModel<>();
            
            public SelectorPanelItem(String sLabelName){
                super();
                
                // list preparation 
                list.setModel(listModel);
                list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                add(list);
                setViewportView(list);
                setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(COLOR_ITEMBORDER), sLabelName));
            }
            public final void add(String sItem){
                listModel.addElement(sItem);
            }
            public final void add(String sItem, int iIndex){
                try{
                    listModel.add(iIndex, sItem);
                }catch(ArrayIndexOutOfBoundsException ex){
                    listModel.addElement(sItem);
                }
            }
            @Override
            public final void addMouseListener(MouseListener listener){
                list.addMouseListener(listener);
            }
            public final void clear(){
                listModel.clear();
            }
            public final JList getList(){
                return list;
            }
            public final String getSelection(){
                return list.getSelectedValue();
            }
            @Override
            public final void setEnabled(boolean bEnabled){
                super.setEnabled(bEnabled);
                list.setEnabled(bEnabled);
            }
            public final void setSelection(String sName){
                if (listModel.contains(sName)){
                    list.setSelectedValue(sName, true);
                } else {
                    list.setSelectedIndex(INT_VALUE_DEFAULT_DATAVALUE);
                }
            }
        }
    }
    private class UnitDisplayPanel extends DisplayPanel implements ActionListener{
        private final String STRING_MESSAGE_TITLE_UNITFORMATERROR = "Unit Input Error";
        private final String STRING_MESSAGE_BODY_NOTANUMBER = "Value is not a number!";
        private final String STRING_MESSAGE_BODY_NOTINRANGE = "Number is not in valid Range!";
        private final String STRING_MESSAGE_BODY_NOSYSTEMSELECTED = "No unit system selected!";
        
        private final String STRING_LABEL_UNITPICKER = "Quick Unit Picker";
        private final String STRING_LABEL_SYSTEMPICKER = "System Picker";
        private final String STRING_LABEL_LENGTHSELECTOR = STRING_DISPLAY_UNITNAME_LENGTH + " Exp";
        private final String STRING_LABEL_MASSSELECTOR = STRING_DISPLAY_UNITNAME_MASS + " Exp";
        private final String STRING_LABEL_TIMESELECTOR = STRING_DISPLAY_UNITNAME_TIME + " Exp";
        private final String STRING_LABEL_TEMPSELECTOR = STRING_DISPLAY_UNITNAME_TEMPERATURE + " Exp";
        private final String STRING_LABEL_CURRENTSELECTOR = STRING_DISPLAY_UNITNAME_CURRENT + " Exp";
        private final String STRING_LABEL_LUMENSELECTOR = STRING_DISPLAY_UNITNAME_LUMEN + " Exp";
        
        private final JComboBox<String> quickSelector = new JComboBox<>();
        private final JComboBox<String> systemSelector = new JComboBox<>();
        private final JTextField lengthSelector = new JTextField();
        private final JTextField massSelector = new JTextField();
        private final JTextField timeSelector = new JTextField();
        private final JTextField temperatureSelector = new JTextField();
        private final JTextField currentSelector = new JTextField();
        private final JTextField lumenSelector = new JTextField();
        
        private final UnitDisplayPanel panel;
        private final HidUnitTable table;
        private boolean bInitializing = true;
        
        public UnitDisplayPanel(HidUnitTable unitTable){
            super();
            this.panel = this;
            this.table = unitTable;
            
            // layouting
            setLayout(new GridLayout(8, 1));
            add(new UnitPanelItem(STRING_LABEL_UNITPICKER, quickSelector));
            add(new UnitPanelItem(STRING_LABEL_SYSTEMPICKER, systemSelector));
            add(new UnitPanelItem(STRING_LABEL_LENGTHSELECTOR, lengthSelector));
            add(new UnitPanelItem(STRING_LABEL_MASSSELECTOR, massSelector));
            add(new UnitPanelItem(STRING_LABEL_TIMESELECTOR, timeSelector));
            add(new UnitPanelItem(STRING_LABEL_TEMPSELECTOR, temperatureSelector));
            add(new UnitPanelItem(STRING_LABEL_CURRENTSELECTOR, currentSelector));
            add(new UnitPanelItem(STRING_LABEL_LUMENSELECTOR, lumenSelector));
            
            // init boxitems
            unitTable.getItemList().forEach(item -> {
                quickSelector.addItem(item.getName());
            });
            for (HidUnitSystems system : HidUnitSystems.values()){
                systemSelector.addItem(system.getDisplayName());
            }
            
            // events
            quickSelector.addActionListener(this.panel);
            systemSelector.addActionListener(this.panel);
            lengthSelector.addActionListener(this.panel);
            massSelector.addActionListener(this.panel);
            timeSelector.addActionListener(this.panel);
            temperatureSelector.addActionListener(this.panel);
            currentSelector.addActionListener(this.panel);
            lumenSelector.addActionListener(this.panel);
        }
        @Override
        public final void prepare(int iParentUsagePage, HidElementTableRow oldRow) {
            refreshDisplay(getValue());
        }
        @Override
        public final boolean compute(){
            try{
                setValue(combineInputs());
                return true;
            }catch(IllegalArgumentException ex){
                JOptionPane.showMessageDialog(parent, ex.getMessage(), STRING_MESSAGE_TITLE_UNITFORMATERROR, JOptionPane.OK_OPTION + JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        @Override
        public void actionPerformed(ActionEvent evt) {
            if (!this.bInitializing) {
                Object source = evt.getSource();
                if (source.equals(quickSelector)) {
                    int iValue = this.table.findItemValue(String.valueOf(quickSelector.getSelectedItem()));
                    refreshDisplay(iValue);
                } else if(source.equals(systemSelector)){
                    try{
                        refreshDisplay(combineInputs());
                    } catch(IllegalArgumentException ex){}
                } else if(source instanceof JTextField){
                    try{
                        refreshDisplay(combineInputs());
                    } catch(IllegalArgumentException ex){
                        JOptionPane.showMessageDialog(parent, ex.getMessage(), STRING_MESSAGE_TITLE_UNITFORMATERROR, JOptionPane.OK_OPTION + JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }
        
        private byte convertTextInput(String sValue, String sValueName) throws IllegalArgumentException{
            try {
                int iValue = Integer.valueOf(sValue);
                if (!HidUnitSystems.isRangeValidExp(iValue)){throw new IllegalArgumentException(String.format("%s - %s", sValueName, STRING_MESSAGE_BODY_NOTINRANGE));}
                return (byte)iValue;
            }catch (NumberFormatException ex){
                throw new IllegalArgumentException(String.format("%s - %s", sValueName, STRING_MESSAGE_BODY_NOTANUMBER));
            }
        }
        private int combineInputs() throws IllegalArgumentException {
            byte bSystem = HidUnitSystems.findSystemValue(String.valueOf(systemSelector.getSelectedItem()));
            byte bLength = convertTextInput(lengthSelector.getText(), STRING_LABEL_LENGTHSELECTOR);
            byte bMass = convertTextInput(massSelector.getText(), STRING_LABEL_MASSSELECTOR);
            byte bTime = convertTextInput(timeSelector.getText(), STRING_LABEL_TIMESELECTOR);
            byte bTemp = convertTextInput(temperatureSelector.getText(), STRING_LABEL_TEMPSELECTOR);
            byte bCurrent = convertTextInput(currentSelector.getText(), STRING_LABEL_CURRENTSELECTOR);
            byte bLumen = convertTextInput(lumenSelector.getText(), STRING_LABEL_LUMENSELECTOR);
            if (((bLength != 0) || (bMass != 0) || (bTime != 0) || (bTemp != 0) || (bCurrent != 0) || (bLumen != 0)) && bSystem == 0) {
                throw new IllegalArgumentException(STRING_MESSAGE_BODY_NOSYSTEMSELECTED);}
            return HidUnitSystems.buildUnit(bSystem, bLength, bMass, bTime, bTemp, bCurrent, bLumen);
        }
        private void refreshDisplay(int iValue){
            this.bInitializing = true;
            
            String sQuickUnit = this.table.findItemName(iValue);
            quickSelector.setSelectedItem(sQuickUnit);
            
            String sSystemName = HidUnitSystems.findSystemName(iValue);
            systemSelector.setSelectedItem(sSystemName);
            
            lengthSelector.setText(String.valueOf((int)HidUnitSystems.decodeLength(iValue)));
            massSelector.setText(String.valueOf((int)HidUnitSystems.decodeMass(iValue)));
            timeSelector.setText(String.valueOf((int)HidUnitSystems.decodeTime(iValue)));
            temperatureSelector.setText(String.valueOf((int)HidUnitSystems.decodeTemperature(iValue)));
            currentSelector.setText(String.valueOf((int)HidUnitSystems.decodeCurrent(iValue)));
            lumenSelector.setText(String.valueOf((int)HidUnitSystems.decodeLumen(iValue)));
            
            this.bInitializing = false;
        }
        private class UnitPanelItem extends JPanel{
            public UnitPanelItem(String sLabelName, Component comp){
                setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
                setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(COLOR_ITEMBORDER), sLabelName));
                add(comp);
            }
        }
    }
}
