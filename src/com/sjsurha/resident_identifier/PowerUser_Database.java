/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sjsurha.resident_identifier;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.TreeSet;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/**
 *
 * @author John
 */
public class PowerUser_Database implements Serializable{
    private static final long serialVersionUID = 1;
    
    //last user request?
        //issue with restore from file?? (can't use static?)
    private final HashMap<String, String> admins;
    private final HashMap<String, String> users;
    
    private final TreeSet<LogEntry> log;
    
    private final int MAX_CONSECUTIVE_FAILED_ATTEMPTS = 5; //Number of failed Admin or User Authenication attempts before program resets
    private Integer consecutive_failed_attempts;
    
     //Security function variables (static members reset upon model restore)
    private Integer user_requests;
    private static Long last_user_request;
    private final double TIME_DIFFERENCE_TOLERANCE = 15.0*1000000000; //time (in nanoseconds) that is valid to have USER_REQUEST_TOLERANCE requests within
    private final int USER_REQUEST_TOLERANCE = 10;
    private final long ADMIN_VERIFICATION_TOLERANCE = 30*1000000000; //time (in nanoseconds) that a verified admin can access admin functions without admin authentication
    private static Long last_admin_request; //time (in nanoseconds) of last admin authentication
    
    public PowerUser_Database()
    {
        admins = new HashMap<>();
        users = new HashMap<>();
        log = new TreeSet<>();
        consecutive_failed_attempts = 0;
        
        user_requests = 0;
        last_user_request = System.nanoTime();
        last_admin_request = (long)-1;
    }
    
    public PowerUser_Database(HashMap<String, String> adminResc, HashMap<String, String> userResc, TreeSet<LogEntry> logResc)
    {
        this();
        if(adminResc != null)
            admins.putAll(adminResc);
        if(userResc != null)
            users.putAll(userResc);
        if(logResc != null)
            log.addAll(logResc);
    }
    
