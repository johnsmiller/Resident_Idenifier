/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sjsurha.resident_identifier;

import com.toedter.calendar.JDateChooser;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 *
 * @author John
 */
public final class ViewEditEvent extends JPanel
{
    private final Model model;
    private final JTextField name;
    private final JComboBox eventCombobox;
    private final JDateChooser dateChooser;
    private final JTextField maxParticipants;
    private final JButton submitButton;
    private final JButton removeResident;
    private final JButton deleteButton;
    private final JComboBox hour;
    private final JComboBox minute;

    public ViewEditEvent(Model ModelIn)
    {
        super();
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        
        model = ModelIn;

        eventCombobox = model.getEventsJComboBox();
        eventCombobox.setMaximumSize(new Dimension(Integer.MAX_VALUE,24));
        eventCombobox.addActionListener(updateFieldsActionListener());//add actionlistener to update name/date/time/maxparticipant fields as selection changes
        this.add(eventCombobox);

        JPanel nme = new JPanel();
        name = new JTextField();
        name.setPreferredSize(new Dimension(300,25));
        nme.add(new JLabel("Event Name: "));
        nme.add(name);
        this.add(nme);

        dateChooser = new JDateChooser();
        dateChooser.setPreferredSize(new Dimension(100,25));

        hour = ViewerController.Get_Time_Selector()[0];
        minute = ViewerController.Get_Time_Selector()[1];
               
        maxParticipants = new JTextField();
        maxParticipants.addFocusListener(clearMaxParticipants());
        maxParticipants.setPreferredSize(new Dimension(125,25));
        
        updateFields();

        JPanel date_time = new JPanel();
        date_time.add(dateChooser);
        date_time.add(hour);
        date_time.add(minute);
        date_time.add(new JLabel("Max Participants: "));
        date_time.add(maxParticipants);
        this.add(date_time);

        submitButton = new JButton("Submit Changes");
        submitButton.addActionListener(Submit_ActionListener());

        removeResident = new JButton("Remove Resident");
        removeResident.addActionListener(removeAttendeeActionListener());

        deleteButton = new JButton("Delete Event");
        deleteButton.addActionListener(Delete_ActionListener());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(submitButton);
        buttonPanel.add(removeResident);
        buttonPanel.add(deleteButton);

        this.add(buttonPanel);
    }
    
    private void updateFields()
    {       
        if(eventCombobox.getItemCount()==0 || eventCombobox.getSelectedItem()==null)
            return;
        
        Event event = (Event)eventCombobox.getSelectedItem();
        
        name.setText(event.getName());

        dateChooser.setCalendar(event.getDateTime());
        
        hour.setSelectedIndex(event.getDateTime().get(Calendar.HOUR_OF_DAY));
        minute.setSelectedIndex(event.getDateTime().get(Calendar.MINUTE)/5);

        maxParticipants.setText(((event.getMaxParticipants()==-1)? "No Participant Limit" : event.getMaxParticipants()+""));
    }
    
