package com.sjsurha.resident_identifier;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;
import javax.swing.ButtonGroup;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JTable;
import javax.swing.JTextField;

public final class Event implements Comparable<Event>, Serializable, TreeSet_TableModel_ComboBoxModel.TableModel_ComboModel_Interface{
    private static final long serialVersionUID = 2;

    private String name;
    private Calendar date_time; 
    private final HashMap<String, GregorianCalendar> attendees; //Think about sorting by time (gregCal) of checkin to eliminate waitingList. <-- Bad idea. Will most likely be O(n) every time
    private final HashMap<String, GregorianCalendar> waitinglist; //Change to hashmap. Better to have O(1) lookup & add with O(n) sort than O(n) add and O(1) sort
    private TreeMap<Building, Integer> eventBuildings;
    private boolean autoWaitlist; //Set if user wishes all future adds default to waiting list without asking
    private int max_participants;
    private final HashMap<String, Integer> tickets;
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

        for(Building b : acceptedBuildings){
            eventBuildings.put(b, 0);
        }
    };

    /**
     *
     * @param Name
     * @param Date_Time
     * @param Max_Participants
     * @param acceptedBuildings
     */
    public Event(String Name, Calendar Date_Time, Building[] acceptedBuildings, int Max_Participants)
    {
        name = Name;
        date_time = Date_Time;
        attendees = new HashMap<>(Max_Participants);
        waitinglist = new HashMap<>();
        max_participants = Max_Participants;
        tickets = new HashMap<>();
        eventBuildings = new TreeMap<>();

        for(Building b : acceptedBuildings){
            eventBuildings.put(b, 0);
        }
    };

    public boolean isBuilding(Building building)
    {
        return eventBuildings.containsKey(building);
    }

    @Override
    public String toString()
    {
        return (date_time.get(Calendar.MONTH)+1)+"/"+date_time.get(Calendar.DAY_OF_MONTH)+"/"+date_time.get(Calendar.YEAR)+" "+name;
    }

    public boolean removeParticipant(String ID, Model model)
    {
        if(attendees.remove(ID)!=null || waitinglist.remove(ID) != null)
        {
            Building b = model.extractBuilding(ID);
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

    private boolean addAttendee(String ID, Model model)
    {
        if(attendees.containsKey(ID) || waitinglist.containsKey(ID)){
            attendees.put(ID, new GregorianCalendar());
            return true;
        }

        attendees.put(ID, new GregorianCalendar());
        Building b = model.extractBuilding(ID);
        if(eventBuildings.containsKey(b))
            eventBuildings.put(b, eventBuildings.get(b)+1);
        else
            eventBuildings.put(b, 0);
        return true;
    }

    private boolean addWaitlist(String ID, Model model)
    {
        if(attendees.containsKey(ID) || waitinglist.containsKey(ID)){
            waitinglist.put(ID, new GregorianCalendar());
            return true;
        }

        waitinglist.put(ID, new GregorianCalendar());
        Building b = model.extractBuilding(ID);
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
     * @return  
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

    protected JTable getAttendeesJTable(Model model)
    {
        return getEventJTable(attendees, model);
    }

    protected JTable getWaitlistJTable(Model model)
    {
        return getEventJTable(waitinglist, model);
    }

    /**
    * Formats an event's attendees list into an Object 2-dimensional
    * array for use in a JTable.
    * 
    * Assumed Columns: Check-in Time, Student ID, Last Name, 
    * First Name, Bedspace, Tickets
    * 
    * @param checkInData the event whose attendees list is to be formatted
    * @param model the model to pull the resident data from
    * @return a two-dimensional array formatted for display in a JTable
    */
   private JTable getEventJTable(HashMap<String, GregorianCalendar> checkInData, Model model)
   {
       String[] columnNames = {"Check-in", "ID", "Last Name", "First Name", "Bedspace", "Tickets"};

       Object[][] ret = new Object[checkInData.size()][6];
       int i = 0;
       for(String id : checkInData.keySet())
       {
           String[] nameBedspace = model.getNameBedspace(id);
           ret[i][0] = checkInData.get(id).getTime().toString();
           ret[i][1] = id;
           ret[i][2] = nameBedspace[0];
           ret[i][3] = nameBedspace[1];
           ret[i][4] = nameBedspace[2];
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
     * @param model
     * @param suppressCheckinPrompt if true, does not prompt to update check in
     *  time
     * @return true if the resident is successfully checked in / check in time
     *  is updated
     * @throws com.sjsurha.resident_identifier.Exceptions.CEDuplicateAttendeeException
     * @throws com.sjsurha.resident_identifier.Exceptions.CEMaximumAttendeesException
     * @throws com.sjsurha.resident_identifier.Exceptions.CENonResidentException
     * @throws com.sjsurha.resident_identifier.Exceptions.CENonResidentException.CEUnpermittedBuildingException
     */
    public boolean validAttendee(String ID, Model model, boolean suppressCheckinPrompt) throws Exceptions.CEDuplicateAttendeeException, Exceptions.CEMaximumAttendeesException, Exceptions.CENonResidentException, Exceptions.CENonResidentException.CEUnpermittedBuildingException
    {
        if(ID == null || !model.checkResident(ID)){
            throw new Exceptions.CENonResidentException("Student ID not in Resident Database");//Change to JDialog?
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
            if(eventBuildings.size() > 0 && !eventBuildings.containsKey(model.extractBuilding(ID)))
            {
                throw new Exceptions.CENonResidentException.CEUnpermittedBuildingException("Resident's Building not allowed to check in");
            }
            else if(!maxAttendee()) //Event is NOT full. Check in as attendee
            {
                addAttendee(ID, model);
                return true;
            }
            else if(autoWaitlist) //Event IS full and automatic waitlist IS selected
            {
                addWaitlist(ID, model);
                return true;
            }
            else //Event IS full and automatic waitlist is NOT selected
            {
                if(maxAttendeeHandler())
                    return validAttendee(ID, model, suppressCheckinPrompt);
                else
                    return false;
            }
        }

        if(suppressCheckinPrompt) 
        {            
            throw new Exceptions.CEDuplicateAttendeeException("Resident is already attending this event or is on the waitlist");
        }

        timeDifference = ViewerController.getTimeDifference(checkinTime, new GregorianCalendar());

        message = "Resident already swiped in\n" 
                   + timeDifference[0] + " Days " + timeDifference[1] + " Hours " 
                   + timeDifference[2] + " Minutes " + timeDifference[3] + " Seconds ago\nRecheck in? "
                   + "(Previous time is discarded. This does not affect number of tickets)";

           if(JOptionPane.showConfirmDialog(null, message, "Duplicate Resident", JOptionPane.OK_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE) != JOptionPane.OK_OPTION)
               throw new Exceptions.CEDuplicateAttendeeException("Resident is already attending this event or is on the waitlist");
           else if(isAttendee(ID)){
               addAttendee(ID, model);
               return true;
           }
           else{
               addWaitlist(ID, model);
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

    protected void updateBuildings(Building[] allowedBuildings) {
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
            restore = new Building(b.getName().replace("_REMOVED", ""));
            if(newBuildings.containsKey(restore))
            {
                newBuildings.put(restore, eventBuildings.get(b));
            }
            else if(eventBuildings.get(b) > 0)
            {
                newBuildings.put(new Building(b.getName() + "_REMOVED"), eventBuildings.get(b));
            }
        }

        eventBuildings = newBuildings;          
    }
}