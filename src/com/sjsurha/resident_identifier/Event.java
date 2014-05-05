package com.sjsurha.resident_identifier;

import com.sjsurha.resident_identifier.Exceptions.CEDuplicateAttendeeException;
import com.sjsurha.resident_identifier.Exceptions.CEMaximumAttendeesException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.TreeMap;
import java.util.TreeSet;
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

//NOTE: THIS WILL BECOME A SUBCLASS OF MODEL AT DISTRIBUTION. CURRENTLY NOT SUBCLASS FOR EASE OF TESTING
public final class Event implements Comparable<Event>, Serializable{
    private static final long serialVersionUID = 1L;
    
    private String name;
    private GregorianCalendar date_time;
    private HashMap<String, GregorianCalendar> attendees; //Think about sorting by time (gregCal) of checkin to eliminate waitingList
    private TreeMap<GregorianCalendar, String> waitinglist;
    private boolean autoWaitlist; //Set if user wishes all future adds default to waiting list without asking
    private int max_participants;
    private TreeMap<String, Integer> tickets;
    
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
        waitinglist = new TreeMap<>();
        max_participants = -1;
        tickets = new TreeMap<>();
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
        waitinglist = new TreeMap<>();
        max_participants = Max_Participants;
        tickets = new TreeMap<>();
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
            return;
        }
        if(tickets.containsKey(ID))
            tickets.put(ID, tickets.get(ID)+tick);
        else
            tickets.put(ID, tick);
    }
    
    public TreeMap<String,Integer> getTickets()
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
            ret.addAll(waitinglist.values());
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
        boolean wait = false; //To prevent searching an arraylist multiple times
        if(isAttendee(ID) || (wait = isWaitlisted(ID))){
            if(suppressCheckinPrompt)
                throw new CEDuplicateAttendeeException("Resident is already attending this event or is on the waitlist");
           GregorianCalendar time = attendees.get(ID);
           if(wait){
               ArrayList<String> temp = new ArrayList<>(waitinglist.values());
               int loc = temp.indexOf(ID);
               time = (GregorianCalendar)waitinglist.keySet().toArray()[loc];
           }
           int[] arr = getTimeDifference(time, new GregorianCalendar());
           
           String message = "Resident already swiped in\n" 
                   + arr[0] + " Days " + arr[1] + " Hours " 
                   + arr[2] + " Minutes " + arr[3] + " Seconds ago\nRecheck in? "
                   + "(Previous time is discarded. This does not affect number of tickets)";
           
           if(JOptionPane.showConfirmDialog(null, message, "Duplicate Resident", JOptionPane.OK_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE) != JOptionPane.OK_OPTION)
               throw new CEDuplicateAttendeeException("Resident is already attending this event or is on the waitlist");
           if(!wait){
               attendees.put(ID, new GregorianCalendar());
               return true;
           }
           else{
               waitinglist.remove(time);
               waitinglist.put(new GregorianCalendar(), ID);
               return true;
           }
        } else if(maxAttendee() && !autoWaitlist){
            if(maxAttendeeHandler())
                return validAttendee(ID, suppressCheckinPrompt);
            else
                return false;
        }
        else if(maxAttendee() && autoWaitlist)
            return addWaitlist(ID);
        else 
            return addAttendee(ID);        
    }
    
    private int[] getTimeDifference(GregorianCalendar start, GregorianCalendar stop)
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
        return waitinglist.containsValue(ID);
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
        waitinglist.put(new GregorianCalendar(), ID);
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
    public TreeMap<GregorianCalendar, String> getWaitinglist() {
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
     * Returns a runnable thread that searches this event for the given ID
     * If found, the thread adds this event to the given Tree<Set>
     * NOTE: This implementation provides synchronized functionality. 
     * 
     * @param id
     * @param results
     * @return 
     */
    public Runnable search(final String id, final TreeSet<Event> results)
    {
        final Event th = this;
        return new Runnable() {

            @Override
            public void run() {
                if(attendees.containsKey(id) || waitinglist.containsValue(id)){ //Short-circuit array O(n) lookup if in attendees
                    results.add(th); //DIFFERINTIATE BETWEEN WAITINGLIST AND ATTENDEE!!!!!
                }
            }
        };
    }
    
    /**
     * Event's implementation to handle an event that has reached its maximum 
     * number of attendees.
     * 
     * Allows a user to 
     * a) increase (or disable) the max attendee limit 
     * b) start a waiting list for this event, 
     * c) Do nothing. If a resident sign-in triggered this function, the resident will not be added
     * 
     * @return true if waitlist or increased attendees, false if bad authenication or user decides not to increase/activate max/waitlist
     */
    private boolean maxAttendeeHandler() 
    {
        JTextField increaseBy = new JTextField();
        JRadioButton increase = new JRadioButton("Increase the limit (current limit: " + getMaxParticipants() + "). Increase limit by: ");
        Object[] op1 = {increase, increaseBy};
        JRadioButton op2 = new JRadioButton("Start a waiting list. \nAll IDs swiped for this event after this point will go on a printable waiting list");
        JRadioButton op3 = new JRadioButton("Don't add anymore attendees, including this one.");
        String message = "This event has reached its maxiumum number of attendees. This value was set when the event was created, \nYou can: ";
        Object[] options = {message, op1, op2, op3};
        int temp = JOptionPane.showOptionDialog(null, options, "Max Attendees Reached", JOptionPane.OK_OPTION, JOptionPane.INFORMATION_MESSAGE, null, null, null);
        boolean select = (temp != JOptionPane.NO_OPTION && temp != JOptionPane.CLOSED_OPTION);
        if(select && increase.isSelected()){
            try{
                int inc;
                if((inc = Integer.parseInt(increaseBy.getText())) < 0) {
                    JOptionPane.showMessageDialog(null, "Error: Invalid increase amount entered.", "Increase Error", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
                synchronized(this) {
                    setMaxParticipants(getMaxParticipants()+inc);
                }
                return true;
            }
            catch(NumberFormatException | NullPointerException ex){
                JOptionPane.showMessageDialog(null, "Error: Invalid increase amount entered.", "Increase Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }        
        }
        if(select && op2.isSelected()){
            synchronized(this) {
                setAutoWaitlist(true);
            }
            return true;
        }
        return false;
    }
}
