package com.sjsurha.resident_identifier;

import com.sjsurha.resident_identifier.Exceptions.CEAuthenticationFailedException;
import com.sjsurha.resident_identifier.Exceptions.CEDuplicateAttendeeException;
import com.sjsurha.resident_identifier.Exceptions.CEMaximumAttendeesException;
import com.sjsurha.resident_identifier.Exceptions.CENonResidentException;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Model class contains student and event information. 
 * 
 * @author John Miller
 * @version .7 - Development
 */

//Handle checking in non-residents?
//Pull residence halls' names from bedspace --done
public final class Model implements Serializable{
    private static final long serialVersionUID = 2;
    
    //Stored Data Members
    private final TreeSet_TableModel_ComboBoxModel<Building> buildings; //Will hold the names of all builings. Considerations: Excel Import, Manual Creation, Database merge. Clear on residents clear.
    private final TreeSet_TableModel_ComboBoxModel<Event> events;
    private final HashMap<String, Resident> residents;
    private final HashMap<String, String> admins;
    private final HashMap<String, String> users;
    private final TreeSet<LogEntry> log;

    
    
    //Security function variables (static members reset upon model restore)
    private Integer user_requests;
    private static Long last_user_request;
    private final double TIME_DIFFERENCE_TOLERANCE = 15.0*1000000000; //time (in nanoseconds) that is valid to have USER_REQUEST_TOLERANCE requests within
    private final int USER_REQUEST_TOLERANCE = 10;
    private final long ADMIN_VERIFICATION_TOLERANCE = 30*1000000000; //time (in nanoseconds) that a verified admin can access admin functions without admin authentication
    private static Long last_admin_request; //time (in nanoseconds) of last admin authentication
    private final int MAX_CONSECUTIVE_FAILED_ATTEMPTS = 5; //Number of failed Admin or User Authenication attempts before program resets
    private Integer consecutive_failed_attempts;
    
    //CSV import variables
    private File previous_import_path; //DOESN'T IMPLEMENT SERIALIZABLE
    private int ID_COLUMN_NUMBER = 0; //Location of SJSU User ID column in excel sheet rows
    private int FIRST_NAME_COLUMN_NUMBER = 4; //Location of SJSU First Name column in excel sheet rows
    private int LAST_NAME_COLUMN_NUMBER = 2; //Location of SJSU Last Name column in excel sheet rows
    private int BEDSPACE_COLUMN_NUMBER = 17; //Location of SJSU Bedspace column in excel sheet rows
    private String BUILDING_ID_DELIMITER = "-";
    private boolean leadingZeroFix = true;
    private final int ID_CELL_LENGTH = 9; //Length of SJSU User ID to locate valid student ID entries
    private final int SAMPLE_COLUMNS = 30; //Number of columns to pull for user sample
    private final int SAMPLE_ROWS = 30; //Number of rows to pull for user sample
    private final int SKIP_ROW_COUNT = 14; //Number of cells to skip before pulling data for user sample
    private final HashSet<String> INVALID_ID_STRINGS; //Strings that indicate the current row is not a resident
    
    private final String[] eventHeaders = {"Select", "Event Date", "Event Name", "Attendees", "Waitlisted"};
    private final String[] buildingHeaders = {"Allowed?", "Building"};
    /**
     * 
     * @throws CEAuthenticationFailedException 
     */
    protected Model() throws CEAuthenticationFailedException
    {
        events = new TreeSet_TableModel_ComboBoxModel<>(eventHeaders);
        buildings = new TreeSet_TableModel_ComboBoxModel<>(buildingHeaders);
        residents = new HashMap<>();
        admins = new HashMap<>();
        users = new HashMap<>();
        log = new TreeSet<>();
        previous_import_path = null;
        user_requests = 0;
        last_user_request = System.nanoTime();
        last_admin_request = (long)-1;
        consecutive_failed_attempts = 0;
        INVALID_ID_STRINGS = new HashSet<>(Arrays.asList("CLOSED", "LAST NAME", "SJSU ID"));
        
        if(!powerUserCreationPopup(LogEntry.Level.Administrator)) //Adds an admin to ensure administrative functions are operational at run-time
            throw new CEAuthenticationFailedException("Database requires at least 1 Administrator to function");
    }
    
