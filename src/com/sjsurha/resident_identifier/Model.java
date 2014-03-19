package com.sjsurha.resident_identifier;

//Syncronized: events
//To Syncronize: 
//Additions: Server/Client across-internet functionality? (possible?)
//           Halls Program: Remove non-pertanant info
//                          Checkin by hall
//                          printout statistics by hall

//Password encrypting requires java 6 or later
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Model class contains student and event information. 
 * 
 * @author John Miller
 * @version .7 - Development
 */

//Serialize (store data) when main thread closed
//Set-up function to store Model's individual private members in case model class changed after program is implemented.
//Handle an event that check in a student multiple times?? (future dev?)
//Handle checking attendance across several events?? (future dev?) (Neccessary?)
public final class Model implements Serializable{
    private static final long serialVersionUID = 2L;
    
    
    //Stored Data Members
    private final EventTreeSet_TableModel_ComboBoxModel events;
    private final HashMap<String, Resident> residents;
    private final HashSet<String> admins;
    private File previous_excel_path;
    
    //Security function variables (static members reset upon model restore)
    private Integer user_requests;
    private static Long last_user_request;
    private final double TIME_DIFFERENCE_TOLERANCE = 15.0*1000000000; //time (in nanoseconds) that is valid to have USER_REQUEST_TOLERANCE requests within
    private final int USER_REQUEST_TOLERANCE = 10;
    private final long ADMIN_VERIFICATION_TOLERANCE = 30*1000000000; //time (in nanoseconds) that a verified admin can access admin functions without admin authentication
    private static Long last_admin_request; //time (in nanoseconds) of last admin authentication
    private final int MAX_CONSECUTIVE_FAILED_ATTEMPTS = 5; //Number of failed Admin Authenication attempts before program resets
    private Integer consecutive_failed_attempts;
    
    //Excel import variables
    private int ID_COLUMN_NUMBER = 0; //Location of SJSU User ID column in excel sheet rows
    private int FIRST_NAME_COLUMN_NUMBER = 4; //Location of SJSU First Name column in excel sheet rows
    private int LAST_NAME_COLUMN_NUMBER = 2; //Location of SJSU Last Name column in excel sheet rows
    private int BEDSPACE_COLUMN_NUMBER = 17; //Location of SJSU Bedspace column in excel sheet rows
    private final int ID_CELL_LENGTH = 9; //Length of SJSU User ID to locate valid student ID entries
    private final int SAMPLE_COLUMNS = 30; //Number of columns to pull for user sample
    private final int SAMPLE_ROWS = 30; //Number of rows to pull for user sample
    private final int SKIP_ROW_COUNT = 14; //Number of cells to skip before pulling data for user sample
    private final String[] INVALID_ID_STRINGS = {"", "CLOSED"}; //Strings that indicate the current row is not a resident
      
    /**
     *
     */
    protected Model() throws CEAuthenticationFailedException
    {
        events = new EventTreeSet_TableModel_ComboBoxModel();
        previous_excel_path = null;
        residents = new HashMap<>();
        user_requests = 0;
        last_user_request = System.nanoTime();
        last_admin_request = (long)-1;
        admins = new HashSet<>();
        consecutive_failed_attempts = 0;
        if(!adminCreationPopup()) //Adds an admin to ensure administrative functions are operational at run-time
            throw new CEAuthenticationFailedException("Database requires at least 1 Administrator to function");
        importEvents(); //Temporary until 'Import Database' Function under Edit Database is fully functional
    }
    
    static
    {
        last_user_request = System.nanoTime();
        last_admin_request = (long) -1;
    }

    //USER FUNCTION(S) (admin-authentication not required at this level)
    
    /**
     * Checks if ID is in residents (student is a resident) and if resident has not yet attended this event.
     *
     * @param id Student ID to check for residency & event participation
     * @param event Event to check ID against
     * @return Returns True if Resident & has not yet attended event
     * @throws CEDuplicateAttendeeException Thrown if Resident has already attended event
     * @throws CENonResidentException Thrown if Student ID does not exist in Resident hashmap
     * @throws CEMaxiumAttendeesException Thrown if Event has reached the maximum number of Residents that can attend an event
     */
    protected synchronized boolean addAttendee(String id, Event event) throws CEDuplicateAttendeeException, CENonResidentException, CEMaxiumAttendeesException
    {
        //RequestCheck();
        if(id == null || !residents.containsKey(id))
            throw new CENonResidentException("Student ID not in Resident Database");//Change to JDialog
        try {
            return event.validAttendee(id);
        } catch (CEMaxiumAttendeesException ex) {
            if(maxAttendeeHandler(event))
                return event.validAttendee(id);
            else
                return false;           
        }        
    }
    
