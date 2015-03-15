package com.sjsurha.resident_identifier;

import com.sjsurha.resident_identifier.Exceptions.CEAuthenticationFailedException;
import java.io.Serializable;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JTable;

/**
 * Model class contains student and event information. 
 * 
 * @author John Miller
 * @version .7 - Development
 */

//Handle checking in non-residents?
//Pull residence halls' names from bedspace --done
public final class Model implements Serializable{
    private static final long serialVersionUID = 3;
    
    private static Model modelInstance;
    
    //Stored Data Members
    private final Resident_Event_Database residentEventDatabase;
    private final PowerUser_Database powerUserDatabase;   
    
    
    
    /**
     * 
     * @throws CEAuthenticationFailedException 
     */
    private Model() throws CEAuthenticationFailedException
    {
        residentEventDatabase = new Resident_Event_Database();
        powerUserDatabase = new PowerUser_Database();
        
        if(!powerUserCreation(LogEntry.Level.Administrator)) //Adds an admin to ensure administrative functions are operational at run-time
            throw new CEAuthenticationFailedException("Database requires at least 1 Administrator to function");
    }
    
    private Model(Object[][] rescued) throws CEAuthenticationFailedException
    {            
        Object[] databases = restore(rescued);
        residentEventDatabase = (Resident_Event_Database) databases[0];
        powerUserDatabase = (PowerUser_Database) databases[1];

    }
    
    protected static synchronized Model getInstance()
    {
        return modelInstance;
    }
    
    protected static synchronized Model setModelInstance() throws CEAuthenticationFailedException
    {
        modelInstance = new Model();
        return modelInstance;
    }
    
    protected static synchronized Model setModelInstance(Object[][] rescued) throws CEAuthenticationFailedException
    {
        if(rescued != null)
            modelInstance = new Model(rescued);
        return modelInstance;
    }
    
    protected static synchronized Model setModelInstance(Model modelIn)
    {
        if(modelIn != null)
            modelInstance = modelIn;
        return modelInstance;
    }
    
    
    /**
     * Returns true if enough data was restored to allow safe operation of model
     * e.g.: if admins.size()>0 in powerUserDatabase
     * @param rescued
     * @return true if safe to continue with this model
     */
    private Object[] restore(Object[][] rescued) throws CEAuthenticationFailedException
    {
        TreeSet<Event> rescEvent = null; 
        HashMap<String, Resident> rescResident = null;
        TreeSet<Building> rescBuilding = null;
        HashMap<String, String> rescAdmins = null;
        HashMap<String, String> rescUsers = null;
        TreeSet<LogEntry> rescLog = null;
        
        try{
           rescEvent = (TreeSet<Event>) rescued[0][1];
        } catch (ClassCastException ex) { 
        } try {
            rescResident = (HashMap<String, Resident>) rescued[1][1];
        } catch (ClassCastException ex) { 
        } try {
            rescBuilding = (TreeSet<Building>) rescued[2][1];
        } catch (ClassCastException ex) { 
        } try {
            rescAdmins = (HashMap<String, String>) rescued[3][1];
        } catch (ClassCastException ex) { 
        } try {
            rescUsers = (HashMap<String, String>) rescued[4][1];
        } catch (ClassCastException ex) { 
        } try {
            rescLog = (TreeSet<LogEntry>) rescued[5][1];
        } catch (ClassCastException ex) { }
        
        if(rescAdmins == null || rescAdmins.isEmpty()) //to ensure administrative functions are operational at run-time
            throw new CEAuthenticationFailedException("Database requires at least 1 Administrator to function");
        
        Object[] ret = {new Resident_Event_Database(rescBuilding, rescEvent, rescResident), new PowerUser_Database(rescAdmins, rescUsers, rescLog)};
        
        return ret;
        
    }
    
    
    /**
     * Function to import and merge the database with an existing database
     * IMPORTANT: Admins and Events will not import duplicates (thus Events with
     * different attendees/waitlist/maxParticipants, the importing database's 
     * record is kept).
     * 
     * By contrast, duplicate residents are imported and replace the importing 
     * database's records
     * 
     * In future versions of this function, conflicting 
     * Events will be merged and the most recently imported resident will be kept
     * 
     * 
     * @param modelIn the decrypted model to be imported
     * @param importAdmins if true, import non-existing admins into this model
     * @param importUsers if true, import non-existing users into this model
     * @param importEvents if true, import non-existing events into this model
     * @param importResidents if true, import all (replace existing) residents
     */
    
