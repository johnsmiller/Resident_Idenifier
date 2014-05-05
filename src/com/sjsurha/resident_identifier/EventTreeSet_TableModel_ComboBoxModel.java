package com.sjsurha.resident_identifier;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ComboBoxModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

/**
 *
 * @author John Miller
 */
/**
 * TreeSet<Event> class that implements the TableModel and ComboBoxModel interfaces
 */
public class EventTreeSet_TableModel_ComboBoxModel extends TreeSet<Event> implements TableModel, ComboBoxModel<Event>
{
    private static final long serialVersionUID = 1L;
    private final String[] headers = {"Select", "Event Date", "Event Name", "Attendees", "Waitlisted"};
    private boolean[] tableBooleanSelection;
    private static HashSet<TableModelListener> tableModelListeners;
    private static HashSet<ListDataListener> comboBoxModelListDataListeners;
    Event[] modelEventArray;
    private Object comboBoxModelSelectedItem;

    public EventTreeSet_TableModel_ComboBoxModel()
    {
        super();
        tableBooleanSelection = new boolean[0];
        tableModelListeners = new HashSet<>(5);
        comboBoxModelListDataListeners = new HashSet<ListDataListener>(10);
        modelEventArray = this.toArray(new Event[super.size()]);
    }
    
    static 
    {
        tableModelListeners = new HashSet<>(5);
        comboBoxModelListDataListeners = new HashSet<ListDataListener>(10);
    }

    @Override
    public boolean add(Event e)  //Modify to take in Authenication ID for future logging function
    {
        if(super.add(e)) {
            comboBoxModelSelectedItem = e;
            modelListenersNotify();
            ViewerController.saveModel();
            return true;
        }
        return false;
    }

    @Override
    public boolean remove(Object o) //Modify to take in Authenication ID for future logging function
    {
        if(super.remove(o)){
            if(comboBoxModelSelectedItem.equals(o))
                comboBoxModelSelectedItem = ((this.size()>0)? this.first() : null);
            modelListenersNotify();
            ViewerController.saveModel();
            return true;
        }
        return false;
    }

    @Override
    public boolean addAll(Collection<? extends Event> c) //Modify to take in Authenication ID for future logging function
    {
        if(super.addAll(c)){
            modelListenersNotify();
            ViewerController.saveModel();
            return true;
        }
        return false;  
    }



    @Override
    public void clear() //Modify to take in Authenication ID for future logging function
    {
        super.clear();
    }

    /*
     * Ensures all implemented interfaces' listeners are notified if model data changes
     */

    private void modelListenersNotify()
    {
        modelEventArray = this.toArray(new Event[this.size()]);
        notifyTableModelChange();
        notifyComboBoxModelChange(0, this.size()-1);
    }

    /**
     * Ensures all implemented interfaces' listeners are notified if model data changes
     * @param index0 beginning index of change
     * @param index1 ending index of change
     */

    private void modelListenersNotify(int index0, int index1)
    {
        modelEventArray = this.toArray(new Event[this.size()]);
        notifyTableModelChange(index0, index1);
        notifyComboBoxModelChange(index0, index1);

    }


    /**
     * Removes all model listeners to ensure clean program exit (no additional threads running)
     */

    public void removeAllListeners()
    {
        tableModelListeners.clear();
        comboBoxModelListDataListeners.clear();
    }



    //TABLE MODEL IMPLEMENTATION

    private void notifyTableModelChange()
    {
        tableBooleanSelection = new boolean[super.size()];
        for(TableModelListener t : tableModelListeners)
            t.tableChanged(new TableModelEvent(this));
    }

    private void notifyTableModelChange(int index0, int index1)
    {
        tableBooleanSelection = new boolean[super.size()];
        for(TableModelListener t : tableModelListeners)
            t.tableChanged(new TableModelEvent(this, index0, index1));
    }

    @Override
    public int getRowCount() {
        return this.size();

    }

    @Override
    public int getColumnCount() {
        return headers.length;
    }

    @Override
    public String getColumnName(int columnIndex) {
        if(columnIndex>-1 && columnIndex < getColumnCount())
            return headers[columnIndex];
        return null;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if(getValueAt(0, columnIndex) == null )
            return Object.class;
        return getValueAt(0, columnIndex).getClass();
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if (columnIndex != 0)
            return false;
        return true;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        switch (columnIndex)
        {
            case 0:
                return tableBooleanSelection[rowIndex];
            case 1:
                return modelEventArray[rowIndex].getShortDate() + " " + modelEventArray[rowIndex].getTime();
            case 2:
                return modelEventArray[rowIndex].getName();
            case 3:
                return modelEventArray[rowIndex].getAttendees().size();
            case 4:
                return modelEventArray[rowIndex].getWaitinglist().size();
            case 5:
                return modelEventArray[rowIndex];
            default:
                return null;
        }
    }


    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if(columnIndex == 0)
            tableBooleanSelection[rowIndex] = (boolean) aValue;
    }

    @Override
    public void addTableModelListener(TableModelListener l) {
        tableModelListeners.add(l);
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
        tableModelListeners.remove(l);
    }

    //COMBOBOX MODEL IMPLEMENTATION

    /**
     * used by ComboBoxModel class to notify of data changes
     * @param index0 beginning index of changes
     * @param index1 ending index of changes
     */
    private void notifyComboBoxModelChange(int index0, int index1)
    {
        for(ListDataListener l : comboBoxModelListDataListeners)
            l.contentsChanged(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, index0, index1));
    }

    @Override
    public void setSelectedItem(Object anItem) {
        comboBoxModelSelectedItem = anItem;
        notifyComboBoxModelChange(0, getSize()-1); //Should this be the indexes of selected/deselected?
    }

    @Override
    public Object getSelectedItem() {
        return comboBoxModelSelectedItem;
    }

    @Override
    public int getSize() {
        return this.size();
    }

    @Override
    public Event getElementAt(int index) {
        if(index < modelEventArray.length)
            return modelEventArray[index];
        return null;
    }

    @Override
    public void addListDataListener(ListDataListener l) {
        comboBoxModelListDataListeners.add(l);
    }

    @Override
    public void removeListDataListener(ListDataListener l) {
        comboBoxModelListDataListeners.remove(l);
    }

}