     /**
     *
     * @param id
     * @return
     */
    protected boolean checkResident(String id)
    {
        RequestCheck();
        if(residents.containsKey(id))
            return true;
        return false;
    }
    
    private void RequestCheck()
    {
        long diffTime = System.nanoTime()-last_user_request;
        if(diffTime<=TIME_DIFFERENCE_TOLERANCE)
        {
            if(user_requests<USER_REQUEST_TOLERANCE){
                synchronized(user_requests){  user_requests++; }
            }
            else
                throw new IllegalStateException("User has exceeded alotted number of requests within alotted time period.");
        } else {
            synchronized(last_user_request) { last_user_request = System.nanoTime(); }
            synchronized(user_requests){  user_requests = 0;  }
        }
    }
    
    protected int residentCount()
    {
        if(residents != null)
            return residents.size();
        return 0;
    }
    
    protected synchronized boolean ContainsEvent(Event event)
    {
        return events.contains(event);
    }
    
    protected synchronized void removeEvent(Event event)
    {
        events.remove(event);
    }
    
    private synchronized boolean addEvent(Event event)
    {
        return events.add(event);
    }
    
    protected void emptyEventDatabase() {
        if(Admin_Authentication_Popup())
        {
            if(JOptionPane.showConfirmDialog(null, "DELETE ALL EVENTS IN DATABASE? \nTHIS CANNOT BE UNDONE", "Delete Events", JOptionPane.OK_CANCEL_OPTION)== JOptionPane.OK_OPTION){
                synchronized(events)  {   events.clear();  }
                JOptionPane.showMessageDialog(null, "All Events Deleted", "Delete Events", JOptionPane.INFORMATION_MESSAGE);
            }
            else
                JOptionPane.showMessageDialog(null, "Database deletion cancelled", "Delete Events", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    /**
     * The model is required to implement this function due to the fact that
     * it changes data the event collection uses to sort events.
     * To ensure proper ordering, the event is removed, modified, and
     * reinserted as a new event
     * 
     * @param event the event to be removed
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
     *
     * @param Name
     * @param DateTime
     * @return
     */
    protected synchronized Event createEvent(String Name, GregorianCalendar DateTime)
    {
        Event event = new Event(Name, DateTime);
        if(addEvent(event))
            return event;
        return null;
    }
    
    /**
     *
     * @param Name
     * @param DateTime
     * @param MaxAttendees
     * @return
     */
    protected synchronized Event createEvent(String Name, GregorianCalendar DateTime, int MaxAttendees)
    {
        Event event = new Event(Name, DateTime, MaxAttendees);
        if(addEvent(event))
            return event;
        return null;
            
    }
    
    /**
     *
     * @param Name
     * @param year
     * @param month
     * @param day
     * @param hour
     * @param minute
     * @return
     */
    protected synchronized Event createEvent(String Name, int year, int month, int day, int hour, int minute)
    {
        Event event = new Event(Name, new GregorianCalendar(year, month, day, hour, minute));
        if(addEvent(event))
            return event;
        return null;
    }
    
    /**
     *
     * @param Name
     * @param year
     * @param month
     * @param day
     * @param hour
     * @param minute
     * @param MaxAttendees
     * @return
     */
    protected Event createEvent(String Name, int year, int month, int day, int hour, int minute, int MaxAttendees)
    {
        Event event = new Event(Name, new GregorianCalendar(year, month, day, hour, minute), MaxAttendees);
        if(addEvent(event))
            return event;
        return null;
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
    
    public JComboBox getEventsJComboBox()
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
     * Development function to assist in maintaining the live database
     * while project is still evolving. 
     * 
     * Used when model changes invalidate previous saved/encrypted model
     */
    private void importEvents()
    {
        try (ObjectInputStream input = new ObjectInputStream(new FileInputStream(new File("RecoveredEvents.obj")))) {
            TreeSet<Event> ImportedEvents = (TreeSet<Event>) input.readObject();
            events.addAll(ImportedEvents);
            JOptionPane.showMessageDialog(null, ImportedEvents.size() + " Events Imported successfully.", "Import complete", JOptionPane.INFORMATION_MESSAGE);
        }
        
        catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(Model.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(null, "Error: Import unsuccessful.", "Import error", JOptionPane.ERROR_MESSAGE);
        }
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
     * @param e the event whose attendees list is to be formatted
     * @return a two-dimensional array formatted for display in a JTable
     */
    protected Object[][] getEventAttendeeJTable(Event e)
    {
        //String[] columnNames = {"Check-in", "ID", "Last Name", "First Name", "Bedspace", "Tickets"};
        HashMap<String, GregorianCalendar> tempAttendees = e.getAttendees();
        Object[][] ret = new Object[tempAttendees.size()][6];
        int i = 0;
        for(String id : tempAttendees.keySet())
        {
            ret[i][0] = tempAttendees.get(id).getTime().toString();
            ret[i][1] = id;
            ret[i][2] = residents.get(id).getLast_name();
            ret[i][3] = residents.get(id).getFirst_name();
            ret[i][4] = residents.get(id).getBedspace();
            ret[i][5] = ((e.getTickets().containsKey(id))? e.getTickets().get(id) : 0);
            if((int)ret[i][5]<10 && (int)ret[i][5]>0)
                ret[i][5] = "0" + ret[i][5];
            i++;
        }
        return ret;
    }
    
    /**
     * This functionality will soon be moved to the Event class
     * USES MODEL'S RESIDENTS TO GET RESIDENT INFO
     * Formats an event's waitlist into an Object 2-dimensional
     * array for use in a JTable.
     * 
     * Assumed Columns: Check-in Time, Student ID, Last Name, 
     * First Name, Bedspace, Tickets
     * 
     * @param e the event whose waitlist is to be formatted
     * @return a two-dimensional array formatted for display in a JTable
     */
    protected Object[][] getEventWaitlistJTable(Event e)
    {
        //String[] columnNames = {"Check-in", "ID", "Last Name", "First Name", "Bedspace", "Tickets"};
        TreeMap<GregorianCalendar, String> tempWaitlist = e.getWaitinglist();
        Object[][] ret = new Object[tempWaitlist.size()][6];
        int i = 0;
        for(GregorianCalendar g : tempWaitlist.keySet())
        {
            String id = tempWaitlist.get(g);
            ret[i][0] = g.getTime().toString();
            ret[i][1] = id;
            ret[i][2] = residents.get(id).getLast_name();
            ret[i][3] = residents.get(id).getFirst_name();
            ret[i][4] = residents.get(id).getBedspace();
            ret[i][5] = ((e.getTickets().containsKey(id))? e.getTickets().get(id) : 0);
            if((int)ret[i][5]<10)
                ret[i][5] = "0" + ret[i][5];
            i++;
        }
        return ret;
    }
    
    /**
     * Early and arbitrary implementation.
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
     * Total number of unique resident (no duplicates)
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
            ret.add("The following statistics apply to events recorded from " + fromDate.getDisplayName(Calendar.MONTH,Calendar.LONG, Locale.ENGLISH)+" "+fromDate.getDisplayName(Calendar.DAY_OF_MONTH,Calendar.LONG, Locale.ENGLISH)+", "+fromDate.getDisplayName(Calendar.YEAR,Calendar.LONG, Locale.ENGLISH) + " to " + toDate.getDisplayName(Calendar.MONTH,Calendar.LONG, Locale.ENGLISH)+" "+toDate.getDisplayName(Calendar.DAY_OF_MONTH,Calendar.LONG, Locale.ENGLISH)+", "+toDate.getDisplayName(Calendar.YEAR,Calendar.LONG, Locale.ENGLISH));
            ret.add("Total Events in date range: " + numOfEvents);
            ret.add("Most attended event: " + maxAttended.getName() + " on " + maxAttended.getLongDate() + " " + maxAttended.getTime() + " with " + maxAttendees);
            ret.add("Total number of Residents for all events: " + totalAttendees);
            ret.add("Total number of Individual Residents who attended at least one event: " + individualAttendees.size());
        }
        return ret;
    }
    
    /**
     * Model's implementation to handle an event that has reached its maximum 
     * number of attendees.
     * 
     * Allows a user to 
     * a) increase (or disable) the max attendee limit 
     * b) start a waiting list for this event, 
     * c) Do nothing. If a resident sign-in triggered this function, the resident will not be added
     * 
     * @param event the event that has reached its maximum capacity
     * @return true if waitlist or increased attendees, false if bad authenication or user decides not to increase/activate max/waitlist
     */
    protected boolean maxAttendeeHandler(Event event) 
    {
        JTextField increaseBy = new JTextField();
        JRadioButton increase = new JRadioButton("Increase the limit (current limit: " + event.getMaxParticipants() + "). Increase limit by: ");
        Object[] op1 = {increase, increaseBy};
        JRadioButton op2 = new JRadioButton("Start a waiting list. \nAll IDs swiped for this event after this point will go on a printable waiting list");
        JRadioButton op3 = new JRadioButton("Don't add anymore attendees, including this one.");
        String message = "This event has reached its maxiumum number of attendees. This value was set when the event was created, \nYou can: ";
        Object[] options = {message, op1, op2, op3};
        int temp = JOptionPane.showOptionDialog(null, options, "Max Attendees Reached", JOptionPane.OK_OPTION, JOptionPane.INFORMATION_MESSAGE, null, null, null);
        boolean select = (temp != JOptionPane.NO_OPTION && temp != JOptionPane.CLOSED_OPTION);
        if(select && increase.isSelected()){
            try{
                int inc = Integer.parseInt(increaseBy.getText());
                synchronized(event) {
                    event.setMaxParticipants(event.getMaxParticipants()+inc);
                }
                return true;
            }
            catch(NumberFormatException | NullPointerException ex){
                JOptionPane.showMessageDialog(null, "Error: Invalid increase amount entered.", "Increase Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }        
        }
        if(select && op2.isSelected()){
            synchronized(event) {
                event.setAutoWaitlist(true);
            }
            return true;
        }
        return false;
    }
    
        
    //ADMIN-FUNCTIONS 
    
    /**
     * Private function to ensure adding an admin is standardized between 
     * changing model implementations
     * 
     * New administrator's ID is directly added without removing any 
     * additional input or symbols. This could cause issues across
     * different program input hardware (magnetic reader vs barcode
     * scanner)
     * 
     * @param ID the new Administrator's ID
     * @return 
     */
    
    private boolean addAdmin(String ID)
    {
        return admins.add(ID);
    }
    
    /**
     * Private function to ensure removing an admin is standardized between 
     * changing model implementations
     * 
     * Does not extract ID, so method of Admin removal (such as a 
     * magnetic card reader or barcode scanner) must be the same type used
     * when removing
     * 
     * @param ID the new Administrator's ID
     * @return 
     */
    
    private boolean removeAdmin(String ID)
    {
        return admins.remove(ID);
    }
    
    /**
     * Used to add new Administrators to the database
     * 
     * An existing admin is required to authenticate before adding the new admin
     * 
     * @return true if an existing admin is authenticated and a new admin is created
     * @throws AuthenticationFailedException
     */
    protected boolean adminCreation()
    {
        return(Admin_Authentication_Popup()&&adminCreationPopup());
    }
    
    /**
     * Used to remove admins from the database
     * 
     * @return 
     */
    
    protected boolean adminRemoval()
    {
        return(Admin_Authentication_Popup()&&adminRemovalPopup());
    }
    
    /**
     * Used for creating a new database administrator to allow them to
     * preform administrative functions
     * 
     * This method is called when the user requests to create a new admin
     * as well as when the database is initially created.
     * @return true if the new admin is created
     */
    
    private boolean adminCreationPopup()
    {
        String ID1 = (String)JOptionPane.showInputDialog(null,"Please swipe new Admin's ID card:", "New Admin",JOptionPane.QUESTION_MESSAGE);
        String ID2 = (String)JOptionPane.showInputDialog(null,"Please reswipe new Admin's ID card:", "New Admin",JOptionPane.QUESTION_MESSAGE);
        if(ID1 != null && ID2 != null && ID1.equals(ID2)){
            if(addAdmin(ID1)){
                JOptionPane.showMessageDialog(null, "New Admin Created Successfully", "New Admin Added", JOptionPane.INFORMATION_MESSAGE);
                return true;
            }
            else{
                JOptionPane.showMessageDialog(null, "An error occured while adding the new Admin. \nMost likely, this person is already an Admin", "Admin Creation Failure", JOptionPane.ERROR_MESSAGE);
            }
        }
        else {
            JOptionPane.showMessageDialog(null, "The two card readings do not match. Please try again.", "Admin Creation Failure", JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }
    
    /**
     * Used to remove a specific Admin from the database
     * 
     * Will not remove admin if number of Admins is less than 2 since
     * database requires at least 1 admin to function properly
     * 
     * @return true if the admin existed and was removed
     */
    
    private boolean adminRemovalPopup()
    {
        String ID1 = (String)JOptionPane.showInputDialog(null,"Please swipe Admin's ID card to remove:", "Remove Admin",JOptionPane.QUESTION_MESSAGE);
        if(ID1 != null && (JOptionPane.showConfirmDialog(null, "Perminately Remove Admin?", "Admin Removal", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)){
            if(admins.size()>1 && removeAdmin(ID1)){
                JOptionPane.showMessageDialog(null, "Admin Removed Successfully", "New Admin Added", JOptionPane.INFORMATION_MESSAGE);
                return true;
            }
            else{
                JOptionPane.showMessageDialog(null, 
                        "An error occured while removing the Admin. "
                        + "\nMost likely, this person is not an Admin "
                        + "\nOr this is the last admin in the database "
                        + "\n(database requires at least 1 admin to function)", 
                        "Admin Removal Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        else {
            JOptionPane.showMessageDialog(null, "Admin not removed. Invalid ID or Cancled", "Admin Removal Failure", JOptionPane.ERROR_MESSAGE);
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
    
    /**
     * Private function used to standardize validating admins across
     * different model implementations
     * 
     * @param ID the ID to check against all registered admins
     * @return true if the input ID is a registered admin
     */
    
    private boolean validateAdmin(String ID)
    {
        synchronized(admins){return(admins.contains(ID));}
    }    
    
    /**
     * Model's implementation to validate for an administrative function
     * Function itself is not synchronized for efficiency's sake
     * Critical function areas after user input are synchronized
     * 
     * This function will be the center of the future logging function.
     * Entries will be made by this function, which will take in a log enum type
     * and a message describing the what this authorization will do.
     * Entries will contain date/time, swiped ids, and results of authentication (enum)
     * (cancelled, approved, failed)
     * 
     * @return true if the user was authenticated, false if user canceled out
     * of the dialog, safely exits program if more than 5 failed attempts.
     */
    
    protected boolean Admin_Authentication_Popup()
    {
        if(checkAdminTolerance())
            return true;
        while(consecutive_failed_attempts < MAX_CONSECUTIVE_FAILED_ATTEMPTS)
        {
            String ID = (String)JOptionPane.showInputDialog(null,"Please swipe your ID card:", "Authentication Required for Action",JOptionPane.QUESTION_MESSAGE);
            if(ID==null){
                return false;
            } else if(validateAdmin((ID))){
                synchronized(last_admin_request) {  last_admin_request = System.nanoTime(); }
                synchronized(consecutive_failed_attempts) { consecutive_failed_attempts = 0;    }
                return true;
            } else {
                JOptionPane.showMessageDialog(null, "The read card is not an Admin User. This might be due to a bad reading. Please try again.", "Authentication Error", JOptionPane.ERROR_MESSAGE);           
                synchronized(consecutive_failed_attempts) { consecutive_failed_attempts++;  }
            }
        }
        synchronized(consecutive_failed_attempts) { consecutive_failed_attempts = 0;    } //Since the user will be forced to enter database password when restarting program
        System.exit(0);
        return false;
    }
    
    private synchronized boolean checkAdminTolerance()
    {
        return (last_admin_request != -1 && System.nanoTime()-last_admin_request<=ADMIN_VERIFICATION_TOLERANCE);
    }
    
    //Resident Functions
    
    protected boolean addResident(String ID, String lastName, String firstName, String bedSpace)
    {
        if(Admin_Authentication_Popup())
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
        if(Admin_Authentication_Popup())
        {
            if(JOptionPane.showConfirmDialog(null, "DELETE ALL RESIDENTS IN DATABASE? \nTHIS CANNOT BE UNDONE", "Delete Residents", JOptionPane.OK_CANCEL_OPTION)== JOptionPane.OK_OPTION)
                synchronized(residents)  {   residents.clear();  }
        }
    }
    
    /**
     * Utilizes poi-3.9 java project to import excel files
     */
    protected void excelImport()
    {
        try {
            JFileChooser fileChooserGUI = new JFileChooser(); //File Chooser window for user
            String[] acceptedFileTypes = {"xls", "xlsx", "xlsm"}; //restricts files to excel files
            
            TreeSet<Integer> usedRowsSet = new TreeSet<>(); //set to keep track of non-empty rows for sample
            TreeSet<Integer> usedColumnsSet = new TreeSet<>();  //set to keep track of non-empty columns for sample
            Integer[] usedRowsArr; //once usedRowSet is populated, contents are tranferred to this array
            Integer[] usedColumnsArr; //once usedColumnSet is populated, contents are tranferred to this array
            
            JTable selectorTable; //Table used to display sample of rows/columns
            Object[][] tableData; //populated with non-empty rows/columns of sample sets
            Object[] tableHeaders; //Column headers (numbers 1 to # of columns)
                                    
            fileChooserGUI.setFileFilter(new FileNameExtensionFilter(null, acceptedFileTypes)); //restrict file types
            
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
            
            selectorTable = new JTable(tableData, tableHeaders); //create table 
            selectorTable.setPreferredScrollableViewportSize(new Dimension(700, 300)); //MAGIC NUMBERS
            ColumnsAutoSizer.sizeColumnsToFit(selectorTable); //Autosizer function for tables
            selectorTable.setFillsViewportHeight(true);
            JScrollPane scoller = new JScrollPane(selectorTable); //add scroll pane to table
            
            
            Object[] message = {"Please select ONE cell that contains a user ID", scoller};
            if(JOptionPane.showConfirmDialog(null, message, "Select ID", JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION || selectorTable.getSelectedColumn() < 0 || selectorTable.getSelectedColumn() > usedColumnsArr.length-1)
                return; //Error message please
            ID_COLUMN_NUMBER = usedColumnsArr[selectorTable.getSelectedColumn()];

            Object[] message2 = {"Please select ONE cell that contains a LAST name",scoller};
            if(JOptionPane.showConfirmDialog(null, message2, "Select Last Name", JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION || selectorTable.getSelectedColumn() < 0 || selectorTable.getSelectedColumn() > usedColumnsArr.length-1)
                return; //Error message please
            LAST_NAME_COLUMN_NUMBER = usedColumnsArr[selectorTable.getSelectedColumn()];

            Object[] message3 = {"Please select ONE cell that contains a FIRST name",scoller};
            if(JOptionPane.showConfirmDialog(null, message3, "Select First Name", JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION || selectorTable.getSelectedColumn() < 0 || selectorTable.getSelectedColumn() > usedColumnsArr.length-1)
                return; //Error message please
            FIRST_NAME_COLUMN_NUMBER = usedColumnsArr[selectorTable.getSelectedColumn()];

            Object[] message4 = {"Please select ONE cell that contains a BedSpace (ex: CVA-000)",scoller};
            if(JOptionPane.showConfirmDialog(null, message4, "Select Bed Space", JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION || selectorTable.getSelectedColumn() < 0 || selectorTable.getSelectedColumn() > usedColumnsArr.length-1)
                return; //Error message please
            BEDSPACE_COLUMN_NUMBER = usedColumnsArr[selectorTable.getSelectedColumn()];

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
        }
    }
}
    
/*---------NOTES -------------
   
     * IMPORTANT: COLUMN WHERE DESIRED LOCATION IS HARD CODED. PERHAPS A LATER FEATURE WILL ALLOW THE USER TO SELECT THE COLUMNS
     * Split Bedspace into building and room/bed? No, not helpful for future with new buildings. Modify resident class to just directly accept room string
*/
