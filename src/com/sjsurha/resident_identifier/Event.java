package com.sjsurha.resident_identifier;

import com.sjsurha.resident_identifier.Exceptions.CEDuplicateAttendeeException;
import com.sjsurha.resident_identifier.Exceptions.CEMaximumAttendeesException;
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
import javax.swing.ButtonGroup;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author John
 */

public final class Event implements Comparable<Event>, Serializable{
    private static final long serialVersionUID = 1L;
    
    private String name;
    private GregorianCalendar date_time; 
    private HashMap<String, GregorianCalendar> attendees; //Think about sorting by time (gregCal) of checkin to eliminate waitingList. <-- Bad idea. Will most likely be O(n) every time
    private HashMap<String, GregorianCalendar> waitinglist; //Change to hashmap. Better to have O(1) lookup & add with O(n) sort than O(n) add and O(1) sort
    private boolean autoWaitlist; //Set if user wishes all future adds default to waiting list without asking
    private int max_participants;
    private HashMap<String, Integer> tickets;
    //private HashMap<String, HashMap<String, Integer>> ticketCatagories; //<Catagory Name, <ID, # of Tickets>>
    
    /**
     *
     * @param Name
     * @param DateTime
     */
    public Event(String Name, GregorianCalendar DateTime)
    {
        name = Name;
        date_time = DateTime;
        attendees = new HashMap<>(100);
        waitinglist = new HashMap<>();
        max_participants = -1;
        tickets = new HashMap<>();
    };
    
    /**
     *
     * @param Name
     * @param Date_Time
     * @param Max_Participants
     */
    public Event(String Name, GregorianCalendar Date_Time, int Max_Participants)
    {
        name = Name;
        date_time = Date_Time;
        attendees = new HashMap<>(Max_Participants);
        waitinglist = new HashMap<>();
        max_participants = Max_Participants;
        tickets = new HashMap<>();
    };
    
    //Thread-enabled search function. Passed a Storage container of events, Storage.add(this) if resident attended event
        //Must lock Storage if adding since multiple events will be running this function simultaneously
        //Elements sorted in storage automatically by date, so will be sorted when method exits
            //Handle case of multiple events on same date/time
    @Override
    public String toString()
    {
        return (date_time.get(Calendar.MONTH)+1)+"/"+date_time.get(Calendar.DAY_OF_MONTH)+"/"+date_time.get(Calendar.YEAR)+" "+name;
    }
    
    public boolean removeAttendee(String ID)
    {
        return(attendees.remove(ID)!= null);
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
            if(tickets.containsKey(ID) && tickets.get(ID)>=tick)
                tickets.put(ID, tickets.get(ID)+tick);
        }
        else if(tickets.containsKey(ID))
            tickets.put(ID, tickets.get(ID)+tick);
        else
            tickets.put(ID, tick);
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
    public boolean validAttendee(String ID, boolean suppressCheckinPrompt) throws CEDuplicateAttendeeException, CEMaximumAttendeesException
    {
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
            if(!maxAttendee()) //Event is NOT full. Check in as attendee
            {
                attendees.put(ID, new GregorianCalendar());
                return true;
            }
            else if(autoWaitlist) //Event IS full and automatic waitlist IS selected
            {
                waitinglist.put(ID,new GregorianCalendar());
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
        
        timeDifference = getTimeDifference(checkinTime, new GregorianCalendar());
        
        message = "Resident already swiped in\n" 
                   + timeDifference[0] + " Days " + timeDifference[1] + " Hours " 
                   + timeDifference[2] + " Minutes " + timeDifference[3] + " Seconds ago\nRecheck in? "
                   + "(Previous time is discarded. This does not affect number of tickets)";
        
           if(JOptionPane.showConfirmDialog(null, message, "Duplicate Resident", JOptionPane.OK_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE) != JOptionPane.OK_OPTION)
               throw new CEDuplicateAttendeeException("Resident is already attending this event or is on the waitlist");
           else if(isAttendee(ID)){
               attendees.put(ID, new GregorianCalendar());
               return true;
           }
           else{
               waitinglist.put(ID, new GregorianCalendar());
               return true;
           }
    }
    
    protected static int[] getTimeDifference(GregorianCalendar start, GregorianCalendar stop)
    {
        if(start.compareTo(stop)>0){
            GregorianCalendar temp = start;
            start = stop;
            stop = temp;
        }
        double diff_seconds = (stop.getTimeInMillis() - start.getTimeInMillis())/1000.0;
        int days = 0;
        int hours = 0;
        int minutes = 0;
        int seconds = 0;
        while(diff_seconds>=86400){
            days++;
            diff_seconds-=86400;
        } 
        while(diff_seconds>=3600){
            hours++;
            diff_seconds-=3600;
        }
        while(diff_seconds>=60){
            minutes++;
            diff_seconds-=60;
        }
        seconds = (int) diff_seconds;
        int[] ret = {days,hours,minutes,seconds};
        return ret;
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
    
    public String Get_Details()
    {
        String ret = "Name: " + name + " \nDate: " 
                + date_time.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.US) + ", " 
                + getLongDate() + "\t" + getTime()
                + " \nNumber of Attendees: " + attendees.size() + "\nWaiting List size: " 
                + ((waitinglist!=null)? waitinglist.size():0)
                + ((max_participants==-1)? "\nNo participant limit" : "\nMax Participants: " + max_participants);
        
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
        return true;
    }
    
    private boolean addWaitlist(String ID)
    {     
        waitinglist.put(ID, new GregorianCalendar());
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
    public GregorianCalendar getDateTime() {
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
        if(attendees.containsKey(id) || waitinglist.containsKey(id))
        {
            return true;
        }
        return false;
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
}

/**
     * Remnant of TreeMap WaitingList. Couldn't bring myself to delete it
     * 
     * Does O(1) lookup in attendees. If resident found, event is added to 
     * results TreeSet and function returns.
     * <br></br>
     * <br></br>
     * Else, if the resident is not in the attendees and the waiting list is 
     * populated, a new thread is launched that searches the waiting list in 
     * O(n) time and this function returns to allow the calling function to 
     * continue
     * 
     * <p>NOTE: This implementation DOES NOT provide synchronized functionality.
     * It's assumed that the TreeSet paramter <b>results</b> provides synchronized 
     * add function</p>
     * 
     * @param id Student ID to search events for
     * @param results the modified treeset with synchronized add function
     * @param sem Semaphore used to signal thread has completed
     */ /*
    public void search(final String id, final TreeSet<Event> results, final Semaphore sem)
    {
        //Short-circuit array O(n) lookup & thread creation if in attendees
        if(attendees.containsKey(id))
        {
            results.add(this);
            //SIGNAL THREAD HAS FINISHED 
            sem.release();
            return;
        }
        //Launch Thread for array O(n) lookup, allow calling function to continue
        else if (waitinglist.size() > 0) {
            final Event th = this;
            Thread t1 = new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        if(waitinglist.containsValue(id)){
                            results.add(th);
                        //DIFFERINTIATE BETWEEN WAITINGLIST AND ATTENDEE??
                        }
                        //SIGNAL THREAD HAS FINISHED 
                        sem.release();
                    }
                }
            );
            t1.start();
            return;
        }
        
        else {
            sem.release();
            return;
        }
    } */