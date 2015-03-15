/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sjsurha.resident_identifier;

import java.io.Serializable;
import java.util.HashMap;
import java.util.TreeSet;
import javax.swing.JComboBox;
import javax.swing.JTable;

/**
 *
 * @author John
 */
public class Resident_Event_Database implements Serializable{
    private static final long serialVersionUID = 1;    
    
    private final TreeSet_TableModel_ComboBoxModel<Building> buildings; //Will hold the names of all builings. Considerations: Excel Import, Manual Creation, Database merge. Clear on residents clear.
    private final TreeSet_TableModel_ComboBoxModel<Event> events;
    private final HashMap<String, Resident> residents;
    
    public final static String[] EVENT_HEADERS = {"Select", "Event Date", "Event Name", "Attendees", "Waitlisted"};
    public final static String[] BUIDLING_HEADERS = {"Allowed?", "Building"};
    public final static String BUILDING_ID_DELIMITER = "-";
    
    public Resident_Event_Database()
    {
        buildings = new TreeSet_TableModel_ComboBoxModel<>(BUIDLING_HEADERS);
        events = new TreeSet_TableModel_ComboBoxModel<>(EVENT_HEADERS);
        residents = new HashMap<>();
    }
    
    public Resident_Event_Database(TreeSet<Building> buildRec, TreeSet<Event> eventRec, HashMap<String, Resident> residentsRec)
    {
        this();
        if(buildRec != null)
            buildings.addAll(buildRec);
        if(eventRec != null)
            events.addAll(eventRec);
        if(residentsRec != null)
            residents.putAll(residentsRec);
    }
    
    /**
     * 
     * @param id
     * @return
     */
    protected synchronized boolean checkResident(String id)
    {
        //RequestCheck();
        return residents.containsKey(id);
    }
    
    /**
     * Looks up the Student ID in stored residents & extracts the building ID
     * using the predefined string delimiter.
     * 
     * Building ID is added to list of building IDs if it does not exist already
     * 
     * Returns null if student doesn't exist.
     * 
     * @param ID the student ID to get the Building ID from
     * @return the Building ID or null
     */
    public Building extractBuilding(String ID)
    {
        Resident res = getResident(ID);
        if(res == null)
            return null;
        
        int delmiterIndex = (res.getBedspace().contains(BUILDING_ID_DELIMITER))? res.getBedspace().indexOf(BUILDING_ID_DELIMITER) : res.getBedspace().length();
        String building = res.getBedspace().toUpperCase().substring(0, delmiterIndex);
        Building b = new Building(building);
        buildings.add(b);
        return b;
    }
        
    /**
     * return size of resident database
     * @return number of residents contained in database
     */
    protected int residentCount()
    {
        return residents.size();
    }
    
    /**
     * Returns true if event with same name and date/time exists
     * @param event the event to check against
     * @return true if given event exists
     */
    
    protected synchronized boolean ContainsEvent(Event event)
    {
        return events.contains(event);
    }
    
    /**
     * Returns true if the set contained the element and was removed
     * @param event the event to remove from the model
     */
    protected synchronized void removeEvent(Event event)
    {
        events.remove(event);
    }
    
    /**
     * Adds the given event to the model if it does not already contain an event
     * with the same name and date/time. Otherwise, nothing changes
     * @param event the event to add to the event
     * @return true if the set changed as a result of this call
     */
    
    protected synchronized boolean addEvent(Event event)
    {
        return events.add(event);
    }
    
    protected synchronized void emptyEventDatabase() {
        events.clear();
    }    
    
    /**
     * Returns a JComboBox that uses the Events collection as a 
     * ComboBoxModel
     * 
     * Future implementation will disallow accessing the getModel function of
     * this JComboBox
     * 
     * @return JComboBox based off the events collection
     */
    
    protected JComboBox getEventsJComboBox()
    {
        return new JComboBox(events);
    }
    
    /**
     * Returns a JTable that uses the Events collection as a
     * JTableModel
     * 
     * Future implementation will disallow accessing the getModel function of
     * this JTable
     * 
     * @return JTable based off the events collection
     */
    
    protected JTable getEventsJTable()
    {
        JTable ret = new JTable();
        ret.setModel(events);
        return ret;
    }
    
    protected JTable getBuildingJTable()
    {            
        JTable ret = new JTable();
        ret.setModel(buildings);
        return ret;
    }
    
    /**
     * Standardized way of returning the size of the events collection across
     * different model implementations 
     * 
     * @return an integer equal to the number of events in database
     */
    protected int eventCount()
    {
        return events.size();
    }
    
    protected int buildingCount()
    {
        return buildings.getSize();
    }
    
    /**
     * Iterates over all events and checks if the resident attended them. 
     * 
     * Checking is O(1) operation, whether attendee or wait listed.
     * 
     * Entire operation is O(n), where n is the number of events in the database
     * 
     * @param ID the ID to search for in all events
     * @return a TreeSet (of type Event) containing all events this resident 
     * attended.
     */
    
    protected TreeSet<Event> getAttendedEvents(String ID)
    {
        TreeSet<Event> ret = new TreeSet<Event>();
        
        for(Event e : events)
        {
            if(e.search(ID))
                ret.add(e);
        }
        
        return ret;
    }
    
