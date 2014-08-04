package com.sjsurha.resident_identifier;

//Syncronized: events
//To Syncronize: admin (authenication while adding)?
//Additions: Server/Client across-internet functionality? (possible?)
//           Halls Program: Remove non-pertanant info
//                          Checkin by hall
//                          printout statistics by hall

//Password encrypting requires java 6 or later
import com.sjsurha.resident_identifier.Exceptions.CEAuthenticationFailedException;
import com.sjsurha.resident_identifier.Exceptions.CEDuplicateAttendeeException;
import com.sjsurha.resident_identifier.Exceptions.CEMaximumAttendeesException;
import com.sjsurha.resident_identifier.Exceptions.CENonResidentException;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Scanner;
import java.util.TreeSet;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Model class contains student and event information. 
 * 
 * @author John Miller
 * @version .7 - Development
 */

//Set-up function to store Model's individual private members in case model class changed after program is implemented.
//Handle an event that check in a student multiple times?? (future dev?) --Done
//Handle checking attendance across several events?? (future dev?) (Neccessary?)
//Handle checking in non-residents?
//Pull residence halls' names from bedspace
public final class Model implements Serializable{
    private static final long serialVersionUID = 2L;
    
    
    //Stored Data Members
    private final EventTreeSet_TableModel_ComboBoxModel events;
    private final TreeSet<LogEntry> log;
    private final HashMap<String, Resident> residents;
    private final HashSet<String> buildings; //Will hold the names of all builings
        //Considerations: Excel Import, Manual Creation, Database merge. Clear on residents clear.
    private final HashMap<String, String> admins;//Change to hashmap, values are encrypted versions of admin pins
    private final HashMap<String, String> users; //change to hashmap, values are encrypted versions of user pins
    private File previous_excel_path;
    //Stored, public program variables
        //Global Variables for ViewPanes
    protected final Dimension JTablePopUpSize = new Dimension(600, 400);
    
    
    //Security function variables (static members reset upon model restore)
    private Integer user_requests;
    private static Long last_user_request;
    private final double TIME_DIFFERENCE_TOLERANCE = 15.0*1000000000; //time (in nanoseconds) that is valid to have USER_REQUEST_TOLERANCE requests within
    private final int USER_REQUEST_TOLERANCE = 10;
    private final long ADMIN_VERIFICATION_TOLERANCE = 30*1000000000; //time (in nanoseconds) that a verified admin can access admin functions without admin authentication
    private static Long last_admin_request; //time (in nanoseconds) of last admin authentication
    private final int MAX_CONSECUTIVE_FAILED_ATTEMPTS = 5; //Number of failed Admin or User Authenication attempts before program resets
    private Integer consecutive_failed_attempts;
    
    //Excel import variables
    private int ID_COLUMN_NUMBER = 0; //Location of SJSU User ID column in excel sheet rows
    private int FIRST_NAME_COLUMN_NUMBER = 4; //Location of SJSU First Name column in excel sheet rows
    private int LAST_NAME_COLUMN_NUMBER = 2; //Location of SJSU Last Name column in excel sheet rows
    private int BEDSPACE_COLUMN_NUMBER = 17; //Location of SJSU Bedspace column in excel sheet rows
    private String BUILDING_ID_DELIMITER = "-";
    private final int ID_CELL_LENGTH = 9; //Length of SJSU User ID to locate valid student ID entries
    private final int SAMPLE_COLUMNS = 30; //Number of columns to pull for user sample
    private final int SAMPLE_ROWS = 30; //Number of rows to pull for user sample
    private final int SKIP_ROW_COUNT = 14; //Number of cells to skip before pulling data for user sample
    private final HashSet<String> INVALID_ID_STRINGS; //Strings that indicate the current row is not a resident
      