    static
    {
        last_user_request = System.nanoTime();
        last_admin_request = (long) -1;
    }
    
    
    protected boolean powerUserRemovalPopup(LogEntry.Level level)
    {
        boolean isAdminRemoval = level.equals(LogEntry.Level.Administrator);
                
        final JTextField ID_TextField = new JTextField();
        final JCheckBox confirmRemoval = new JCheckBox("Permantly Remove " + level.toString() + "?");
        
        String title = level.toString() + " Removal";
        
        Object[] message = {"Enter " + level.toString() + " to be removed", ID_TextField, confirmRemoval};
        Object[] options = {"OK", "Cancel"};
        
        JOptionPane pane = new JOptionPane(message, JOptionPane.INFORMATION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null, options, ID_TextField);
        JDialog diag = pane.createDialog(title);
        
        diag.setVisible(true);
        
        String ID = ID_TextField.getText();
        
        if(pane.getValue() == null || pane.getValue().equals(options[1]))
        {
            return false;
        }
        else if(!confirmRemoval.isSelected())
        {
            JOptionPane.showMessageDialog(null, "Error: Please confirm " + level.toString() + " removal", level.toString() + " Removal Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        else if(isAdminRemoval && admins.size() < 2)
        {
            JOptionPane.showMessageDialog(null, "Error: Database must have at least 1 Administrator", level.toString() + " Removal Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        else if(!authenticationPopup(LogEntry.Level.Administrator, "Remove " + level.toString() + " ID: " + ID))
        {
            return false;
        }
        else if((isAdminRemoval && (admins.remove(ID) == null)) || (!isAdminRemoval && (users.remove(ID) == null)))
        {
            JOptionPane.showMessageDialog(null, "Error: " + level.toString() + " not found", level.toString() + " Removal Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        JOptionPane.showMessageDialog(null, level.toString() + " Removed Successfully", level.toString() + " Removed", JOptionPane.INFORMATION_MESSAGE);
        return true;        
    }
    
    protected boolean powerUserCreationPopup(LogEntry.Level level)
    {
        final JTextField ID_TextField_1 = new JTextField();
        final JTextField ID_TextField_2 = new JTextField();
        final JPasswordField Pin_PassField_1 = new JPasswordField();
        final JPasswordField Pin_PassField_2 = new JPasswordField();
        
        String title = level.toString() + " Creation";
        
        Object[] ID1_Line = {"Scan ID: ", ID_TextField_1};
        Object[] ID2_Line = {"Rescan ID: ", ID_TextField_2};
        Object[] Pin1_Line = {"Create Pin: ", Pin_PassField_1};
        Object[] Pin2_Line = {"(4 digits or more)"};
        Object[] Pin3_Line = {"Retype Pin: ", Pin_PassField_2};
        
        Object[] message = {ID1_Line, ID2_Line, Pin1_Line, Pin2_Line, Pin3_Line};
        Object[] options = {"OK", "Cancel"};

        JOptionPane pane = new JOptionPane(message, JOptionPane.INFORMATION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null, options, ID1_Line);
        JDialog diag = pane.createDialog(title);
        
        ID_TextField_1.addActionListener(ViewerController.switchJTextComponent(ID_TextField_1, ID_TextField_2, false));
        ID_TextField_2.addActionListener(ViewerController.switchJTextComponent(ID_TextField_2, Pin_PassField_1, false));
        Pin_PassField_1.addActionListener(ViewerController.switchJTextComponent(Pin_PassField_1, Pin_PassField_2, false));
        Pin_PassField_2.addActionListener(ViewerController.disposeDialogActionListener(diag));
        
        diag.setVisible(true);
        
        String ID1 = ID_TextField_1.getText();
        String ID2 = ID_TextField_2.getText();
        String Pass1 = SealObject.encryptPass(new String(Pin_PassField_1.getPassword()));
        String Pass2 = SealObject.encryptPass(new String(Pin_PassField_2.getPassword()));
        
        if(pane.getValue() == null || pane.getValue().equals(options[1]))
        {
            return false;
        }
        else if(ID1 == null || ID1.length() <= 0 || Pass1.length() < 4 || !ID1.equals(ID2) || !Pass1.equals(Pass2))
        {
            JOptionPane.showMessageDialog(null, "Error: Invalid Pin or ID", "Creation Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        else if(admins.size() > 0 && !authenticationPopup(LogEntry.Level.Administrator, "Create new " + level.toString()))
        {
            return false;
        }
        else if(level.equals(LogEntry.Level.Administrator))
        {
            admins.put(ID1, Pass1);
            ViewerController.saveModel();
            return true;
        }
        else
        {
            users.put(ID1, Pass1);
            ViewerController.saveModel();
            return true;
        }
       
    }
    
    protected boolean authenticationPopup(LogEntry.Level level, String descp)
    {
        boolean isAdminAuth = level.equals(LogEntry.Level.Administrator);
        
        final JTextField ID_Textfield = new JTextField();
        final JPasswordField Pin_Passfield = new JPasswordField();
        
        String title = level.toString() + " Authentication";
        
        Object[] ID_Line = {"Enter ID: ", ID_Textfield};
        Object[] Pin_Line = {"Enter Pin: ", Pin_Passfield};
        
        Object[] message = {ID_Line, Pin_Line};
        Object[] options = {"OK", "Cancel"};

        JOptionPane pane = new JOptionPane(message, JOptionPane.INFORMATION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null, options, ID_Textfield);
        JDialog diag = pane.createDialog(title);

        ID_Textfield.addActionListener(ViewerController.switchJTextComponent(ID_Textfield, Pin_Passfield, false));
        Pin_Passfield.addActionListener(ViewerController.disposeDialogActionListener(diag));
        
        diag.setVisible(true);
        
        String ID = ID_Textfield.getText();
        char[] Pin = Pin_Passfield.getPassword();

        if(pane.getValue() == null || pane.getValue().equals(options[1]))
        {
            //Canceled
            String logID = (ID == null)? "Null" : ID;
            synchronized(log) {log.add(new LogEntry(logID, descp, LogEntry.Result.Failure, level));}
            return false;
        }
        else if(ID == null || Pin == null)
        {
            String logID = (ID == null)? "Null" : ID;
            synchronized(log) {log.add(new LogEntry(logID, descp, LogEntry.Result.Failure, level));}
            JOptionPane.showMessageDialog(null, "Error: Invalid Pin or ID", "Authentication Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        else if(admins.containsKey(ID) && SealObject.passCheck(new String(Pin), admins.get(ID))) //Admin can access both user & admin functions
        {
            synchronized(log) {log.add(new LogEntry(ID, descp, LogEntry.Result.Success, level));}
            synchronized(last_admin_request) {  last_admin_request = System.nanoTime(); }
            synchronized(consecutive_failed_attempts) { consecutive_failed_attempts = 0;} 
            return true;
        }
        
        else if(!isAdminAuth && users.containsKey(ID) && SealObject.passCheck(new String(Pin), users.get(ID))) //Users can only access user functions
        {
            synchronized(log) {log.add(new LogEntry(ID, descp, LogEntry.Result.Success, level));}
            synchronized(last_user_request) {  last_user_request = System.nanoTime(); }
            synchronized(consecutive_failed_attempts) { consecutive_failed_attempts = 0;    }
            return true;
        }
        
        JOptionPane.showMessageDialog(null, "Error: Invalid Pin or ID", "Authentication Error", JOptionPane.ERROR_MESSAGE);
        
        synchronized(log) {log.add(new LogEntry(ID, descp, LogEntry.Result.Failure, level));}
        synchronized(consecutive_failed_attempts) { consecutive_failed_attempts++;  }
        
        if (consecutive_failed_attempts >= MAX_CONSECUTIVE_FAILED_ATTEMPTS) {
            synchronized(log) {log.add(new LogEntry("SYSTEM", "SYSTEM SHUTDOWN RESULTED FROM: " + descp, LogEntry.Result.Failure, level));};
            synchronized(consecutive_failed_attempts) { consecutive_failed_attempts = 0;    } //Since the user will be forced to enter database password when restarting program
            System.exit(0);
        }
        
        return false;
    }
    
    protected String[][] getLogData()
    {
        //String[] ColumnHeaders = {"Date", "User", "Level", "Result", "Message"};
        String[][] logData = new String[log.size()][5];
        SimpleDateFormat sdf1 = new SimpleDateFormat();
        sdf1.applyPattern("yyyy\\MM\\dd HH:mm:ss EEEE");
        
        int i = 0;
        for(LogEntry l : log)
        {
            logData[i][0] = sdf1.format(l.getTime().getTime());
            logData[i][1] = l.getID();
            logData[i][2] = l.getLevel().toString();
            logData[i][3] = l.getResult().toString();
            logData[i][4] = l.getMessage();
            i++;
        }
        
        return logData;
    }
    
    protected void importArchivedLog(PowerUser_Database oldPowerUsers)
    {
        for(LogEntry l : oldPowerUsers.log)
        {
            l.archived();
        }
        
        synchronized(log) {log.addAll(oldPowerUsers.log);}
    }
    
    protected synchronized void importAdmins(PowerUser_Database oldPowerUsers)
    {
        //This ensures all current admins' pins are kept.
        HashMap<String, String> temp = (HashMap<String, String>) oldPowerUsers.admins.clone();
        synchronized(admins) {
            temp.putAll(admins); //Add/override all old admins with current admins
            admins.putAll(temp);//Every current Admin is overridden with identical data & all old admins are added
        } 
    }
    
    protected void importUsers(PowerUser_Database oldPowerUsers)
    {
        //This ensures all current admins' pins are kept.
        HashMap<String, String> temp = (HashMap<String, String>) oldPowerUsers.users.clone();
        synchronized(users) {
            temp.putAll(users); //Add/override all old admins with current admins
            users.putAll(temp);//Every current Admin is overridden with identical data & all old admins are added
        } 
    }
    
    public class Authentication_Return
    {
        private final LogEntry.Level level;
        private final boolean authenticationResult;
        private final LogEntry log;
        public Authentication_Return(boolean authenticationRes, LogEntry lg)
        {
            level = lg.getLevel();
            authenticationResult = authenticationRes;
            log = lg;
        }

        public LogEntry.Level getLevel() {
            return level;
        }

        public boolean isAuthenticated() {
            return authenticationResult;
        }

        public LogEntry getLog() {
            return log;
        }
        
    }
    
    protected int getPowerUserCount(LogEntry.Level level)
    {
        if(level.equals(LogEntry.Level.Administrator))
            return admins.size();
        else if(level.equals(LogEntry.Level.User))
            return users.size();
        else
            return 0;
    }
    
}
