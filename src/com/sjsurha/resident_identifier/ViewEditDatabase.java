/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sjsurha.resident_identifier;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextPane;

/**
 *
 * @author John
 */
public final class ViewEditDatabase extends JPanel
{
    private final Model model;
    private final JTextPane currentResCount;
    private final JPanel buttonPanel;
    
    private final JButton importResidents;
    private final JButton addResident;
    private final JButton deleteResidents; //DELETE ALL RESIDENTS BUTTON
    
    private final JButton mergeDatabase;
    
    private final JButton deleteEvents;
            
    private final JButton addAdmin;
    private final JButton removeAdmin;

    public ViewEditDatabase(Model ModelIn)
    {
        super();
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        
        model = ModelIn;

        currentResCount = new JTextPane();
        updateText();
        currentResCount.setEditable(false);
        this.add(currentResCount);
                
        JPanel eventButtonPanel = new JPanel();
        JPanel adminButtonPanel = new JPanel();
        JPanel residentButtonPanel = new JPanel();
        
        deleteEvents = new JButton("Delete All Events");
        deleteEvents.setEnabled(false);//deleteEvents.addActionListener(deleteEventsActionListener());
        eventButtonPanel.add(deleteEvents);
        
        addAdmin = new JButton("Add New Admin");
        addAdmin.addActionListener(Add_Admin_ActionListener());
        adminButtonPanel.add(addAdmin);
        
        removeAdmin = new JButton("Remove Admin");
        removeAdmin.setEnabled(false);//removeAdmin.addActionListener(removeAdminActionListener());
        adminButtonPanel.add(removeAdmin);
        
        mergeDatabase = new JButton("Merge with Existing Database");
        mergeDatabase.setEnabled(false);//mergeDatabase.addActionListener(mergeActionListener());
        adminButtonPanel.add(mergeDatabase);

        importResidents = new JButton("Import Residents");
        importResidents.addActionListener(Import_ActionListener());
        residentButtonPanel.add(importResidents);

        addResident = new JButton("Manually Add Resident");
        addResident.addActionListener(addResidentActionListener());
        residentButtonPanel.add(addResident);

        deleteResidents = new JButton("Delete All Residents");
        deleteResidents.addActionListener(deleteResidents());
        residentButtonPanel.add(deleteResidents);
        
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        
        buttonPanel.add(eventButtonPanel);
        buttonPanel.add(adminButtonPanel);
        buttonPanel.add(residentButtonPanel);

        this.add(buttonPanel);
    }

    @Override
    public void repaint()
    {
        if(currentResCount != null)
            updateText();
        super.repaint();
    }

    private void updateText()
    {
        currentResCount.setText("Number of Events in database: " + model.getEvents().size() + "\nNumber of Admins in Database: " + model.adminCount() + "\nNumber of Residents in Database: " + model.residentCount());
    }

    private ActionListener Import_ActionListener()
    {
        return new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if(model.Admin_Authentication_Popup()){
                    model.excelImport();
                    updateText();
                }
            }
        };
    }

    private ActionListener deleteResidents()
    {
        return new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                model.emptyResidentDatabase();
                updateText();
            }
        };
    }

    private ActionListener Add_Admin_ActionListener()
    {
        return new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                model.adminCreation();
                updateText();
            }
        };
    }

    private ActionListener addResidentActionListener()
    {
        return new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                String ID = (String)JOptionPane.showInputDialog(null,"Please swipe Resident's Student ID card:", "Student ID",JOptionPane.QUESTION_MESSAGE);
                if(ID == null || ID.length()<10){
                    JOptionPane.showMessageDialog(null, "Error: Card not read, please try again.");
                    return;
                }

                String firstName = (String)JOptionPane.showInputDialog(null,"Please enter Resident's FIRST name:", "Student Name",JOptionPane.QUESTION_MESSAGE);
                if(firstName == null){
                    JOptionPane.showMessageDialog(null, "Error: No name entered, please try again.");
                    return;
                }

                String lastName = (String)JOptionPane.showInputDialog(null,"Please enter Resident's LAST name:", "Student Name",JOptionPane.QUESTION_MESSAGE);
                if(lastName == null){
                    JOptionPane.showMessageDialog(null, "Error: No name entered, please try again.");
                    return;
                }

                String bedSpace = (String)JOptionPane.showInputDialog(null,"Please enter Resident's Hall, Room, and Bedspace \n(Ex: CVC-407Ca, HV-102Ab, JW-1207Cb)", "Student Room",JOptionPane.QUESTION_MESSAGE);
                if(bedSpace == null){
                    JOptionPane.showMessageDialog(null, "Error: No room entered, please try again.");
                    return;
                }

                model.addResident(ID.substring(1, 10), lastName, firstName, bedSpace);
                updateText();
            }
        };
    }

}
