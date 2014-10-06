/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sjsurha.resident_identifier;

import com.toedter.calendar.JCalendar;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Calendar;
import java.util.Date;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableModel;

/**
 *
 * @author John
 */
public final class ViewCreateEvent extends JPanel
{
    private final Model model;
    private JTextField name_textfield;
    private JCalendar date_selection;
    private JPanel time_panel;
    private JTextField max_participants_textfield;
    private JButton submit_button;
    private JTable restrict_buildings;

    public ViewCreateEvent(Model ModelIn)
    {
        super(new BorderLayout());
        model = ModelIn;
        build();
    }

    private void build()
    {
        name_textfield = new JTextField();
        name_textfield.setPreferredSize(new Dimension(150,23));
        JPanel name_panel = new JPanel();
        name_panel.add(new JLabel("Event Name: "));
        name_panel.add(name_textfield);

        max_participants_textfield = new JTextField("0");
        max_participants_textfield.setPreferredSize(new Dimension(40,23));
        JPanel participant_panel = new JPanel();
        participant_panel.add(new JLabel("Optional Limit of Participants"));
        participant_panel.add(max_participants_textfield);


        date_selection = new JCalendar(new Date(), true);
        JPanel right_panel = new JPanel(new BorderLayout());
        right_panel.add(new JLabel("Date: "), BorderLayout.NORTH);
        right_panel.add(date_selection, BorderLayout.CENTER);


        time_panel = new JPanel();
        final JComboBox hour = ViewerController.Get_Time_Selector()[0];
        final JComboBox minute = ViewerController.Get_Time_Selector()[1];
        hour.setSelectedIndex(Calendar.getInstance().get(Calendar.HOUR_OF_DAY));
        minute.setSelectedIndex(0);
        time_panel.add(new JLabel("Time: "));
        time_panel.add(hour);
        time_panel.add(minute);
        
        restrict_buildings = model.getBuildingJTable();
        
        JScrollPane scroll_restrict_buildings = new JScrollPane(restrict_buildings);
        scroll_restrict_buildings.setPreferredSize(new Dimension(50, 150)); //MAGIC NUMBERS

        submit_button = new JButton("Submit");
        submit_button.addActionListener(Submit_ActionListener(hour, minute));
        name_textfield.addActionListener(Submit_ActionListener(hour, minute));

        JPanel left_panel = new JPanel();
        left_panel.setLayout(new BoxLayout(left_panel, BoxLayout.Y_AXIS));
        left_panel.add(name_panel);
        left_panel.add(time_panel);
        left_panel.add(participant_panel);
        left_panel.add(scroll_restrict_buildings);


        this.add(left_panel, BorderLayout.WEST);
        this.add(right_panel, BorderLayout.CENTER);
        this.add(submit_button, BorderLayout.SOUTH);
        
        clear();
    }

    private void clear()
    {
        name_textfield.setText("");
        max_participants_textfield.setText("0");
        TableModel tableModel = restrict_buildings.getModel();
        for(int i = 0; i<tableModel.getRowCount(); i++)
        {
            tableModel.setValueAt(true, i, 0);
        }
        this.repaint();
    }

    private ActionListener Submit_ActionListener(final JComboBox hour, final JComboBox minute)
    {
        return new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e){
                if(name_textfield.getText().isEmpty()){
                    JOptionPane.showMessageDialog(null, "Event Name cannot be empty. \nEvent not added.", "Event Creation Failure", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                Calendar cal = date_selection.getCalendar();
                cal.set(Calendar.HOUR_OF_DAY, hour.getSelectedIndex());
                cal.set(Calendar.MINUTE, minute.getSelectedIndex()*5);
                String max_partic_string = max_participants_textfield.getText();
                Integer max_partic_int = 0;
                try{ max_partic_int = Integer.parseInt(max_partic_string); } catch (NullPointerException ex){}
                
                if(max_partic_int > 0) {
                    Event temp_event = new Event(name_textfield.getText(), cal, ViewerController.getBuildings(restrict_buildings), max_partic_int);
                    if(model.addEvent(temp_event)) {
                        clear();
                    } else
                        JOptionPane.showMessageDialog(null, "Error: Conflicting Events. \nTwo events may not have the same date/time and name", "Event Creation Error", JOptionPane.ERROR_MESSAGE);
                } 
                
                else if(!max_partic_string.isEmpty() && !max_partic_string.equalsIgnoreCase("0")){
                    JOptionPane.showMessageDialog(null, "Invalid Limit of Participants.\nPositive integers only \nEvent not added.", "Event Creation Failure", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                if (!model.authenticationModule(LogEntry.Level.User, "New Event: " + name_textfield.getText())){
                    JOptionPane.showMessageDialog(null, "Authentication Failed. Event not added.", "Authentication Failure", JOptionPane.ERROR_MESSAGE);
                    return;
                } 
                
                else {
                    Event temp_event = new Event(name_textfield.getText(), cal, ViewerController.getBuildings(restrict_buildings)); 
                    if(model.addEvent(temp_event)){
                        clear();
                    } else
                        JOptionPane.showMessageDialog(null, "Error: Conflicting Events. \nTwo events may not have the same date/time and name", "Event Creation Error", JOptionPane.ERROR_MESSAGE);                            
                }
            }
        };
    }
 }
