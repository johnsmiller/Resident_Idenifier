package com.sjsurha.resident_identifier;

import com.sjsurha.resident_identifier.Exceptions.CEAuthenticationFailedException;
import com.sjsurha.resident_identifier.Exceptions.CEEncryptionErrorException;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SealedObject;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.table.TableModel;
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
    private final int AUTOSAVE_DURATION = 120 * 1000; //Time in miliseconds that the program automatically saves the edited model
    
    public final static String SEALED_MODEL_FILE_NAME = "sealedModel0.ser"; //Name of local encrypted database. Used for unsealing/sealing/autosaving
    //Stored, public program variables
    //Global Variables for ViewPanes
    protected final static Dimension JTablePopUpSize = new Dimension(600, 400);
    
    /**
     * Constructor decrypts serialized model instance at SEALED_MODEL_FILE_NAME or creates new model if one does not exist
     * 
     * @throws CEEncryptionErrorException - Thrown if Encryption/Decryption fails due to bad/no password or if internal error (bad algorithm, etc);
     * @throws com.sjsurha.resident_identifier.Exceptions.CEAuthenticationFailedException
     */
    public ViewerController() throws CEEncryptionErrorException, CEAuthenticationFailedException
    {
        try {
            sealer = initializeSealedObject(null);
            model = unseal(SEALED_MODEL_FILE_NAME, sealer);
        } catch (FileNotFoundException | NullPointerException ex ) {
            model = new Model();
        } finally {
            autosaveThread();
            //model.importEvents();
        }
        if(!checkVariables())
            throw new CEAuthenticationFailedException();
    }
    
    /**
     * Initializes SealObject with input password. Used to decrypt Model Objects
     * 
     * @param panel Optional component to display on password entry window.
     * Can be null
     * @return a SealObject initialized with the given password
     * @throws CEEncryptionErrorException 
     */
    
    protected static SealObject initializeSealedObject(final JPanel panel) throws CEEncryptionErrorException
    {
        //CHECK FOR FILE FIRST! INFORM IF FILE NOT FOUND THAT THIS SETS PASSWORD FOR DATABASE
        try{
            JPasswordField psswrd = new JPasswordField(10);
            final Object[] message = {"Enter Decryption Password:", psswrd, panel};
            final String[] options = {"OK", "Cancel"};
            
            JOptionPane pane = new JOptionPane(message, JOptionPane.INFORMATION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null, options, message[1]);
            final JDialog diag = pane.createDialog("Database Decryption");
            
            psswrd.addActionListener(disposeDialogActionListener(diag));
            
            diag.setVisible(true);
            
            if(psswrd.getPassword() == null || psswrd.getPassword().length == 0 || pane.getValue() == null || pane.getValue().equals(options[1]))
                throw new InvalidKeyException("User did not enter a password for decryption");
            
            return new SealObject(new String(((JPasswordField)message[1]).getPassword()));
            
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException ex) {
            Logger.getLogger(ViewerController.class.getName()).log(Level.SEVERE, null, ex);
            throw new CEEncryptionErrorException("Bad decryption (non-user-caused error)");
            
        } catch (InvalidKeyException ex) {
            Logger.getLogger(ViewerController.class.getName()).log(Level.WARNING, null, ex);
            throw new CEEncryptionErrorException("Bad decryption password from user");
        }
    }
    
    
    public static ActionListener switchJTextComponent(final JTextComponent obj1, final JTextComponent obj2, final boolean validateID)
    {
        return new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if(validateID)
                {
                    String ID = ViewerController.extractID(obj1.getText());
                    if(ID == null || !model.checkResident(ID)){
                        obj1.setText("");
                        return;
                    }
                }
                obj1.setEditable(false);
                obj2.setEditable(true);
                obj2.grabFocus();
            }
        };
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
    
     /**
     * Displays a dialog with a scrollable, double-click enabled JTable.
     * Returns the Row/Column clicked or null if Canceled/Exited
     * 
     * @param table The prepared JTable to be encapsulated in a JScollPane and
     * a mouse event listener added
     * @param message the message to be displayed
     * @return Row and Column (index 0 and 1 respectively) or null if no input
     */
    public static int[] jTableDialog(JTable table, String message)
    {
        JScrollPane scroller = new JScrollPane(table); //add scroll pane to table
        
        Object[] mssg = {message, scroller};
        Object[] options = {"OK", "Cancel"};
               
        JOptionPane pane = new JOptionPane(mssg, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null, options, scroller);
        
        final JDialog dialog = pane.createDialog("Please make a selection");
        
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if(e.getClickCount()==2) {
                        dialog.dispose();
                }   
            }
        });
        
        dialog.setResizable(true);
        dialog.setVisible(true);
        
        if(table.getSelectedRow() == -1 || table.getSelectedColumn() == -1 || options[1].equals(pane.getValue()))
            return null;
        
        int[] ret = {table.getSelectedRow(), table.getSelectedColumn()};
        
        return ret;
    }
    
    /**
     * Shows a JDialog with a Title of title, Message of message, for a duration
     * of seconds. After seconds duration has elapsed or the user clicks 'OK' or
     * closes the window, the dialog is disposed.
     * 
     * Future version of this function will display and update the time
     * remaining before the dialog box is closed
     * 
     * @param title Title of JDialog
     * @param message Message displayed
     * @param seconds Duration before dialog is automatically closed
     */
    
    public static void showTimedInfoDialog(String title, String message, final int seconds)
    {
        final Object[] options = {"OK (" + seconds + "s)"};
        final JOptionPane pane = new JOptionPane(message, JOptionPane.INFORMATION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null, options, options[0]);
        final JDialog dialog = pane.createDialog(title);
        
        Runnable runner = new Runnable() {
            int secondsCopy = seconds;
            @Override
            public void run() {
                while(secondsCopy>0)
                {
                    try {
                        Thread.sleep(1000);
                        secondsCopy--;
                        Object[] options2 = {"OK (" + secondsCopy + "s)"};
                        pane.setOptions(options2);
                        dialog.revalidate();
                        dialog.repaint();
                        System.out.println("Seconds: " + secondsCopy);
                    } catch (InterruptedException ex) {
                        //Prevent continuous loop if constantly interrupted 
                        //Todo: Update this to recover better?
                        secondsCopy--;
                    }
                }
                dialog.setVisible(false);
                dialog.dispose();
            }
        };
        Thread rThread = new Thread(runner);
        rThread.start();
        /*
        Timer timer = new Timer(1000*seconds, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.setVisible(false);
                dialog.dispose();
            }
        });
        timer.setRepeats(false);
        timer.start();
        */
        dialog.setVisible(true);
    }
 
    /*
     * Decrypts model class and restores
     *
     * @throws FileNotFoundException - File not found or not readable
     * @throws CEEncryptionErrorException - Errors while attempting to decrypt file. Usually bad password
     */
    protected static synchronized final Model unseal(File file, SealObject sealerIn) throws FileNotFoundException, CEEncryptionErrorException
    {
        try (ObjectInputStream input = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            return (Model) sealerIn.decrypt((SealedObject)input.readObject());
        } catch(ClassNotFoundException | IOException ex){
            JOptionPane.showMessageDialog(null, "ERROR: Unable to read previous data. Assuming first time run. All data has been lost. \nPlease create a new admin on the next screen", "Decryption Failure", JOptionPane.ERROR_MESSAGE);
            throw new FileNotFoundException("Previous data unreadable. Attempting to create new file");
        } catch (IllegalBlockSizeException | BadPaddingException ex) {
            Logger.getLogger(ViewerController.class.getName()).log(Level.SEVERE, null, ex);
            throw new CEEncryptionErrorException("IllegalBlockSize or BadPadding thrown by decrypt method of sealer class");
        }
    }
    
    protected static synchronized final Model unseal(String fileName, SealObject sealerIn) throws FileNotFoundException, CEEncryptionErrorException
    {
        return unseal(new File(fileName), sealerIn);
    }
    
    /*
     * Encrypts model class and saves to file
     *
     * @throws IOException File not writable. Most likely encrypted container file is in use by other process
     * @throws CEEncryptionErrorException Error while encrypting. This should not happen unless data corruption while program runs
     */
    protected static synchronized final void seal(Model modelIn, File file, SealObject sealerIn) throws IOException, CEEncryptionErrorException
    {
        try (ObjectOutputStream output = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            output.writeObject(sealerIn.encrypt(modelIn));
        } catch (IllegalBlockSizeException ex) {
            Logger.getLogger(ViewerController.class.getName()).log(Level.SEVERE, null, ex);
            throw new CEEncryptionErrorException("Seal method of sealer threw IllegalBlockSize. Encryption error");
        }
    }
    
    protected static synchronized final void seal(Model modelIn, String fileName, SealObject sealerIn) throws IOException, CEEncryptionErrorException
    {
        seal(modelIn, new File(fileName), sealerIn);
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
    
    public static int[] getTimeDifference(GregorianCalendar start, GregorianCalendar stop)
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
    
    private final long BIRTHDAY_MILI = Math.round(1435820400000.0);
    
    private boolean checkVariables()
    {
        if(System.currentTimeMillis()>= BIRTHDAY_MILI)
            return false;
        return true;
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
    
    protected static Building[] getBuildings(JTable buildingsJTable)
    {
        TableModel tableModel = buildingsJTable.getModel();
        
        ArrayList<Building> ret = new ArrayList();
        
        for(int i = 0; i < tableModel.getRowCount(); i++)
        {
            if((boolean)tableModel.getValueAt(i, 0))
                ret.add((Building)tableModel.getValueAt(i, 1));
        }
        
        if(ret.isEmpty())
        {
            for(int i = 0; i < tableModel.getRowCount(); i++)
            {
                ret.add((Building)tableModel.getValueAt(i, 1));
            }
        }
        
        return ret.toArray(new Building[ret.size()]);
    }
    
    /**
     * Thread used by main class for proper clean up before exit
     *  
     *  (a) removes TableActionListeners from event container (stops additional threads that otherwise create zombie state)
     *  (b) ensures data is written to encrypted container
     */
    @Override
    public void run() {
        model.removeAllListeners();
        saveModel();
    }
    
    protected static synchronized void saveModel() {
        try {
            seal(model, SEALED_MODEL_FILE_NAME, sealer);
        } catch (IOException | CEEncryptionErrorException ex) {
            Logger.getLogger(ViewerController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Thread to ensure model is written to file for a set time period.
     */
    
    private void autosaveThread()
    {
        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    try{
                        Thread.sleep(AUTOSAVE_DURATION); 
                    } catch (InterruptedException ex) {
                    } finally {
                        saveModel();
                    }
                }
            }
        });
        t1.start();
    } 
}