    /**
     * Ensures clean program exit by discarding all listener threads
     * that use the TableModel_ComboBox collection
     */
    protected void removeAllListeners()
    {
        events.removeAllListeners();
        buildings.removeAllListeners();
    }

    /**
     * Replaces old values with newly imported if conflict (matching ID) is found
     * @param resident resident to add to database
     * @return always returns true
     */
    protected synchronized boolean addResident(Resident resident) {
        residents.put(resident.getId(), resident);
        return true; //need return value?
    }
    
    protected synchronized void emptyResidentDatabase()
    {
        residents.clear();  
        buildings.clear(); 
    }
    
    protected String[] getNameBedspace(String ID)
    {
        if(checkResident(ID))
        {
            Resident temp = getResident(ID);
            String[] ret = {temp.getLast_name(), temp.getFirst_name(), temp.getBedspace()};
            return ret;
        }
        else
        {
            String[] ret = {"Unknown Resident", "Unknown Resident", "Unknown Resident"};
            return ret;
        }
    }
    
    private synchronized Resident getResident(String ID)
    {
        return residents.get(ID);
    }
    
    protected synchronized void addAllEvents(Resident_Event_Database residentEventDatabaseIn)
    {
        events.addAll(residentEventDatabaseIn.events);
    }
    
    protected synchronized void addAllResidents(Resident_Event_Database residentEventDataBaseIn)
    {
        residents.putAll(residentEventDataBaseIn.residents);
        buildings.addAll(residentEventDataBaseIn.buildings);
    }

}

/*
 /**
     * Early arbitrary function implementation.
     * Returns an ArrayList of all Events in the specified range (inclusive)
     *
     * @param fromDate the beginning date to search from (inclusive)
     * @param toDate the end date to search to (exclusive)
     * @return an ArrayList of Events 
     *
    protected ArrayList<Event> getEventsByDateRange(GregorianCalendar fromDate, GregorianCalendar toDate)
    {
        ArrayList<Event> ret = new ArrayList<>();
        if(fromDate.compareTo(toDate)>0)
        {
            GregorianCalendar temp = fromDate;
            fromDate = toDate;
            toDate = temp;
        }
        for(Event e : events)
        {
            if(e.compareTo(fromDate)>-1 && e.compareTo(toDate)<1)
                ret.add(e);
        }
        return ret;
    }
    
    /**
     * Early and arbitrary implementation that returns various statistics over 
     * a specified range of dates (inclusive) as a String ArrayList
     * 
     * The current data returned includes:
     * Total events in the specified range, 
     * Most attended event (date, time, and number of attendees), 
     * Total number of residents (including duplicates)
     * Total number of unique residents (no duplicates)
     * 
     * @param fromDate the beginning date to start searching from (inclusive)
     * @param toDate the end date of search (inclusive)
     * @return an ArrayList of strings with the specified data
     *
    protected ArrayList<String> getEventStatisticsByDateRange(GregorianCalendar fromDate, GregorianCalendar toDate)
    {
        if(fromDate.compareTo(toDate)>0)
        {
            GregorianCalendar temp = fromDate;
            fromDate = toDate;
            toDate = temp;
        }
        ArrayList<String> ret = new ArrayList<>();
        TreeSet<String> individualAttendees = new TreeSet<>();
        Event maxAttended = events.first();
        int numOfEvents = 0;
        int maxAttendees = 0;
        int totalAttendees = 0;
        for(Event e : events)
        {
            if(e.compareTo(fromDate)>-1 && e.compareTo(toDate)<1){
                numOfEvents++;
                int size = e.getAttendees().size();
                if(size>maxAttendees)
                {
                    maxAttendees = size;
                    maxAttended = e;
                }
                totalAttendees += size;
                individualAttendees.addAll(e.getAttendees().keySet());
            }
        }
        if(numOfEvents!=0){
            ret.add("The following statistics apply to events recorded from " 
                    + fromDate.getDisplayName(Calendar.MONTH,Calendar.LONG, 
                        Locale.ENGLISH)+" "
                    +fromDate.getDisplayName(Calendar.DAY_OF_MONTH, 
                        Calendar.LONG, Locale.ENGLISH)+", "
                    +fromDate.getDisplayName(Calendar.YEAR,Calendar.LONG, 
                        Locale.ENGLISH) 
                    + " to " 
                    + toDate.getDisplayName(Calendar.MONTH,Calendar.LONG, 
                        Locale.ENGLISH)+" "
                    +toDate.getDisplayName(Calendar.DAY_OF_MONTH, Calendar.LONG, 
                        Locale.ENGLISH)+", "
                    +toDate.getDisplayName(Calendar.YEAR,Calendar.LONG, 
                        Locale.ENGLISH));
            
            ret.add("Total Events in date range: " + numOfEvents);
            ret.add("Most attended event: " + maxAttended.getName() + " on " 
                    + maxAttended.getLongDate() + " " + maxAttended.getTime() 
                    + " with " + maxAttendees);
            
            ret.add("Total number of Residents for all events: " 
                    + totalAttendees);
            ret.add("Unique residents who attended at least one event: " 
                    + individualAttendees.size());
        }
        return ret;
    }
*/