    /**
     * 
     */
    static
    {
        last_user_request = System.nanoTime();
        last_admin_request = (long) -1;
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
     * In future versions of this function, all records will be kept (conflicting 
     * Events will be merged and the most recently imported resident will be kept)
     * 
     * 
     * @param modelIn the decrypted model to be imported
     * @param importAdmins if true, import non-existing admins into this model
     * @param importEvents if true, import non-existing events into this model
     * @param importResidents if true, import all (replace existing) residents
     */
    
    protected synchronized void mergeDatabase(Model modelIn, boolean importAdmins, boolean importUsers, boolean importEvents, boolean importResidents)
    {
        if(modelIn == null)
            return;
        
        if(importAdmins)
        {
            //This ensures all current admins' pins are kept.
            HashMap<String, String> temp = (HashMap<String, String>) modelIn.admins.clone();
            temp.putAll(admins); //Add/override all old admins with current admins
            admins.putAll(temp); //Every Admin is overridden with identical data & all old admins are added
        }
        
        if(importEvents)
        {
            events.addAll(modelIn.events);
        }
        
        if(importResidents)
        {
            residents.putAll(modelIn.residents);
            buildings.addAll(modelIn.buildings);
        }
        
        for(LogEntry l : modelIn.log)
        {
            l.archived();
            log.add(l);
        }
        
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
        if(residents.containsKey(id))
            return true;
        return false;
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
    protected Building extractBuilding(String ID)
    {
        Resident res = residents.get(ID);
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
        if(residents != null)
            return residents.size();
        return 0;
    }
    
    /**
     * Returns true if event with same name and date/time exists
     * @param event the event to check against
     * @return true if given event exists
     */
    
    protected synchronized boolean ContainsEvent(Event event)
    {
        if(events.size()>0)
            return events.contains(event);
        return false;
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
    
    protected void emptyEventDatabase() {
        if(authenticationPopup(LogEntry.Level.Administrator, "Empty Event Database"))
        {
            if(JOptionPane.showConfirmDialog(null, "DELETE ALL EVENTS IN DATABASE? \nTHIS CANNOT BE UNDONE", "Delete Events", JOptionPane.OK_CANCEL_OPTION)== JOptionPane.OK_OPTION){
                synchronized(events)  {   events.clear();  }
                JOptionPane.showMessageDialog(null, "All Events Deleted", "Delete Events", JOptionPane.INFORMATION_MESSAGE);
            }
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
        removeEvent(event);
        event.setName(name);
        event.setDateTime(dateTime);
        event.setMaxParticipants(maxAttendees);
        event.updateBuildings(allowedBuildings);
        return addEvent(event);
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
        return events.size();
    }
    
    protected int buildingCount()
    {
        return buildings.getSize();
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
     * Early arbitrary function implementation.
     * Returns an ArrayList of all Events in the specified range (inclusive)
     *
     * @param fromDate the beginning date to search from (inclusive)
     * @param toDate the end date to search to (exclusive)
     * @return an ArrayList of Events 
     */
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
     */
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
    
    protected int userCount()
    {
        synchronized(users){
            if(users != null)
                return users.size();
            return 0;
        }
    }
        
    //ADMIN-FUNCTIONS 
    
    protected boolean powerUserRemovalPopup(LogEntry.Level level)
    {
        boolean isAdminRemoval = level.equals(LogEntry.Level.Administrator);
                
        final JTextField ID_TextField = new JTextField();
        final JCheckBox confirmRemoval = new JCheckBox("Permantly Remove " + level.toString() + "?");
        
        String title = level.toString() + " Removal";
        
        Object[] message = {"Enter " + level.toString() + " to be removed", ID_TextField, confirmRemoval};
        Object[] options = {"OK", "Cancel"};
        
        JOptionPane pane = new JOptionPane(message, JOptionPane.INFORMATION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null, options, ID_TextField);
        JDialog diag = pane.createDialog(title);
        
        diag.setVisible(true);
        
        String ID = ID_TextField.getText();
        
        if(pane.getValue() == null || pane.getValue().equals(options[1]))
        {
            return false;
        }
        else if(!confirmRemoval.isSelected())
        {
            JOptionPane.showMessageDialog(null, "Error: Please confirm " + level.toString() + " removal", level.toString() + " Removal Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        else if(isAdminRemoval && admins.size() < 2)
        {
            JOptionPane.showMessageDialog(null, "Error: Database must have at least 1 Administrator", level.toString() + " Removal Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        else if(!authenticationPopup(LogEntry.Level.Administrator, "Remove " + level.toString() + " ID: " + ID))
        {
            return false;
        }
        else if((isAdminRemoval && (admins.remove(ID) == null)) || (!isAdminRemoval && (users.remove(ID) == null)))
        {
            JOptionPane.showMessageDialog(null, "Error: " + level.toString() + " not found", level.toString() + " Removal Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        JOptionPane.showMessageDialog(null, level.toString() + " Removed Successfully", level.toString() + " Removed", JOptionPane.INFORMATION_MESSAGE);
        return true;        
    }
    
    protected boolean powerUserCreationPopup(LogEntry.Level level)
    {
        final JTextField ID_TextField_1 = new JTextField();
        final JTextField ID_TextField_2 = new JTextField();
        final JPasswordField Pin_PassField_1 = new JPasswordField();
        final JPasswordField Pin_PassField_2 = new JPasswordField();
        
        String title = level.toString() + " Creation";
        
        Object[] ID1_Line = {"Scan ID: ", ID_TextField_1};
        Object[] ID2_Line = {"Rescan ID: ", ID_TextField_2};
        Object[] Pin1_Line = {"Create Pin: ", Pin_PassField_1};
        Object[] Pin2_Line = {"(4 digits or more)"};
        Object[] Pin3_Line = {"Retype Pin: ", Pin_PassField_2};
        
        Object[] message = {ID1_Line, ID2_Line, Pin1_Line, Pin2_Line, Pin3_Line};
        Object[] options = {"OK", "Cancel"};

        JOptionPane pane = new JOptionPane(message, JOptionPane.INFORMATION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null, options, ID1_Line);
        JDialog diag = pane.createDialog(title);
        
        ID_TextField_1.addActionListener(ViewerController.switchJTextComponent(ID_TextField_1, ID_TextField_2, false));
        ID_TextField_2.addActionListener(ViewerController.switchJTextComponent(ID_TextField_2, Pin_PassField_1, false));
        Pin_PassField_1.addActionListener(ViewerController.switchJTextComponent(Pin_PassField_1, Pin_PassField_2, false));
        Pin_PassField_2.addActionListener(ViewerController.disposeDialogActionListener(diag));
        
        diag.setVisible(true);
        
        String ID1 = ID_TextField_1.getText();
        String ID2 = ID_TextField_2.getText();
        String Pass1 = SealObject.encryptPass(new String(Pin_PassField_1.getPassword()));
        String Pass2 = SealObject.encryptPass(new String(Pin_PassField_2.getPassword()));
        
        if(pane.getValue() == null || pane.getValue().equals(options[1]))
        {
            return false;
        }
        else if(ID1 == null || ID1.length() <= 0 || Pass1.length() < 4 || !ID1.equals(ID2) || !Pass1.equals(Pass2))
        {
            JOptionPane.showMessageDialog(null, "Error: Invalid Pin or ID", "Creation Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        else if(admins.size() > 0 && !authenticationPopup(LogEntry.Level.Administrator, "Create new " + level.toString()))
        {
            return false;
        }
        else if(level.equals(LogEntry.Level.Administrator))
        {
            admins.put(ID1, Pass1);
            ViewerController.saveModel();
            return true;
        }
        else
        {
            users.put(ID1, Pass1);
            ViewerController.saveModel();
            return true;
        }
       
    }
    
    protected boolean authenticationPopup(LogEntry.Level level, String descp)
    {
        boolean isAdminAuth = level.equals(LogEntry.Level.Administrator);
        
        final JTextField ID_Textfield = new JTextField();
        final JPasswordField Pin_Passfield = new JPasswordField();
        
        String title = level.toString() + " Authentication";
        
        Object[] ID_Line = {"Enter ID: ", ID_Textfield};
        Object[] Pin_Line = {"Enter Pin: ", Pin_Passfield};
        
        Object[] message = {ID_Line, Pin_Line};
        Object[] options = {"OK", "Cancel"};

        JOptionPane pane = new JOptionPane(message, JOptionPane.INFORMATION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null, options, ID_Textfield);
        JDialog diag = pane.createDialog(title);

        ID_Textfield.addActionListener(ViewerController.switchJTextComponent(ID_Textfield, Pin_Passfield, false));
        Pin_Passfield.addActionListener(ViewerController.disposeDialogActionListener(diag));
        
        diag.setVisible(true);
        
        String ID = ID_Textfield.getText();
        char[] Pin = Pin_Passfield.getPassword();

        if(pane.getValue() == null || pane.getValue().equals(options[1]))
        {
            //Canceled
            String logID = (ID == null)? "Null" : ID;
            synchronized(log) {log.add(new LogEntry(logID, descp, LogEntry.Result.Failure, level));}
            return false;
        }
        else if(ID == null || Pin == null)
        {
            String logID = (ID == null)? "Null" : ID;
            synchronized(log) {log.add(new LogEntry(logID, descp, LogEntry.Result.Failure, level));}
            JOptionPane.showMessageDialog(null, "Error: Invalid Pin or ID", "Authentication Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        else if(admins.containsKey(ID) && SealObject.passCheck(new String(Pin), admins.get(ID))) //Admin can access both user & admin functions
        {
            synchronized(log) {log.add(new LogEntry(ID, descp, LogEntry.Result.Success, level));}
            synchronized(last_admin_request) {  last_admin_request = System.nanoTime(); }
            synchronized(consecutive_failed_attempts) { consecutive_failed_attempts = 0;} 
            return true;
        }
        
        else if(!isAdminAuth && users.containsKey(ID) && SealObject.passCheck(new String(Pin), users.get(ID))) //Users can only access user functions
        {
            synchronized(log) {log.add(new LogEntry(ID, descp, LogEntry.Result.Success, level));}
            synchronized(last_user_request) {  last_user_request = System.nanoTime(); }
            synchronized(consecutive_failed_attempts) { consecutive_failed_attempts = 0;    }
            return true;
        }
        
        JOptionPane.showMessageDialog(null, "Error: Invalid Pin or ID", "Authentication Error", JOptionPane.ERROR_MESSAGE);
        
        synchronized(log) {log.add(new LogEntry(ID, descp, LogEntry.Result.Failure, level));}
        synchronized(consecutive_failed_attempts) { consecutive_failed_attempts++;  }
        
        if (consecutive_failed_attempts >= MAX_CONSECUTIVE_FAILED_ATTEMPTS) {
            synchronized(log) {log.add(new LogEntry("SYSTEM", "SYSTEM SHUTDOWN RESULTED FROM: " + descp, LogEntry.Result.Failure, level));};
            synchronized(consecutive_failed_attempts) { consecutive_failed_attempts = 0;    } //Since the user will be forced to enter database password when restarting program
            System.exit(0);
        }
        
        return false;
    }
        
    /**
     * Used to standardize retrieving total number of admins in database
     * across different model implementations
     * 
     * @return the total number of separate admins registered
     */
    
    protected int adminCount()
    {
        synchronized(admins){
            if(admins != null)
                return admins.size();
            return 0;
        }
    }
    
    //Resident Functions
    
    protected boolean addResident(String ID, String lastName, String firstName, String bedSpace)
    {
        if(authenticationPopup(LogEntry.Level.Administrator, "Add Resident: " + ID))
        {
            synchronized(residents) {   residents.put(ID, new Resident(ID, firstName, lastName, bedSpace));    }
            return true;
        }
        return false;
    }
       
    
    protected String[] getNameBedspace(String ID)
    {
        synchronized(residents){
            if(residents.containsKey(ID))
            {
                Resident temp = residents.get(ID);
                String[] ret = {temp.getLast_name(), temp.getFirst_name(), temp.getBedspace()};
                return ret;
            }
            return null;
        }
    }
    
    protected void emptyResidentDatabase()
    {
        if(authenticationPopup(LogEntry.Level.Administrator, "Empty Resident Database"))
        {
            if(JOptionPane.showConfirmDialog(null, "DELETE ALL RESIDENTS IN DATABASE? \nTHIS CANNOT BE UNDONE", "Delete Residents", JOptionPane.OK_CANCEL_OPTION)== JOptionPane.OK_OPTION){
                synchronized(residents)  {   residents.clear();  }
                synchronized(buildings) { buildings.clear(); }
                ViewerController.showTimedInfoDialog("Deletion Successful", "Database Deletion Completed", 2);
            }
            else
                ViewerController.showTimedInfoDialog("Deletion Cancelled", "Database Deletion Cancelled", 2);
        }
    }
    
    protected void csvImport()
    {
        Scanner scanner = null;
        
        try {
            JFileChooser fileChooserGUI = new JFileChooser(); //File Chooser window for user
            String[] acceptedFileTypes = {"csv"}; //restricts files to .csv files
            fileChooserGUI.setFileFilter(new FileNameExtensionFilter(null, acceptedFileTypes)); //restrict file types
            
            JTable selectorTable; //Table used to display sample of rows/columns
            Object[][] tableData = new Object[SAMPLE_ROWS][]; //populated with non-empty rows/columns of sample sets
            Object[] tableHeaders; //Column headers (numbers 1 to # of columns)
           
            if(previous_import_path != null && previous_import_path.exists()) //set default fileChooser location to previous location if valid
                fileChooserGUI.setCurrentDirectory(previous_import_path);
            
            if(fileChooserGUI.showOpenDialog(null) != JFileChooser.APPROVE_OPTION //Check for invalid file
                    || fileChooserGUI.getSelectedFile() == null 
                    || !fileChooserGUI.getSelectedFile().exists())
            { return; }
            

            previous_import_path = fileChooserGUI.getSelectedFile(); //update previous selection location
            
            //Get scanner instance
            scanner = new Scanner(fileChooserGUI.getSelectedFile());

            //Set the delimiter used in file
            //scanner.useDelimiter(",");
            for(int i = 0; (scanner.hasNextLine() && i<SAMPLE_ROWS); i++) //populates row & column sets
            {
                tableData[i] = scanner.nextLine().split(",");
            }
            
            if(tableData.length < 4)
            {
                //error message
                return;
            }
            
            int numOfColumns = tableData[0].length;
            
            tableHeaders = new Object[numOfColumns]; //HAVE TO ASSUME ALL LINES ARE SAME LENGTH
            
            for(int i = 0; i<tableHeaders.length; i++) //create table headers
                tableHeaders[i] = i+1;
            
            selectorTable = new JTable(tableData, tableHeaders) { 
                @Override
                public boolean isCellEditable(int i, int j)
                {
                    return false;
                }
            };
            
            selectorTable.setPreferredScrollableViewportSize(ViewerController.JTablePopUpSize); //MAGIC NUMBERS
            selectorTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            selectorTable.setFillsViewportHeight(true);
            
            int[] temp = ViewerController.jTableDialog(selectorTable, "Please select ONE cell that contains a user ID");
            if(temp == null || temp[1] > numOfColumns){
                ViewerController.showTimedInfoDialog("Action Cancelled", "Import Cancelled or Invalid Selection", 3);
                return;
            }
            ID_COLUMN_NUMBER = temp[1];
            
            temp = ViewerController.jTableDialog(selectorTable, "Please select ONE cell that contains a LAST name");
            if(temp == null || temp[1] > numOfColumns){
                ViewerController.showTimedInfoDialog("Action Cancelled", "Import Cancelled or Invalid Selection", 3);
                return;
            }
            LAST_NAME_COLUMN_NUMBER = temp[1];
            
            temp = ViewerController.jTableDialog(selectorTable, "Please select ONE cell that contains a FIRST name");
            if(temp == null || temp[1] > numOfColumns){
                ViewerController.showTimedInfoDialog("Action Cancelled", "Import Cancelled or Invalid Selection", 3);
                return;
            }
            FIRST_NAME_COLUMN_NUMBER = temp[1];
            
            temp = ViewerController.jTableDialog(selectorTable, "Please select ONE cell that contains a BedSpace (ex: CVA-000)");
            if(temp == null || temp[1] > numOfColumns){
                ViewerController.showTimedInfoDialog("Action Cancelled", "Import Cancelled or Invalid Selection", 3);
                return;
            }
            BEDSPACE_COLUMN_NUMBER = temp[1];
            
            scanner.close();
            
            scanner = new Scanner(fileChooserGUI.getSelectedFile());

            String[] splitLineStrings;
            String ID;
            
            while(scanner.hasNextLine())
            {
                splitLineStrings = scanner.nextLine().split(",");
                
                if(splitLineStrings.length >= numOfColumns && !INVALID_ID_STRINGS.contains(splitLineStrings[ID_COLUMN_NUMBER])) {
                    //Leading '0' fix for SJSU. To be converted to an option under new program settings window
                    while(leadingZeroFix && splitLineStrings[ID_COLUMN_NUMBER].length()<ID_CELL_LENGTH)
                        splitLineStrings[ID_COLUMN_NUMBER] = "0" + splitLineStrings[ID_COLUMN_NUMBER];
                    //Store ID string for readability
                    ID = splitLineStrings[ID_COLUMN_NUMBER];
                    //Create new resident 
                    Resident resident = new Resident(ID, splitLineStrings[FIRST_NAME_COLUMN_NUMBER], splitLineStrings[LAST_NAME_COLUMN_NUMBER], splitLineStrings[BEDSPACE_COLUMN_NUMBER]);
                    //Save new resident
                    synchronized(residents) {   residents.put(ID, resident);  }
                    //Update Buildings hashmap. Duplicates are automatically discarded.
                    extractBuilding(ID);
                }
            }
            
        }   
        catch (HeadlessException | FileNotFoundException e) 
        {
            JOptionPane.showMessageDialog(null, "An Error Occured while attempting to import."
                    + "\nPlease check that this is a valid Excel file."
                    + "\nIt may help to use 'Save As' and create a new excel file to attempt to import from", 
                    "Internal Error Occured", JOptionPane.ERROR_MESSAGE);
        }
        finally
        {
            if(scanner != null)
                scanner.close();
        }
    }

    protected String[][] getLogData() {
        //String[] ColumnHeaders = {"Date", "User", "Level", "Result", "Message"};
        String[][] logData = new String[log.size()][5];
        
        int i = 0;
        for(LogEntry l : log)
        {
            logData[i][0] = l.getTime().getTime().toString();
            logData[i][1] = l.getID();
            logData[i][2] = l.getLevel().toString();
            logData[i][3] = l.getResult().toString();
            logData[i][4] = l.getMessage();
            i++;
        }
        
        return logData;
    }
    
    public final class Event implements Comparable<Event>, Serializable, TreeSet_TableModel_ComboBoxModel.TableModel_ComboModel_Interface{
        private static final long serialVersionUID = 1L;

        private String name;
        private Calendar date_time; 
        private HashMap<String, GregorianCalendar> attendees; //Think about sorting by time (gregCal) of checkin to eliminate waitingList. <-- Bad idea. Will most likely be O(n) every time
        private HashMap<String, GregorianCalendar> waitinglist; //Change to hashmap. Better to have O(1) lookup & add with O(n) sort than O(n) add and O(1) sort
        private TreeMap<Building, Integer> eventBuildings;
        private boolean autoWaitlist; //Set if user wishes all future adds default to waiting list without asking
        private int max_participants;
        private HashMap<String, Integer> tickets;
        //private HashMap<String, HashMap<String, Integer>> ticketCatagories; //<Catagory Name, <ID, # of Tickets>>

        /**
         *
         * @param Name
         * @param DateTime
         * @param acceptedBuildings
         */
        public Event(String Name, Calendar DateTime, Building[] acceptedBuildings)
        {
            name = Name;
            date_time = DateTime;
            attendees = new HashMap<>(100);
            waitinglist = new HashMap<>();
            max_participants = -1;
            tickets = new HashMap<>();
            eventBuildings = new TreeMap<>();

            if(acceptedBuildings != null && acceptedBuildings.length>0)
                for(Building b : acceptedBuildings){
                    eventBuildings.put(b, 0);
                }
            else {
                for(Iterator<Building> itr = buildings.iterator(); itr.hasNext(); )
                {
                    eventBuildings.put(itr.next(), 0);
                }
            }
        };

        /**
         *
         * @param Name
         * @param Date_Time
         * @param Max_Participants
         * @param acceptedBuildings
         */
        public Event(String Name, GregorianCalendar Date_Time, int Max_Participants, Building[] acceptedBuildings)
        {
            name = Name;
            date_time = Date_Time;
            attendees = new HashMap<>(Max_Participants);
            waitinglist = new HashMap<>();
            max_participants = Max_Participants;
            tickets = new HashMap<>();
            eventBuildings = new TreeMap<>();
            
            if(acceptedBuildings != null && acceptedBuildings.length>0)
                for(Building b : acceptedBuildings){
                    eventBuildings.put(b, 0);
                }
            else {
                buildBuildings();
            }
        };
        
        private void buildBuildings()
        {
            if(eventBuildings == null)
                eventBuildings = new TreeMap<>();
            for(Iterator<Building> itr = buildings.iterator(); itr.hasNext(); )
            {
                eventBuildings.put(itr.next(), 0);
            }
        }
        
        public boolean isBuilding(Building building)
        {
            return eventBuildings.containsKey(building);
        }

        @Override
        public String toString()
        {
            return (date_time.get(Calendar.MONTH)+1)+"/"+date_time.get(Calendar.DAY_OF_MONTH)+"/"+date_time.get(Calendar.YEAR)+" "+name;
        }

        public boolean removeParticipant(String ID)
        {
            if(attendees.remove(ID)!=null || waitinglist.remove(ID) != null)
            {
                Building b = extractBuilding(ID);
                eventBuildings.put(b, eventBuildings.get(b)-1);
                return true;
            }
            return false;
        }

        public String getLongDate()
        {
            return(date_time.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.US)
                    + " " + date_time.get(Calendar.DAY_OF_MONTH) 
                    + ", " + date_time.get(Calendar.YEAR));
        }

        public String getShortDate()
        {
            return((date_time.get(Calendar.MONTH)+1)
                    + "/" + date_time.get(Calendar.DAY_OF_MONTH) 
                    + "/" + date_time.get(Calendar.YEAR));
        }

        public String getTime()
        {
            return ((date_time.get(Calendar.HOUR)==0)? 12 : date_time.get(Calendar.HOUR))
                    + ":" + ((date_time.get(Calendar.MINUTE)<10)? "0" : "") + date_time.get(Calendar.MINUTE)
                    + " " + date_time.getDisplayName(Calendar.AM_PM, Calendar.LONG, Locale.US);
        }

        public void addTickets(String ID, Integer tick)
        {
            if(tick<0){
                if(tickets.containsKey(ID) && tickets.get(ID)>=Math.abs(tick))
                    tickets.put(ID, tickets.get(ID)+tick);
            }
            else if(tickets.containsKey(ID))
                tickets.put(ID, tickets.get(ID)+tick);
            else
                tickets.put(ID, tick);
        }

        /**
         *
         * @param ID
         * @return
         */
        public boolean isAttendee(String ID)
        {
            return attendees.containsKey(ID);
        }

        public boolean isWaitlisted(String ID)
        {
            return waitinglist.containsKey(ID);
        }

        public String[] Get_Details()
        {
            String[] ret = {"",""};
            ret[0] = "Name: " + name + " \nDate: " 
                    + date_time.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.US) + ", " 
                    + getLongDate() + "\t" + getTime()
                    + " \nNumber of Attendees: " + attendees.size() + "\nWaiting List size: " 
                    + ((waitinglist!=null)? waitinglist.size():0)
                    + ((max_participants==-1)? "\nNo participant limit" : "\nMax Participants: " + max_participants);
            
            for(Building b : eventBuildings.keySet())
            {
                ret[1] += "\n" + b.toString() + ": " + eventBuildings.get(b);
            }
            
            //ret[1] += "\n*Residents of unlisted buildings cannot check in.\nGo to 'Edit Event' to change";

            return ret;
        }

        private boolean maxAttendee()
        {
            if(max_participants > -1)
                return attendees.size()>=max_participants;
            return false;
        }

        private boolean addAttendee(String ID)
        {
            attendees.put(ID, new GregorianCalendar());
            Building b = extractBuilding(ID);
            if(eventBuildings.containsKey(b))
                eventBuildings.put(b, eventBuildings.get(b)+1);
            else
                eventBuildings.put(b, 0);
            return true;
        }

        private boolean addWaitlist(String ID)
        {     
            waitinglist.put(ID, new GregorianCalendar());
            Building b = extractBuilding(ID);
            if(eventBuildings.containsKey(b))
                eventBuildings.put(b, eventBuildings.get(b)+1);
            else
                eventBuildings.put(b, 0);
            return true;
        }

        /**
         *
         * @return
         */
        public String getName() {
            return name;
        }

        /**
         *
         * @param name
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         *
         * @return
         */
        public Calendar getDateTime() {
            return date_time;
        }

        /**
         *
         * @param dateTime
         */
        public void setDateTime(GregorianCalendar dateTime) {
            this.date_time = dateTime;
        }

        /**
         *
         * @return
         */
        public HashMap<String, GregorianCalendar> getAttendees() {
            return attendees;
        }

        /*Security Risk? Needed?
         * public void setAttendees(HashSet<String> attendees) {
          //  this.attendees = attendees;
        }*/

        /**
         *
         * @return
         */
        public int getMaxParticipants() {
            return max_participants;
        }

        /**
         *
         * @param maxParticipants
         */
        public void setMaxParticipants(int maxParticipants) {
            this.max_participants = maxParticipants;
        }

        /**
         *
         * @return
         */
        public HashMap<String, GregorianCalendar> getWaitinglist() {
            return waitinglist;
        }

        /**
         *
         * @param autoWaitlist
         */
        public void setAutoWaitlist(boolean autoWaitlist)
        {
            this.autoWaitlist = autoWaitlist;
        }

        @Override
        public int compareTo(Event e) 
        {
            if(e.getDateTime().compareTo(date_time) == 0)
                return name.compareTo(e.getName());
            return e.getDateTime().compareTo(date_time);
        }

        /**
         *
         * @param g
         * @return
         */
        public int compareTo(GregorianCalendar g)
        {
            return date_time.compareTo(g);
        }

        /**
         * Searches and returns true if ID is either an attendee or waitlisted for 
         * this event
         * 
         * Search is O(1) (HashMap lookup)
         * 
         * @param id student ID to search for
         * @param results 
         */

        public boolean search(String id)
        {
            return attendees.containsKey(id) || waitinglist.containsKey(id);
        }
        
        public HashMap<String,Integer> getTickets()
        {
            return tickets;
        }

        public ArrayList<String> getTickets(boolean checkIn, boolean waitlist)
        {
            ArrayList<String> ret = new ArrayList<>();
            ArrayList<String> keys = new ArrayList<>(tickets.keySet());
            Iterator<String> itr = keys.iterator();
            while(itr.hasNext())
            {
                String temp = itr.next();
                for(int j = 0; j<tickets.get(temp); j++)
                    ret.add(temp);
            }
            if(checkIn)
                ret.addAll(attendees.keySet());
            if(waitlist)
                ret.addAll(waitinglist.keySet());
            return ret;
        }
        
         protected boolean ticketWindowPopup(String ID)
        {
            final JTextField ID_Textfield = new JTextField();
            final JTextField Increase_Field = new JTextField();
            Object selected;

            if(ID == null){
                Increase_Field.setEditable(false);
                ID_Textfield.addActionListener(verifyIDSwapEditable(ID_Textfield, Increase_Field));
                selected = ID_Textfield;
            }
            else if(attendees.containsKey(ID) || waitinglist.containsKey(ID)){
                ID_Textfield.setEditable(false);
                ID_Textfield.setText(ID);
                selected = Increase_Field;
            } else {
               JOptionPane.showMessageDialog(null, "Error: ID given did not attend this event.", "Add Tickets Error", JOptionPane.ERROR_MESSAGE); 
               return false;
            }

            Object[] message = {"Swipe ID: ", ID_Textfield, "Add tickets: ", Increase_Field};
            Object[] options = {"OK", "Cancel"};

            JOptionPane pane = new JOptionPane(message, JOptionPane.INFORMATION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null, options, selected);
            JDialog diag = pane.createDialog("Add Tickets");
            Increase_Field.addActionListener(ViewerController.disposeDialogActionListener(diag));
            diag.setVisible(true);

            if(Increase_Field.getText() == null || Increase_Field.getText().length() == 0 || pane.getValue() == null || pane.getValue().equals(options[1]))
                return false;

            try{
                Integer Increase_Int = Integer.parseInt(Increase_Field.getText());
                addTickets(ID_Textfield.getText(), Increase_Int);     
                return true;
            } catch (NumberFormatException | NullPointerException ex){
                JOptionPane.showMessageDialog(null, "Error: improper input for Add Tickets. \nTickets not added.", "Add Tickets Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
         
        protected JTable getAttendeesJTable()
        {
            return getEventJTable(attendees);
        }
        
        protected JTable getWaitlistJTable()
        {
            return getEventJTable(waitinglist);
        }
         
        /**
        * Formats an event's attendees list into an Object 2-dimensional
        * array for use in a JTable.
        * 
        * Assumed Columns: Check-in Time, Student ID, Last Name, 
        * First Name, Bedspace, Tickets
        * 
        * @param checkInData the event whose attendees list is to be formatted
        * @return a two-dimensional array formatted for display in a JTable
        */
       private JTable getEventJTable(HashMap<String, GregorianCalendar> checkInData)
       {
           String[] columnNames = {"Check-in", "ID", "Last Name", "First Name", "Bedspace", "Tickets"};
           
           Object[][] ret = new Object[checkInData.size()][6];
           int i = 0;
           for(String id : checkInData.keySet())
           {
               ret[i][0] = checkInData.get(id).getTime().toString();
               ret[i][1] = id;
               ret[i][2] = residents.get(id).getLast_name();
               ret[i][3] = residents.get(id).getFirst_name();
               ret[i][4] = residents.get(id).getBedspace();
               ret[i][5] = ((tickets.containsKey(id))? tickets.get(id) : 0);
               if((int)ret[i][5]<10 && (int)ret[i][5]>0)
                   ret[i][5] = "0" + ret[i][5];
               i++;
           }
           
           JTable table = new JTable(ret, columnNames){ 
                @Override 
                public boolean isCellEditable(int row, int column){  
                    return false;
                }
            };
           
           return table;
       }

        private ActionListener verifyIDSwapEditable(final JTextField ID_Textfield, final JTextField otherTextField)
        {
            return (new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    String ID = ViewerController.extractID(ID_Textfield.getText());
                    if(ID == null || !(attendees.containsKey(ID) || waitinglist.containsKey(ID))){
                        ID_Textfield.setText("");
                        //Display message
                        return;
                    }
                    ID_Textfield.setText(ViewerController.extractID(ID_Textfield.getText()));
                    ID_Textfield.setEditable(false);
                    otherTextField.setEditable(true);
                    otherTextField.grabFocus();
                }
            });
        }

        /**
         * 
         * @param ID Resident ID to check in
         * @param suppressCheckinPrompt if true, does not prompt to update check in
         *  time
         * @return true if the resident is successfully checked in / check in time
         *  is updated
         * @throws CEDuplicateAttendeeException if resident is already attending or
         *  waitlisted for this event
         * @throws CEMaximumAttendeesException if maximum attendees is reached
         */
        public boolean validAttendee(String ID, boolean suppressCheckinPrompt) throws CEDuplicateAttendeeException, CEMaximumAttendeesException, CENonResidentException, CENonResidentException.CEUnpermittedBuildingException
        {
            if(ID == null || !residents.containsKey(ID)){
                throw new CENonResidentException("Student ID not in Resident Database");//Change to JDialog?
            }
            
            GregorianCalendar checkinTime = null;
            int[] timeDifference;
            String message;

            if(isAttendee(ID)) //Is Attendee, O(1) lookup. Best-case scenario
            {            
                checkinTime = attendees.get(ID); //O(1) lookup
            }
            else if(isWaitlisted(ID)) //Is Waitlisted, O(1) lookup. Best-case scenario
            {
                checkinTime = waitinglist.get(ID); //O(1) lookup
            }
            else //Has not been checked in
            {
                if(eventBuildings.size() > 0 && !eventBuildings.containsKey(extractBuilding(ID)))
                {
                    throw new CENonResidentException.CEUnpermittedBuildingException("Resident's Building not allowed to check in");
                }
                else if(!maxAttendee()) //Event is NOT full. Check in as attendee
                {
                    addAttendee(ID);
                    return true;
                }
                else if(autoWaitlist) //Event IS full and automatic waitlist IS selected
                {
                    addWaitlist(ID);
                    return true;
                }
                else //Event IS full and automatic waitlist is NOT selected
                {
                    if(maxAttendeeHandler())
                        return validAttendee(ID, suppressCheckinPrompt);
                    else
                        return false;
                }
            }

            if(suppressCheckinPrompt) 
            {            
                throw new CEDuplicateAttendeeException("Resident is already attending this event or is on the waitlist");
            }

            timeDifference = ViewerController.getTimeDifference(checkinTime, new GregorianCalendar());

            message = "Resident already swiped in\n" 
                       + timeDifference[0] + " Days " + timeDifference[1] + " Hours " 
                       + timeDifference[2] + " Minutes " + timeDifference[3] + " Seconds ago\nRecheck in? "
                       + "(Previous time is discarded. This does not affect number of tickets)";

               if(JOptionPane.showConfirmDialog(null, message, "Duplicate Resident", JOptionPane.OK_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE) != JOptionPane.OK_OPTION)
                   throw new CEDuplicateAttendeeException("Resident is already attending this event or is on the waitlist");
               else if(isAttendee(ID)){
                   addAttendee(ID);
                   return true;
               }
               else{
                   addWaitlist(ID);
                   return true;
               }
        }

        /** 
         * Event's implementation to handle an event that has reached its maximum 
         * number of attendees.
         * 
         * Allows a user to 
         * a) increase (or disable) the max attendee limit 
         * b) start a waiting list for this event, 
         * c) Do nothing. If a resident sign-in triggered this function, 
         * the resident will not be added
         * 
         * @return true if waitlist or increased attendees, false if bad 
         * authenication or user decides not to increase/activate max/waitlist
         */
        private boolean maxAttendeeHandler() 
        {       
            JFormattedTextField increaseByField = new JFormattedTextField(NumberFormat.getIntegerInstance());

            String message = "This event has reached its maxiumum number of "
                    + "attendees. This value was set when the event was created, "
                    + "\nYou can: ";

            JRadioButton op1 = new JRadioButton("Increase the limit "
                    + "(current limit: " + getMaxParticipants() + "). "
                    + "A value of 0 will remove the limit. Increase limit by:");

            JRadioButton op2 = new JRadioButton("Start a waiting list. "
                    + "\nAll IDs swiped for this event after this point will go "
                    + "on a printable waiting list");

            JRadioButton op3 = new JRadioButton("Don't add anymore attendees, "
                    + "including this one.");

            ButtonGroup group = new ButtonGroup();
            group.add(op1);
            group.add(op2);
            group.add(op3);


            Object[] options = {message, op1, increaseByField, op2, op3};

            int temp = JOptionPane.showOptionDialog(null, options, "Max Attendees Reached", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE, null, null, null);
            boolean select = (temp != JOptionPane.CANCEL_OPTION && temp != JOptionPane.CLOSED_OPTION);

            if(select && op1.isSelected()){
                try{
                    increaseByField.commitEdit();
                    Integer inc = (Integer) increaseByField.getValue();

                    if(inc == null || inc < 0) {
                        JOptionPane.showMessageDialog(null, "Error: Invalid increase amount entered.", "Increase Error", JOptionPane.ERROR_MESSAGE);
                        return false;
                    }
                    synchronized(this) {
                        if(inc == 0)
                            setMaxParticipants(-1);
                        else
                            setMaxParticipants(getMaxParticipants()+inc);
                    }
                    return true;
                }
                catch(ParseException | NullPointerException ex){
                    JOptionPane.showMessageDialog(null, "Error: Invalid increase amount entered.", "Increase Error", JOptionPane.ERROR_MESSAGE);
                    return false;
                }        
            }

            else if(select && op2.isSelected()){
                synchronized(this) {
                    setAutoWaitlist(true);
                }
                return true;
            }

            return false;
        }

        @Override
        public Object getColumn(int i) {
            switch (i){
                case 1:
                    return getShortDate() + " " + getTime();
                case 2:
                    return getName();
                case 3:
                    return getAttendees().size();
                case 4:
                    return getWaitinglist().size();
                case 5:
                    return this;
                default:
                    return null;
            }
        }

        private void updateBuildings(Building[] allowedBuildings) {
            TreeMap<Building, Integer> newBuildings = new TreeMap<>();
            for(Building b : allowedBuildings)
                newBuildings.put(b, 0);
            
            Set<Building> bSet = eventBuildings.keySet();
            Iterator<Building> itr = bSet.iterator();
            
            Building b;
            Building restore;
            while(itr.hasNext())
            {
                b = itr.next();
                restore = new Building(b.name.replace("_REMOVED", ""));
                if(newBuildings.containsKey(restore))
                {
                    newBuildings.put(restore, eventBuildings.get(b));
                }
                else if(eventBuildings.get(b) > 0)
                {
                    newBuildings.put(new Building(b.name + "_REMOVED"), eventBuildings.get(b));
                }
            }
            
            eventBuildings = newBuildings;          
        }
    }
    
    public final class Building implements Comparable<Building>, Serializable, TreeSet_TableModel_ComboBoxModel.TableModel_ComboModel_Interface
    {
        private String name;
        
        public Building(String Name)
        {
            name = Name;
        }
        
        @Override
        public Object getColumn(int i) {
            if(i == 1)
                return this;
            return null;
        }
        
        @Override
        public String toString()
        {
            return name;
        }

        @Override
        public int compareTo(Building o) {
            return name.compareToIgnoreCase(o.name);
        }
        
        @Override
        public boolean equals(Object o)
        {
            if(o == null)
                return false;
            
            try {
                Building b = (Building) o;
                return name.equalsIgnoreCase(b.name);
            } catch (NullPointerException ex)
            {
                return false;
            }
        }
        
        @Override
        public int hashCode()
        {
            return name.hashCode();
        }
        
    }
}