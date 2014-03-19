package com.sjsurha.resident_identifier;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SealedObject;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTabbedPane;
import javax.swing.text.JTextComponent;

/**
 * ViewerController builds GUI interface and maintains interaction between User and Model/Sealer classes.
 * Responsible for Encrypting and Decrypting between uses. Autosaves new content to encrypted container every 2 minutes as well as on normal exit
 * @author John Miller with Michael Daniels
 * @version .7 - Development/Initial Release
 */
public final class ViewerController implements Runnable{
    private static Model model; //Model that maintains all events, residents, and has all functions to edit them
    private static SealObject sealer; //Object containing current session's password & encryption methods
    private final static String curFile = "sealedModel0.ser"; //Name of local encrypted database. Used for unsealing/sealing/autosaving
    private final int autosaveDuration = 120 * 1000; //Time in miliseconds that the program automatically saves the edited model
        
    /**
     * Constructor decrypts serialized model instance at curFile or creates new model if one does not exist
     * 
     * @throws CEEncryptionErrorException - Thrown if Encryption/Decryption fails due to bad/no password or if internal error (bad algorithm, etc);
     */
    public ViewerController() throws CEEncryptionErrorException, CEAuthenticationFailedException
    {
        try {
            initialize();
            unseal();
        } catch (FileNotFoundException ex) {
                model = new Model();
        } finally {
            autosaveThread();
            //model.importEvents();
        }
    }
    
    /**
     * Initializes SealObject with input password. 
     * If previous encrypted container exists, exits program for invalid password, sets container's password if it does not exist
     * @throws CEEncryptionErrorException 
     */
    
    private void initialize() throws CEEncryptionErrorException
    {
        try{
            JPasswordField psswrd = new JPasswordField(10);
            final Object[] message = {"Enter Decryption Password:", psswrd};
            final String[] options = {"OK", "Cancel"};
            JOptionPane pane = new JOptionPane(message, JOptionPane.INFORMATION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null, options, message[1]);
            final JDialog diag = pane.createDialog("Database Decryption");
            psswrd.addActionListener(disposeDialogActionListener(diag));
            diag.setVisible(true);
            if(psswrd.getPassword() == null || psswrd.getPassword().length == 0 || pane.getValue() == null || pane.getValue() == JOptionPane.CLOSED_OPTION || pane.getValue().equals(options[1]))
                throw new InvalidKeyException("User did not enter a password for decryption");
            
            sealer = new SealObject(new String(((JPasswordField)message[1]).getPassword()));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException ex) {
            Logger.getLogger(ViewerController.class.getName()).log(Level.SEVERE, null, ex);
            throw new CEEncryptionErrorException("Bad decryption (non-user-caused error)");
        } catch (InvalidKeyException ex) {
            Logger.getLogger(ViewerController.class.getName()).log(Level.WARNING, null, ex);
            throw new CEEncryptionErrorException("Bad decryption password from user");
        }
    }
    