    private ActionListener updateFieldsActionListener()
    {
        return new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                updateFields();
            }
        };
    }
    
    private FocusListener clearMaxParticipants()
    {
        return new FocusListener() {

            @Override
            public void focusGained(FocusEvent e) {
                if(maxParticipants.getText().equals("No Participant Limit"))
                        maxParticipants.setText("");
            }

            @Override
            public void focusLost(FocusEvent e) {
                if(maxParticipants.getText().length()<=0 || maxParticipants.getText().equals("-1") || maxParticipants.getText().equals("0"))
                        maxParticipants.setText("No Participant Limit");
            }
        };
    }

    private ActionListener Submit_ActionListener()
    {
        return new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                    Event event = (Event)eventCombobox.getSelectedItem();
                    GregorianCalendar tempDate = new GregorianCalendar();
                    tempDate.setTime(dateChooser.getDate());
                    tempDate.set(Calendar.HOUR_OF_DAY, hour.getSelectedIndex());
                    tempDate.set(Calendar.MINUTE, minute.getSelectedIndex()*5);
                    String message = event.getName() + " on " + event.getShortDate() + " " 
                            + event.getTime() 
                            + " will be updated to " 
                            + name.getText() + " on "
                            + tempDate.get(Calendar.MONTH) + "/"
                            + tempDate.get(Calendar.DATE) + "/"
                            + tempDate.get(Calendar.YEAR) + " "
                            + tempDate.get(Calendar.HOUR) + ":"
                            + ((tempDate.get(Calendar.MINUTE)<10)? "0" : "") 
                            + tempDate.get(Calendar.MINUTE) + " "
                            + tempDate.getDisplayName(Calendar.AM_PM, Calendar.LONG, Locale.US) + ". Continue?";
                    
                    if(!model.Admin_Authentication_Popup()) {
                        JOptionPane.showMessageDialog(null, "Admin Authentication Failed", "Remove Error", JOptionPane.ERROR_MESSAGE);
                    } else if(!model.ContainsEvent(event)){
                        JOptionPane.showMessageDialog(null, "Error: Event not found", "Event edit error", JOptionPane.ERROR_MESSAGE);
                    }else if(JOptionPane.showConfirmDialog(null, message)!=JOptionPane.YES_OPTION){
                    
                    } else{
                        int newMaxParticipants = event.getMaxParticipants();
                        try{
                            if(maxParticipants.getText().length()>0)
                                newMaxParticipants = Integer.parseInt(maxParticipants.getText());
                            else
                                newMaxParticipants = -1;
                        } catch (NumberFormatException ex){}
                        if(newMaxParticipants<event.getAttendees().size() && newMaxParticipants>0)
                            JOptionPane.showMessageDialog(null, "Error: Max Participants must be equal to or greater than current number of attendees\nCurrently, there are "+ event.getAttendees().size() + " attendees", "Event edit error", JOptionPane.ERROR_MESSAGE);
                        else if(model.updateEvent(event, name.getText(), tempDate, ((newMaxParticipants<1)? -1 : newMaxParticipants))){
                            JOptionPane.showMessageDialog(null, "Model Successfully updated", "Event Edit Success", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(null, "Error: An event with this name and date/time already exists", "Event edit error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
               }
        };        
    }

    private ActionListener removeAttendeeActionListener()
    {
    //Check for exceeding new maxAttendees
    //
        return new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                Event event = (Event)eventCombobox.getSelectedItem();
                String ID = ViewerController.extractID((String)JOptionPane.showInputDialog(null,"Enter ID to be Removed", "Remove resident from event", JOptionPane.QUESTION_MESSAGE));
                if(ID == null) {
                    JOptionPane.showMessageDialog(null, "Invalid ID", "Remove Error", JOptionPane.ERROR_MESSAGE);
                } else if(!model.Admin_Authentication_Popup()){
                    JOptionPane.showMessageDialog(null, "Admin Authentication Failed", "Remove Error", JOptionPane.ERROR_MESSAGE);
                } else if(!event.removeAttendee(ID)) {
                    JOptionPane.showMessageDialog(null, "Resident not removed\nMost likely, this event does not contain this resident", "Remove Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(null, "Resident succesfully removed");
                }
            }
        };
    }

    private ActionListener Delete_ActionListener()
    {
        return new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                Event event = (Event)eventCombobox.getSelectedItem();
                if(model.Admin_Authentication_Popup()){
                    if(model.ContainsEvent(event)){
                        if(JOptionPane.showConfirmDialog(null, event.getName() + " on " + event.getShortDate() + " " + event.getTime() + " will be deleted. Continue?")==JOptionPane.YES_OPTION){
                            model.removeEvent(event);
                            eventCombobox.repaint();
                        }   
                    }
                }                   
            }
        };
    }

}

