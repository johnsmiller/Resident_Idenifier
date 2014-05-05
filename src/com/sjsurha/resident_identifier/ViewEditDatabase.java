/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sjsurha.resident_identifier;

import com.sjsurha.resident_identifier.Exceptions.CEEncryptionErrorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.filechooser.FileNameExtensionFilter;

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
        
        JPanel adminButtonPanel = new JPanel();
        JPanel residentButtonPanel = new JPanel();
                
        addAdmin = new JButton("Add New Admin");
        addAdmin.addActionListener(addAdminActionListener());
        adminButtonPanel.add(addAdmin);
        
        removeAdmin = new JButton("Remove Admin");
        removeAdmin.addActionListener(removeAdminActionListener());
        adminButtonPanel.add(removeAdmin);
        
        mergeDatabase = new JButton("Merge with Existing Database");
        mergeDatabase.addActionListener(mergeActionListener());
        adminButtonPanel.add(mergeDatabase);

        importResidents = new JButton("Import Residents");
        importResidents.addActionListener(importActionListener());
        residentButtonPanel.add(importResidents);

        addResident = new JButton("Manually Add Resident");
        addResident.addActionListener(addResidentActionListener());
        residentButtonPanel.add(addResident);

        deleteResidents = new JButton("Delete All Residents");
        deleteResidents.addActionListener(deleteResidents());
        residentButtonPanel.add(deleteResidents);
        
        deleteEvents = new JButton("Delete All Events");
        deleteEvents.addActionListener(deleteEventsActionListener());
        residentButtonPanel.add(deleteEvents);
        
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        
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
        currentResCount.setText("Number of Events in database: " + model.eventCount() + "\nNumber of Admins in Database: " + model.adminCount() + "\nNumber of Residents in Database: " + model.residentCount());
    }

    private ActionListener importActionListener()
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
    
    private ActionListener deleteEventsActionListener()
    {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                model.emptyEventDatabase();
                updateText();
            }
        };
    }

    private ActionListener addAdminActionListener()
    {
        return new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                model.adminCreation();
                updateText();
            }
        };
    }
    
    private ActionListener removeAdminActionListener()
    {
        return new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                model.adminRemoval();
                updateText();
            }
        };
    }
    
    private ActionListener mergeActionListener() {
        //NOTE: JFILECHOOSER IS CAUSING THREAD EXCEPTIONS
        //Replication: Start program, click Merge button, close window and close program
        //EXCEPTION THROWN: "Exception while removing reference"
        //Exception does not appear to affect seal operation
        return new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                
                final JFileChooser fileChooserGUI = new JFileChooser(); //File Chooser window for user
                
                
                JCheckBox checkAdmins = new JCheckBox("Import Admins"), 
                        checkEvents = new JCheckBox("Import Events"), 
                        checkResidents = new JCheckBox("Import Residents");

                JPanel checkBoxesPanel = new JPanel();
                
                //restrict files to sealed model file types
                String fileType = ViewerController.SEALED_MODEL_FILE_NAME.substring(ViewerController.SEALED_MODEL_FILE_NAME.lastIndexOf('.')+1);
                fileChooserGUI.setFileFilter(new FileNameExtensionFilter("Sealed Model Files (.ser)", fileType));
                fileChooserGUI.setAcceptAllFileFilterUsed(false);
                
                //create JPanel to hold checkboxes
                checkBoxesPanel.add(checkAdmins);
                checkBoxesPanel.add(checkEvents);
                checkBoxesPanel.add(checkResidents);                
                
                //Open file chooser and check for invalid file 
                if(fileChooserGUI.showOpenDialog(null) != JFileChooser.APPROVE_OPTION 
                    || fileChooserGUI.getSelectedFile() == null 
                    || !fileChooserGUI.getSelectedFile().exists())
                { fileChooserGUI.approveSelection(); return; }

                try {
                    Model modelIn = ViewerController.unseal(fileChooserGUI.getSelectedFile(), ViewerController.initializeSealedObject(checkBoxesPanel));
                    model.mergeDatabase(modelIn, checkAdmins.isSelected(), checkEvents.isSelected(), checkResidents.isSelected());
                } catch (        CEEncryptionErrorException | FileNotFoundException ex) {
                    Logger.getLogger(ViewEditDatabase.class.getName()).log(Level.SEVERE, null, ex);
                    //Error Message Please
                } finally {
                    updateText();
                }
            }
        };
    }

    private ActionListener addResidentActionListener()
    {
        return new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                String ID = (String)JOptionPane.showInputDialog(null,"Please swipe Resident's Student ID card:", "Student ID",JOptionPane.QUESTION_MESSAGE);
                ID = ViewerController.extractID(ID);
                if(ID == null){
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

                model.addResident(ID, lastName, firstName, bedSpace);
                updateText();
            }
        };
    }
}