    public static ActionListener disposeDialogActionListener(final JDialog diag)
    {       
        return new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                diag.dispose();
            }
        };
    }
            
    /*
     * Decrypts model class and restores
     *
     * @throws FileNotFoundException - File not found or not readable
     * @throws CEEncryptionErrorException - Errors while attempting to decrypt file. Usually bad password
     */
    private synchronized void unseal() throws FileNotFoundException, CEEncryptionErrorException
    {
        try{
          ObjectInputStream input = new ObjectInputStream(new BufferedInputStream(new FileInputStream(curFile)));
          model = sealer.decrypt((SealedObject)input.readObject());
          input.close();
          return;
        }
        catch(ClassNotFoundException | IOException ex){
            JOptionPane.showMessageDialog(null, "ERROR: Unable to read previous data. Assuming first time run. All data has been lost. \nPlease create a new admin on the next screen", "Decryption Failure", JOptionPane.ERROR_MESSAGE);
            throw new FileNotFoundException("Previous data unreadable. Attempting to create new file");
        } catch (IllegalBlockSizeException | BadPaddingException ex) {
            Logger.getLogger(ViewerController.class.getName()).log(Level.SEVERE, null, ex);
            throw new CEEncryptionErrorException("IllegalBlockSize or BadPadding thrown by decrypt method of sealer class");
        }
    }
    
    /*
     * Encrypts model class and saves to file
     *
     * @throws IOException File not writable. Most likely encrypted container file is in use by other process
     * @throws CEEncryptionErrorException Error while encrypting. This should not happen unless data corruption while program runs
     */
    private synchronized final void seal() throws IOException, CEEncryptionErrorException
    {
        try {
            ObjectOutputStream output = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(curFile)));
            output.writeObject(sealer.encrypt(model));
            output.close();
            return;
        } catch (IllegalBlockSizeException ex) {
            Logger.getLogger(ViewerController.class.getName()).log(Level.SEVERE, null, ex);
            throw new CEEncryptionErrorException("Seal method of sealer threw IllegalBlockSize. Encryption error");
        }
    }
            
    /**
     * Used to standardize extracting student IDs an ID from an input string
     * 
     * @param ID - the input string
     * @return - the extracted ID or null if no student ID extracted
     */
    
    protected static String extractID(String ID)
    {
        if(ID == null)
            return null;
        else if(ID.length() == 9)
            return ID;
        else if(ID.length()==13)
            return ID.substring(1, 10);
        else if(ID.length()>14)
            return ID.substring(6, 15);
        else
            return null;
    }
    
    /**
     * Builds JTabbedPane for JFrame (Main GUI for user interaction)
     * 
     * @return a JTabbedFrame built by the View---.java JFrame classes
     */
    
    protected JTabbedPane Get_JTabbedFrame()
    {
        JTabbedPane tab = new JTabbedPane();

        tab.addTab("Create Event", new ViewCreateEvent(model));
        tab.addTab("Sign In", new ViewSignIn(model)); //ID field does not have focus on initial start-up. Also does not get focus when JComboBox Selection (fixable?)
        tab.addTab("Tickets", new ViewTickets(model));
        tab.addTab("Edit Event", new ViewEditEvent(model));
        tab.addTab("Resident Details", new ViewResidentDetails(model));
        tab.addTab("Edit Database", new ViewEditDatabase(model));
        tab.setSelectedIndex(1);
        tab.setVisible(true);
        return tab;
    }
    
    /**
     * Standardized time selection JComboBoxes
     * @return JComboBox array, index 0 is hours, index 1 is minutes (5 min intervals from 0 to 55)
     */
    
    protected static JComboBox[] Get_Time_Selector()
    {
        String[] hours = {"12 AM", "1 AM", "2 AM", "3 AM", "4 AM", "5 AM", "6 AM", "7 AM", "8 AM", "9 AM", "10 AM", 
            "11 AM", "12 PM", "1 PM", "2 PM", "3 PM", "4 PM", "5 PM", "6 PM", "7 PM", "8 PM", "9 PM", "10 PM", "11 PM"};
        String[] minutes = {"00", "05", "10", "15", "20", "25", "30", "35", "40", "45", "50", "55"};
        JComboBox[] ret = {new JComboBox(hours),new JComboBox(minutes)};
        return ret;
    }
    
    /**
     * Launches new thread and updates following fields as described once delay has elapsed
     * 
     * @param textField array of JTextComponents. Updated with text after milisecDuration miliseconds
     * @param colorField array of Components who's setBackground() method is called with color after milisecDuration
     * @param text string to update JTextComponents with
     * @param color color to update Components with
     * @param milisecDuration delay before updating fields
     */
           
    protected static void setRunnableColorTextUpdate(final JTextComponent[] textField, final Component[] colorField, final String text, final Color color, final long milisecDuration)
    {
        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {   Thread.sleep(milisecDuration);  } 
                catch (InterruptedException ex) {} 
                finally {
                    for(JTextComponent j : textField){
                        j.setText(text);
                        j.repaint();
                    }
                    for(Component c :colorField){
                        c.setBackground(color);
                        c.repaint();
                    }
                }
            }
        });
        
        t1.start();
    }
    
    /**
     * Thread used by main class for proper clean up before exit
     *  
     *  (a) removes TableActionListeners from event container (stops additional threads that otherwise create zombie state)
     *  (b) ensures data is written to encrypted container
     */
    @Override
    public void run() {
        try {
            model.removeAllEventListeners();
            seal();
        } catch (IOException | CEEncryptionErrorException ex) {
            Logger.getLogger(ViewerController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Thread to ensure model is written to file for a set time period.
     * 
     * ISSUE: Is causing a HashMap (admins or residents) in model to throw 
     * concurrent modification exception
     */
    
    private void autosaveThread()
    {
        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    try{
                        Thread.sleep(autosaveDuration); 
                    } catch (InterruptedException ex) {
                    } finally {
                        try {   
                            seal(); 
                        } 
                        catch (IOException | CEEncryptionErrorException ex) {}
                    }
                }
            }
        });
        t1.start();
    } 
}

/*
 * --------Old notes---------
 //JFrame - Main - (Consider requiring reauthentication if mouse/keyboard idle for more than 60 seconds for (admin) JPanels)
        //Jpanel Create Event (admin)
        //JPanel Sign-In (Default?)*
        //JPanel Edit Event (edit/delete) (admin) * 
        //JPanel Event Details (admin) *
        //JPanel Resident Details (Future dev?) (admin)
        //JPanel Resident Management (admin)
            //Import
            //Create/Delete?
    //JFrame - *EventList ->provide event selection for current view
        //Sign-in - (change of event requires authentication)
        //All other views require admin authentication
 * 
 * ------TO DO-----

//Views:
    //Help button (Future devel?)
       //Opens new window with help topics (Java standardization exists?)
    //Create Event (admin) -- done
        //Dropdown calendar for date selection?  --done
        //Drop downs for hour/min selections?   --done
    //Edit Event 
        //Resident Sign-in  --(remove signed-in resident) --done
        //Change Event (admin) 
        //Delete Event? (admin) --done
    //Print Event Details (admin)
    //Print Events a Resident has attended?? (Admin) (future dev?)
        //Print attended events
            //(Differentiate between card swipe and manual entry
            //Bring up print event details for a selected event (future dev)
    //Student Management (admin)
        //Import New resident records
        //Add/Delete Residents?????

    //Choose Event - separate pane that shows up for:
        //Resident Sign-in (admin authentication to change event)
        //All other Edit event items
        //Print Event
         
         
*/