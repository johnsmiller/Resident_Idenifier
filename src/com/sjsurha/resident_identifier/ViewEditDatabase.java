package com.sjsurha.resident_identifier;

import com.sjsurha.resident_identifier.Exceptions.CEEncryptionErrorException;
import java.awt.BorderLayout;
import java.awt.Dimension;
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
import javax.swing.JScrollPane;
import javax.swing.JTable;
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
    private final JPanel adminAndUserPanel;
    private final JPanel databasePanel;
    private final JPanel loggingPanel;
            
    private final JButton addAdmin;
    private final JButton removeAdmin;
    private final JButton addUser;
    private final JButton removeUser;
    private final JButton resetPin;
    private final JButton importResidents;
    private final JButton addResident;
    private final JButton deleteResidents; //DELETE ALL RESIDENTS BUTTON
    private final JButton deleteEvents;  
    private final JButton editVariables;
    private final JButton mergeDatabase;  
    private final JButton viewLog;
    //private final JButton clearLog;
    
    public ViewEditDatabase(Model ModelIn)
    {
        super();
        model = ModelIn;
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        
        addAdmin = new JButton("Add New Admin");
        removeAdmin = new JButton("Remove Admin");
        addUser = new JButton("Add New User");
        removeUser = new JButton("Remove User");
        resetPin = new JButton("Reset Pin");
        importResidents = new JButton("Import Residents");
        addResident = new JButton("Manually Add Resident");
        deleteResidents = new JButton("Delete All Residents");
        deleteEvents = new JButton("Delete All Events");
        editVariables = new JButton("Edit Database Varibles");
        mergeDatabase = new JButton("Merge with Existing Database");
        viewLog = new JButton("View Log Entries");
        //clearLog = new JButton("Clear Log");
        
        addAdmin.addActionListener(addAdminActionListener());       
        removeAdmin.addActionListener(removeAdminActionListener());
        addUser.addActionListener(addUserActionListener());
        removeUser.addActionListener(removeUserActionListener());
        resetPin.addActionListener(resetPinActionListener());
        importResidents.addActionListener(importActionListener());
        addResident.addActionListener(addResidentActionListener());        
        deleteResidents.addActionListener(deleteResidents());
        deleteEvents.addActionListener(deleteEventsActionListener());
        editVariables.addActionListener(editVariablesActionListener());
        mergeDatabase.addActionListener(mergeActionListener());
        viewLog.addActionListener(viewLogActionListener());
        
//        buttonPanel = new JPanel();
//        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        
        currentResCount = new JTextPane();
        currentResCount.setEditable(false);
        updateText();
        
        adminAndUserPanel = new JPanel();
        databasePanel = new JPanel();
        loggingPanel = new JPanel();
        
        adminAndUserPanel.add(addAdmin);
        adminAndUserPanel.add(removeAdmin);
        adminAndUserPanel.add(addUser);
        adminAndUserPanel.add(removeUser);
        adminAndUserPanel.add(resetPin);
        databasePanel.add(importResidents);
        databasePanel.add(addResident);
        databasePanel.add(deleteResidents);
        databasePanel.add(deleteEvents);
        loggingPanel.add(mergeDatabase); //Yeah, I know. But I need this for visual purposes. Open to suggestions for a better layout scheme
        loggingPanel.add(viewLog);
        loggingPanel.add(editVariables);
        //loggingPanel.add(clearLog);
        
        resetPin.setEnabled(false);
        editVariables.setEnabled(false);
        
        this.add(currentResCount);
        this.add(adminAndUserPanel);
        this.add(databasePanel);
        this.add(loggingPanel);
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
        currentResCount.setText("Number of Events in database: " + model.eventCount() + "\nNumber of Admins in Database: " + model.powerUserCount(LogEntry.Level.Administrator) + "\nNumber of Users in Database: " + model.powerUserCount(LogEntry.Level.User) + "\nNumber of Residents in Database: " + model.residentCount() + "\nNumber of Buildings in Database: " + model.buildingCount());
    }

    private ActionListener importActionListener()
    {
        return new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if(model.authenticationModule(LogEntry.Level.Administrator, "Import Residents via CSV file")){
                    model.csvImport();
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
                model.powerUserCreation(LogEntry.Level.Administrator);
                updateText();
            }
        };
    }
    
    private ActionListener removeAdminActionListener()
    {
        return new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                model.powerUserRemoval(LogEntry.Level.Administrator);
                updateText();
            }
        };
    }
    
    private ActionListener addUserActionListener()
    {
        return new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                model.powerUserCreation(LogEntry.Level.User);
                updateText();
            }
        };
    }
    
    private ActionListener removeUserActionListener()
    {
        return new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                model.powerUserRemoval(LogEntry.Level.User);
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
                        checkUsers = new JCheckBox("Import Users"),
                        checkEvents = new JCheckBox("Import Events"), 
                        checkResidents = new JCheckBox("Import Residents");

                JPanel checkBoxesPanel = new JPanel();
                
                //restrict files to sealed model file types
                String fileType = ViewerController.SEALED_MODEL_FILE_NAME.substring(ViewerController.SEALED_MODEL_FILE_NAME.lastIndexOf('.')+1);
                fileChooserGUI.setFileFilter(new FileNameExtensionFilter("Sealed Model Files (.ser)", fileType));
                fileChooserGUI.setAcceptAllFileFilterUsed(false);
                
                //create JPanel to hold checkboxes
                checkBoxesPanel.add(checkAdmins);
                checkBoxesPanel.add(checkUsers);
                checkBoxesPanel.add(checkEvents);
                checkBoxesPanel.add(checkResidents);                
                
                //Open file chooser and check for invalid file 
                if(fileChooserGUI.showOpenDialog(null) != JFileChooser.APPROVE_OPTION 
                    || fileChooserGUI.getSelectedFile() == null 
                    || !fileChooserGUI.getSelectedFile().exists())
                { fileChooserGUI.approveSelection(); return; }

                try {
                    Model modelIn = ViewerController.unseal(fileChooserGUI.getSelectedFile(), ViewerController.initializeSealedObject(checkBoxesPanel));
                    model.mergeDatabase(modelIn, checkAdmins.isSelected(), checkUsers.isSelected(), checkEvents.isSelected(), checkResidents.isSelected());
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

    private ActionListener viewLogActionListener() {
        return new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if(!model.authenticationModule(LogEntry.Level.Administrator, "View Log"))
                    return;
                
                String[] ColumnHeaders = {"Date (Year\\Month\\Day Time)", "User", "Level", "Result", "Message",};
                String[][] RowData = model.getLogData();
                
                JTable logTable = new JTable(RowData, ColumnHeaders){ 
                    @Override 
                    public boolean isCellEditable(int row, int column){  
                        return false;
                    }
                };
                
                logTable.setAutoCreateRowSorter(true);
                logTable.setPreferredScrollableViewportSize(new Dimension(800, 600));
                logTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
                logTable.setLayout(new BorderLayout());
                logTable.getRowSorter().toggleSortOrder(0);
                logTable.getRowSorter().toggleSortOrder(0);
                JOptionPane.showConfirmDialog(null, new JScrollPane(logTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), "Log Entries", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
            }
        };
    }

    private ActionListener resetPinActionListener() {
        return new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        };
    }

    private ActionListener editVariablesActionListener() {
        return new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                
            }
        };
    }
}
