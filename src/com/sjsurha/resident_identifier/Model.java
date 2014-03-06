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
import java.util.Collection;
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
import javax.swing.ComboBoxModel;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableModel;
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
    private final int MAX_CONSECUTIVE_FAILED_ATTEMPTS = 5;
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
     *
     * @return
     */
    protected EventTreeSet_TableModel_ComboBoxModel getEvents()
    {
        return events;
    }
    
    private void importEvents()
    {
        try {
            ObjectInputStream input = new ObjectInputStream(new FileInputStream(new File("RecoveredEvents.obj")));
            TreeSet<Event> ImportedEvents = (TreeSet<Event>) input.readObject();
            events.addAll(ImportedEvents);
            input.close();
            JOptionPane.showMessageDialog(null, ImportedEvents.size() + " Events Imported successfully.", "Import complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(Model.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(null, "Error: Import unsuccessful.", "Import error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
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
     *
     * @param fromDate
     * @param toDate
     * @return
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
     *
     * @param fromDate
     * @param toDate
     * @return
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
            ret.add("Most attended event: "+maxAttended.getName()+" on "+maxAttended.getDateTime().getDisplayName(Calendar.MONTH,Calendar.LONG, Locale.ENGLISH)+" "+maxAttended.getDateTime().getDisplayName(Calendar.DAY_OF_MONTH,Calendar.LONG, Locale.ENGLISH)+", "+maxAttended.getDateTime().getDisplayName(Calendar.YEAR,Calendar.LONG, Locale.ENGLISH)+" with " + maxAttendees);
            ret.add("Total number of Residents for all events: " + totalAttendees);
            ret.add("Total number of Individual Residents who attended at least one event: " + individualAttendees.size());
        }
        return ret;
    }
    
    /**
     *
     * @param event
     * @return
     */
    protected boolean maxAttendeeHandler(Event event) //true if waitlist or increased attendees, false if bad authenication or user decides not to increase/activate max/waitlist
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
    
    private boolean addAdmin(String ID)
    {
        return admins.add(ID);
    }
    
    /**
     *
     * @return
     * @throws AuthenticationFailedException
     */
    protected boolean adminCreation()
    {
        return(Admin_Authentication_Popup()&&adminCreationPopup());
    }
    
    /**
     * Used for creating a new database administrator to allow them to
     * preform administrative functions
     * 
     * This method is called when the user requests to create a new admin
     * as well as when the database is initially created.
     * @return 
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
                JOptionPane.showMessageDialog(null, "An error occured while adding the new Admin. \nMost likely, this person is already an Admin", "Admin Creation Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        else {
            JOptionPane.showMessageDialog(null, "The two card readings do not match. Please try again.", "Admin Creation Error", JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }
    
    protected int adminCount()
    {
        synchronized(admins){
            if(admins != null)
                return admins.size();
            return 0;
        }
    }
    
    private boolean validateAdmin(String ID)
    {
        synchronized(admins){return(admins.contains(ID));}
    }    
    
    /**
     *
     * @return
     * @throws AuthenticationFailedException
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
                return;
            ID_COLUMN_NUMBER = usedColumnsArr[selectorTable.getSelectedColumn()];

            Object[] message2 = {"Please select ONE cell that contains a LAST name",scoller};
            if(JOptionPane.showConfirmDialog(null, message2, "Select Last Name", JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION || selectorTable.getSelectedColumn() < 0 || selectorTable.getSelectedColumn() > usedColumnsArr.length-1)
                return;
            LAST_NAME_COLUMN_NUMBER = usedColumnsArr[selectorTable.getSelectedColumn()];

            Object[] message3 = {"Please select ONE cell that contains a FIRST name",scoller};
            if(JOptionPane.showConfirmDialog(null, message3, "Select First Name", JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION || selectorTable.getSelectedColumn() < 0 || selectorTable.getSelectedColumn() > usedColumnsArr.length-1)
                return;
            FIRST_NAME_COLUMN_NUMBER = usedColumnsArr[selectorTable.getSelectedColumn()];

            Object[] message4 = {"Please select ONE cell that contains a BedSpace (ex: CVA-000)",scoller};
            if(JOptionPane.showConfirmDialog(null, message4, "Select Bed Space", JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION || selectorTable.getSelectedColumn() < 0 || selectorTable.getSelectedColumn() > usedColumnsArr.length-1)
                return;
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
    
    /**
     * TreeSet<Event> class that implements the TableModel and ComboBoxModel interfaces
     */
    public class EventTreeSet_TableModel_ComboBoxModel extends TreeSet<Event> implements TableModel, ComboBoxModel<Event>
    {
        private static final long serialVersionUID = 1L;
        private final String[] headers = {"Select", "Event Date", "Event Name", "Attendees", "Waitlisted"};
        private boolean[] tableBooleanSelection;
        private HashSet<TableModelListener> tableModelListeners;
        private HashSet<ListDataListener> comboBoxModelListDataListeners;
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
        
         @Override
        public boolean add(Event e)
        {
            if(super.add(e)) {
                comboBoxModelSelectedItem = e;
                modelListenersNotify();
                return true;
            }
            return false;
        }
        
        @Override
        public boolean remove(Object o)
        {
            if(super.remove(o)){
                if(comboBoxModelSelectedItem.equals(o))
                    comboBoxModelSelectedItem = ((this.size()>0)? this.first() : null);
                modelListenersNotify();
                return true;
            }
            return false;
        }
        
        @Override
        public boolean addAll(Collection<? extends Event> c)
        {
            if(super.addAll(c)){
                modelListenersNotify();
                return true;
            }
            return false;  
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
}

/*---------NOTES -------------
   
     * IMPORTANT: COLUMN WHERE DESIRED LOCATION IS HARD CODED. PERHAPS A LATER FEATURE WILL ALLOW THE USER TO SELECT THE COLUMNS
     * Split Bedspace into building and room/bed? No, not helpful for future with new buildings. Modify resident class to just directly accept room string
*/