    protected void mergeDatabase(Model modelIn, boolean importAdmins, boolean importUsers, boolean importEvents, boolean importResidents)
    {
        if(modelIn == null)
            return;
        
        //Use modelIn.authenticationModule(LogEntry.Level.Administrator, "Merging this database into another")
        JOptionPane.showMessageDialog(null, "Please provide authentication details for an admin from the previous database on the next screen", "Merging Database Authentication", JOptionPane.INFORMATION_MESSAGE);
        if(!modelIn.authenticationModule(LogEntry.Level.Administrator, "Merging this database into another"))
        {
            JOptionPane.showMessageDialog(null, "Error: the database to be merged did not recognize that administrator. Merge cannot continue", "Merge Database Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        JOptionPane.showMessageDialog(null, "Please provide authentication details for an Admin of THIS database on the next screen", "Current Database Authentication", JOptionPane.INFORMATION_MESSAGE);
        
        if(!authenticationModule(LogEntry.Level.Administrator, "Merging another database into this one"))
        {
            return;
        }
        
        if(importEvents)
        {
            residentEventDatabase.addAllEvents(modelIn.residentEventDatabase);
        }
        
        if(importResidents)
        {
            residentEventDatabase.addAllResidents(modelIn.residentEventDatabase);
        }
        
        if(importAdmins)
        {
            powerUserDatabase.importAdmins(modelIn.powerUserDatabase);
        }
        
        if(importUsers)
        {
            powerUserDatabase.importUsers(modelIn.powerUserDatabase);
        }
        
        powerUserDatabase.importArchivedLog(modelIn.powerUserDatabase);
        
        ViewerController.saveModel();

    }

    //RESIDENCE DATABASE FUNCTION(S)
    
     /**
     * 
     * @param id
     * @return
     */
    protected boolean checkResident(String id)
    {
        //RequestCheck();
        return residentEventDatabase.checkResident(id);
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
        return residentEventDatabase.extractBuilding(ID);
    }
        
    /**
     * return size of resident database
     * @return number of residents contained in database
     */
    protected int residentCount()
    {
        return residentEventDatabase.residentCount();
    }
    
    /**
     * Returns true if event with same name and date/time exists
     * @param event the event to check against
     * @return true if given event exists
     */
    
    protected synchronized boolean ContainsEvent(Event event)
    {
        return residentEventDatabase.ContainsEvent(event);
    }
    
    /**
     * Returns true if the set contained the element and was removed
     * @param event the event to remove from the model
     */
    protected void removeEvent(Event event)
    {
        authenticationModule(LogEntry.Level.Administrator, "Delete Event: " + event.toString());
        residentEventDatabase.removeEvent(event);
    }
    
    /**
     * Adds the given event to the model if it does not already contain an event
     * with the same name and date/time. Otherwise, nothing changes
     * @param event the event to add to the event
     * @return true if the set changed as a result of this call
     */
    
    protected boolean addEvent(Event event)
    {
        return residentEventDatabase.addEvent(event);
    }
    
    protected void emptyEventDatabase() {
        if(JOptionPane.showConfirmDialog(null, "DELETE ALL EVENTS IN DATABASE? \nTHIS CANNOT BE UNDONE", "Delete Events", JOptionPane.OK_CANCEL_OPTION)== JOptionPane.OK_OPTION && authenticationModule(LogEntry.Level.Administrator, "Empty Event Database"))
        {
            residentEventDatabase.emptyEventDatabase();
        }
    }
    
    /**
     * The model is required to implement this function due to the fact that
     * it changes data the event collection uses to sort events.
     * To ensure proper ordering, the event is removed, modified, and
     * reinserted as a new event
     * 
     * @param event the event to be updated
     * @param name the new name of the event (can be the original name)
     * @param dateTime the new date/time of the object (can be original date)
     * @param maxAttendees the new maxAttendees (can be original maxAttendees, 
     * cannot be less than the current number of attendees
     * @param allowedBuildings
     * @return true if the adding of the modified event was successful, false 
     * if not successful
     */
    protected synchronized boolean updateEvent(Event event, String name, GregorianCalendar dateTime, int maxAttendees, Building[] allowedBuildings)
    {
        residentEventDatabase.removeEvent(event);
        event.setName(name);
        event.setDateTime(dateTime);
        event.setMaxParticipants(maxAttendees);
        event.updateBuildings(allowedBuildings);
        return residentEventDatabase.addEvent(event);
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
        return residentEventDatabase.getEventsJComboBox();
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
        return residentEventDatabase.getEventsJTable();
    }
    
    protected JTable getBuildingJTable()
    {            
        JTable ret = residentEventDatabase.getBuildingJTable();
        ret.setAutoCreateRowSorter(true);
        ret.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        ret.setFillsViewportHeight(true);
        ret.getRowSorter().toggleSortOrder(1);
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
        return residentEventDatabase.eventCount();
    }
    
    protected int buildingCount()
    {
        return residentEventDatabase.buildingCount();
    }
    
    /**
     * Ensures clean program exit by discarding all listener threads
     * that use the TableModel_ComboBox collection
     */
    protected void removeAllListeners()
    {
        residentEventDatabase.removeAllListeners();
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
        return residentEventDatabase.getAttendedEvents(ID);
    }
        
    //ADMIN-FUNCTIONS 
    
    protected boolean powerUserRemoval(LogEntry.Level level)
    {
        return powerUserDatabase.powerUserRemovalPopup(level);
    }
    
    protected boolean powerUserCreation(LogEntry.Level level)
    {
        return powerUserDatabase.powerUserCreationPopup(level);
    }
    
    protected boolean authenticationModule(LogEntry.Level level, String descp)
    {
        return powerUserDatabase.authenticationPopup(level, descp);
    }
        
    /**
     * Returns the total number of power users specified by the level
     * 
     * @param level the level of power users to access
     * @return the total number of separate power users registered for that level
     */
    
    protected int powerUserCount(LogEntry.Level level)
    {
        return powerUserDatabase.getPowerUserCount(level);
    }
    
    //Resident Functions
    
    protected boolean addResident(String ID, String lastName, String firstName, String bedSpace)
    {
        if(authenticationModule(LogEntry.Level.Administrator, "Add Resident: " + ID))
        {
            return residentEventDatabase.addResident(new Resident(ID, firstName, lastName, bedSpace));
        }
        return false;
    }
       
    
    protected String[] getNameBedspace(String ID)
    {
        return residentEventDatabase.getNameBedspace(ID);
    }
    
    protected void emptyResidentDatabase()
    {
        if(JOptionPane.showConfirmDialog(null, "DELETE ALL RESIDENTS IN DATABASE? \nTHIS CANNOT BE UNDONE", "Delete Residents", JOptionPane.OK_CANCEL_OPTION)== JOptionPane.OK_OPTION && authenticationModule(LogEntry.Level.Administrator, "Empty Resident Database"))
        { 
                residentEventDatabase.emptyResidentDatabase();
                ViewerController.showTimedInfoDialog("Deletion Successful", "Database Deletion Completed", 2);
        }
    }

    protected String[][] getLogData() {
        return powerUserDatabase.getLogData();
    }
    
    /**
     * Conflicting information is overwritten with new (importing) data
     */
    protected void csvImport()
    {
        HashSet<Resident> newRes = CSV_Import.importCSV();
        for(Resident r : newRes)
        {
            residentEventDatabase.addResident(r);
            residentEventDatabase.extractBuilding(r.getId());
        }
    }
}