    /**
     * 
     * @throws CEAuthenticationFailedException 
     */
    protected Model() throws CEAuthenticationFailedException
    {
        events = new EventTreeSet_TableModel_ComboBoxModel();
        log = new TreeSet<>();
        previous_excel_path = null;
        residents = new HashMap<String, Resident>();
        buildings = new HashSet<>();
        user_requests = 0;
        last_user_request = System.nanoTime();
        last_admin_request = (long)-1;
        admins = new HashMap<>();
        users = new HashMap<>();
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
     * @return true if Admin/Residents changed as a result. True by default for 
     * events
     */
    
    protected synchronized boolean mergeDatabase(Model modelIn, boolean importAdmins, boolean importEvents, boolean importResidents)
    {
        if(modelIn == null)
            return false;
        boolean ret = true;
        
        if(importAdmins)
        {
            //This ensures all current admins' pins are kept.
            HashMap<String, String> temp = (HashMap<String, String>) modelIn.admins.clone();
            temp.putAll(admins); //Add/override all old admins with current admins
            admins.putAll(temp); //Every Admin is overridden with identical data & all old admins are added
        }
        
        if(importEvents)
        {
            ret = ret && events.addAll(modelIn.events);
        }
        
        if(importResidents)
        {
            residents.putAll(modelIn.residents);
        }
        
        ViewerController.saveModel();
        
        return ret;
    }

    //RESIDENCE DATABASE FUNCTION(S)
    
    /**
     * Checks if ID is in residents (student is a resident) and if resident has not yet attended this event.
     *
     * @param id Student ID to check for residency & event participation
     * @param event Event to check ID against
     * @param disableRecheckinPrompt if true, suppresses prompt to ask if user 
     *  wants to update check-in time for attendees
     * @return Returns True if Resident & has not yet attended event
     * @throws CEDuplicateAttendeeException Thrown if Resident has already attended event
     * @throws CENonResidentException Thrown if Student ID does not exist in Resident hashmap
     * @throws CEMaximumAttendeesException Thrown if Event has reached the maximum number of Residents that can attend an event
     */
    protected synchronized boolean addAttendee(String id, Event event, boolean disableRecheckinPrompt) throws CEDuplicateAttendeeException, CENonResidentException, CEMaximumAttendeesException
    {
        //RequestCheck();
        if(id == null || !residents.containsKey(id))
            throw new CENonResidentException("Student ID not in Resident Database");//Change to JDialog?
        return event.validAttendee(id, disableRecheckinPrompt);     
    }
    
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
    
    private synchronized boolean addEvent(Event event)
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
     * @return true if the adding of the modified event was successful, false 
     * if not successful
     */
    protected synchronized boolean updateEvent(Event event, String name, GregorianCalendar dateTime, int maxAttendees)
    {
        removeEvent(event);
        event.setName(name);
        event.setDateTime(dateTime);
        event.setMaxParticipants(maxAttendees);
        return addEvent(event);
    }
    
    /**
     * Creates and returns an event with no max attendee limit 
     * if an event with that name and date/time did not already exist and was 
     * successfully added to the set
     * 
     * Returns null if the set did not change as a result of this call
     * 
     * @param Name Name of the event
     * @param DateTime Date/Time container for event
     * @return the created event if it was added successfully, null if not added
     */
    protected synchronized Event createEvent(String Name, GregorianCalendar DateTime)
    {
        Event event = new Event(Name, DateTime);
        if(addEvent(event))
            return event;
        return null;
    }
    
    /**
     * Creates and returns an event if an event with that name and date/time
     * did not already exist and was successfully added to the set
     * 
     * Returns null if the set did not change as a result of this call
     * 
     * @param Name name of the event
     * @param DateTime Date/Time container of the event
     * @param MaxAttendees maximum  number of attendees that can attend this
     * event
     * @return the created event if successful, null if unsuccessful
     */
    protected synchronized Event createEvent(String Name, GregorianCalendar DateTime, int MaxAttendees)
    {
        Event event = new Event(Name, DateTime, MaxAttendees);
        if(addEvent(event))
            return event;
        return null;
            
    }
    
    /**
     * Constructs a GregorianCalendar and sends it to a synchronized overloaded 
     * version of this function.
     * 
     * Creates and returns an Event with no maxAttendee limit if the set does
     * not already contain an Event with the same Name and date/time. 
     * 
     * Returns null if set did not change as a result of this call
     * 
     * @param Name Name of event
     * @param year Year in which event takes place
     * @param month Month in which event takes place
     * @param day day that event takes place
     * @param hour hour that event takes place
     * @param minute minute that event takes place
     * @return the created event if successfully added, null if not
     */
    protected Event createEvent(String Name, int year, int month, int day, int hour, int minute)
    {
        return createEvent(Name, new GregorianCalendar(year, month, day, hour, minute));
    }
    
    /**
     * Constructs a GregorianCalendar object and sends it to a synchronized
     * overloaded version of this function
     * 
     * Creates and returns an Event with MaxAttendees limit if the set does
     * not already contain an Event with the same Name and date/time. 
     * 
     * Returns null if set did not change as a result of this call
     * 
     * @param Name Name of event
     * @param year Year in which event takes place
     * @param month Month in which event takes place
     * @param day day that event takes place
     * @param hour hour that event takes place
     * @param minute minute that event takes place
     * @param MaxAttendees limit on number of check-ins
     * @return the created event if successfully added, null if not
     */
    protected Event createEvent(String Name, int year, int month, int day, int hour, int minute, int MaxAttendees)
    {
        return createEvent(Name, new GregorianCalendar(year, month, day, hour, minute), MaxAttendees);
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
    
    /**
     * Ensures clean program exit by discarding all listener threads
     * in the event collection
     */
    protected void removeAllEventListeners()
    {
        events.removeAllListeners();
    }
        
    /**
     * This functionality will soon be moved to the Event class
     * USES MODEL'S RESIDENTS TO GET RESIDENT INFO
     * Formats an event's attendees list into an Object 2-dimensional
     * array for use in a JTable.
     * 
     * Assumed Columns: Check-in Time, Student ID, Last Name, 
     * First Name, Bedspace, Tickets
     * 
     * @param checkInData the event whose attendees list is to be formatted
     * @return a two-dimensional array formatted for display in a JTable
     */
    protected Object[][] getEventJTable(HashMap<String, GregorianCalendar> checkInData, HashMap<String, Integer> tickets)
    {
        //String[] columnNames = {"Check-in", "ID", "Last Name", "First Name", "Bedspace", "Tickets"};
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
        return ret;
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
    
    /*protected HashSet getBuildings()
    {
        
    }*/
    
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
           
            if(previous_excel_path != null && previous_excel_path.exists()) //set default fileChooser location to previous location if valid
                fileChooserGUI.setCurrentDirectory(previous_excel_path);
            
            if(fileChooserGUI.showOpenDialog(null) != JFileChooser.APPROVE_OPTION //Check for invalid file
                    || fileChooserGUI.getSelectedFile() == null 
                    || !fileChooserGUI.getSelectedFile().exists())
            { return; }
            

            previous_excel_path = fileChooserGUI.getSelectedFile(); //update previous selection location
            
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
            
            selectorTable.setPreferredScrollableViewportSize(JTablePopUpSize); //MAGIC NUMBERS
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
            String bedspaceString;
            
            while(scanner.hasNextLine())
            {
                splitLineStrings = scanner.nextLine().split(",");
                
                if(splitLineStrings.length >= numOfColumns && !INVALID_ID_STRINGS.contains(splitLineStrings[ID_COLUMN_NUMBER])) {
                    //Leading '0' fix for SJSU
                    while(splitLineStrings[ID_COLUMN_NUMBER].length()<ID_CELL_LENGTH)
                        splitLineStrings[ID_COLUMN_NUMBER] = "0" + splitLineStrings[ID_COLUMN_NUMBER];
                    //Store Bedspace string for readability
                    bedspaceString = splitLineStrings[BEDSPACE_COLUMN_NUMBER];
                    //Create new resident 
                    Resident resident = new Resident(splitLineStrings[ID_COLUMN_NUMBER], splitLineStrings[FIRST_NAME_COLUMN_NUMBER], splitLineStrings[LAST_NAME_COLUMN_NUMBER], bedspaceString);
                    //Save new resident
                    synchronized(residents) {   residents.put(splitLineStrings[ID_COLUMN_NUMBER], resident);  }
                    //Update Buildings hashmap. Duplicates are automatically discarded.
                    int delmiterIndex = (bedspaceString.contains(BUILDING_ID_DELIMITER))? bedspaceString.indexOf(BUILDING_ID_DELIMITER) : bedspaceString.length();
                    buildings.add(bedspaceString.toUpperCase().substring(0, delmiterIndex));
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
}


//Old Import function that used excel files

/*
     * Utilizes poi-3.9 java project to import excel files
     * NOTE: LICENCSE FILE ADDITION
     * 
     * Self-contained function to support importing resident information into database
     * 
     * Currently, this information is: Student ID, Last Name, First Name, and 
     * Bedspace. 
     * 
     * Function evolves with resident class and may soon include resident
     * contact information
     * 
     */
    /*
    protected void excelImport()
    {
        try {
            JFileChooser fileChooserGUI = new JFileChooser(); //File Chooser window for user
            String[] acceptedFileTypes = {"xls", "xlsx", "xlsm"}; //restricts files to excel files
            fileChooserGUI.setFileFilter(new FileNameExtensionFilter(null, acceptedFileTypes)); //restrict file types
            
            TreeSet<Integer> usedRowsSet = new TreeSet<>(); //set to keep track of non-empty rows for sample
            TreeSet<Integer> usedColumnsSet = new TreeSet<>();  //set to keep track of non-empty columns for sample
            Integer[] usedRowsArr; //once usedRowSet is populated, contents are tranferred to this array
            Integer[] usedColumnsArr; //once usedColumnSet is populated, contents are tranferred to this array
            
            JTable selectorTable; //Table used to display sample of rows/columns
            Object[][] tableData; //populated with non-empty rows/columns of sample sets
            Object[] tableHeaders; //Column headers (numbers 1 to # of columns)
           
                                    

            
            if(previous_excel_path != null && previous_excel_path.exists()) //set default fileChooser location to previous location if valid
                fileChooserGUI.setCurrentDirectory(previous_excel_path);
            
            if(fileChooserGUI.showOpenDialog(null) != JFileChooser.APPROVE_OPTION //Check for invalid file
                    || fileChooserGUI.getSelectedFile() == null 
                    || !fileChooserGUI.getSelectedFile().exists())
            { return; }
            

            previous_excel_path = fileChooserGUI.getSelectedFile(); //update previous selection location
            
            XSSFSheet sheet = new XSSFWorkbook(new FileInputStream(fileChooserGUI.getSelectedFile())).getSheetAt(0); //Get first sheet from the workbook

            for(int i = SKIP_ROW_COUNT; (i<=sheet.getLastRowNum() && i<(SKIP_ROW_COUNT+SAMPLE_ROWS)); i++) //populates row & column sets
            {
                Row currentRow = sheet.getRow(i);
                for(int j = 0; j<SAMPLE_COLUMNS; j++){
                    Cell currentCell = ((currentRow != null)? currentRow.getCell(j) : null);
                    if(currentCell != null){
                        switch (currentCell.getCellType()) //Check for numeric or string type cells
                        {
                            case Cell.CELL_TYPE_NUMERIC:
                                usedRowsSet.add(i);
                                usedColumnsSet.add(j);
                                break;
                            case Cell.CELL_TYPE_STRING:
                                if(!currentCell.getStringCellValue().isEmpty()){ //Makes sure empty cells are not added
                                    usedRowsSet.add(i);
                                    usedColumnsSet.add(j);
                                    
                                }
                                break;
                        }
                    }
                }
            }
            
            tableData = new Object[usedRowsSet.size()][usedColumnsSet.size()]; //Prepare the table data
            tableHeaders = new Object[usedColumnsSet.size()]; //prepare column data
                                    
            usedRowsArr = usedRowsSet.toArray(new Integer[usedRowsSet.size()]); //populate row array
            usedColumnsArr = usedColumnsSet.toArray(new Integer[usedColumnsSet.size()]); //populate column array

            for(int i = 0; i<usedRowsSet.size(); i++) //populates tableData with information from cells
            {
                Row currentRow = sheet.getRow(usedRowsArr[i]);
                for(int j = 0; j<usedColumnsSet.size(); j++){
                    Cell currentCell = ((currentRow != null)? currentRow.getCell(usedColumnsArr[j]) : null);
                    if(currentCell != null){
                        switch (currentCell.getCellType()) 
                        {
                            case Cell.CELL_TYPE_NUMERIC:
                                tableData[i][j] = currentCell.getNumericCellValue();
                                break;
                            case Cell.CELL_TYPE_STRING:
                                tableData[i][j] = currentCell.getStringCellValue();
                                break;
                        }
                    }
                }
            }

            for(int i = 0; i<usedColumnsSet.size(); i++) //create table headers
                tableHeaders[i] = i+1;
            
            //create uneditable table
            selectorTable = new JTable(tableData, tableHeaders) { 
                @Override
                public boolean isCellEditable(int i, int j)
                {
                    return false;
                }
            };

            selectorTable.setPreferredScrollableViewportSize(JTablePopUpSize); //MAGIC NUMBERS
            selectorTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            selectorTable.setFillsViewportHeight(true);
            
            int[] temp = ViewerController.jTableDialog(selectorTable, "Please select ONE cell that contains a user ID");
            if(temp == null || temp[1] > usedColumnsArr.length){
                ViewerController.showTimedInfoDialog("Action Cancelled", "Import Cancelled or Invalid Selection", 3);
                return;
            }
            ID_COLUMN_NUMBER = usedColumnsArr[temp[1]];
            
            temp = ViewerController.jTableDialog(selectorTable, "Please select ONE cell that contains a LAST name");
            if(temp == null || temp[1] > usedColumnsArr.length){
                ViewerController.showTimedInfoDialog("Action Cancelled", "Import Cancelled or Invalid Selection", 3);
                return;
            }
            LAST_NAME_COLUMN_NUMBER = usedColumnsArr[temp[1]];
            
            temp = ViewerController.jTableDialog(selectorTable, "Please select ONE cell that contains a FIRST name");
            if(temp == null || temp[1] > usedColumnsArr.length){
                ViewerController.showTimedInfoDialog("Action Cancelled", "Import Cancelled or Invalid Selection", 3);
                return;
            }
            FIRST_NAME_COLUMN_NUMBER = usedColumnsArr[temp[1]];
            
            temp = ViewerController.jTableDialog(selectorTable, "Please select ONE cell that contains a BedSpace (ex: CVA-000)");
            if(temp == null || temp[1] > usedColumnsArr.length){
                ViewerController.showTimedInfoDialog("Action Cancelled", "Import Cancelled or Invalid Selection", 3);
                return;
            }
            BEDSPACE_COLUMN_NUMBER = usedColumnsArr[temp[1]];

            for(Iterator<Row> rowIterator = sheet.iterator(); rowIterator.hasNext(); ) 
            {
                String IDString;
                String firstNameString;
                String lastNameString;
                String bedSpace; 
                
                Row currentRow = rowIterator.next();           
                Cell IDCell = currentRow.getCell(ID_COLUMN_NUMBER);

                IDString = (IDCell != null)? IDCell.getStringCellValue() : null;

                if(IDString != null && IDString.length()==ID_CELL_LENGTH && (Arrays.binarySearch(INVALID_ID_STRINGS, IDString)<0))
                {
                    try{
                        IDString = currentRow.getCell(ID_COLUMN_NUMBER).getStringCellValue();
                        firstNameString = currentRow.getCell(FIRST_NAME_COLUMN_NUMBER).getStringCellValue();
                        lastNameString = currentRow.getCell(LAST_NAME_COLUMN_NUMBER).getStringCellValue();                        
                        bedSpace = currentRow.getCell(BEDSPACE_COLUMN_NUMBER).getStringCellValue();
                        Resident resident = new Resident(IDString, firstNameString, lastNameString, bedSpace);
                        synchronized(residents) {   residents.put(IDString, resident);  }
                    } catch(NullPointerException ex){}
                }
            }
        }   
        catch (HeadlessException | IOException | NoSuchElementException e) 
        {
            JOptionPane.showMessageDialog(null, "An Error Occured while attempting to import."
                    + "\nPlease check that this is a valid Excel file."
                    + "\nIt may help to use 'Save As' and create a new excel file to attempt to import from", 
                    "Internal Error Occured", JOptionPane.ERROR_MESSAGE);
        }
    }
    */