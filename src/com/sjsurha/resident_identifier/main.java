package com.sjsurha.resident_identifier;

import com.sjsurha.resident_identifier.Exceptions.CEAuthenticationFailedException;
import com.sjsurha.resident_identifier.Exceptions.CEEncryptionErrorException;
import java.awt.Dimension;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JOptionPane;


/**
 * @author John Miller
 * @version 1.0 - Initial Distribution
 */
public class main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) 
    {
        Dimension frameSize = new Dimension(550, 300);
        ViewerController temp;
        try {
            temp = new ViewerController();
            Runtime.getRuntime().addShutdownHook(new Thread(temp));
            JFrame main_frame = new JFrame();
            main_frame.add(temp.Get_JTabbedFrame());
            main_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            main_frame.setMinimumSize(frameSize);
            main_frame.setLocationRelativeTo(null);
            main_frame.pack();
            main_frame.setVisible(true);
        } catch (CEEncryptionErrorException ex) {
            Logger.getLogger(ViewerController.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(null, "ERROR: Decryption has failed. Most likely caused by an invalid password. Please try again. Program will now exit.", "Decryption Failure", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        } catch (CEAuthenticationFailedException ex) {
            Logger.getLogger(main.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(null, "ERROR: Database was not initialized with an Admin. Please try again. Program will now exit.", "Database Failure", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
    }
}


/**
 * ----------------------
 * Current Issue Tracker:
 *  - Sensitive Information is visible to admin users. 
 * 
 * 
 *  - In ViewEditDatabase, JFileChooser in mergeActionListener is causing thread exceptions under certain conditions
 *      Status: Benign / Java Source COde Error
 *      Conditions: Start program, click merge database, exit window, exit program
 *      Exception message: Exception while removing reference
 *      Implications: Does not appear to affect seal function. No other indications of bad behavior
 *      Notes: Research indicates this is a thread bug in JFileChooser class
 * 
 *  
 * 
 * -------------------------
 * Planned Updates:
 *  - Node communication via internet
 *      - Most likely, a server will hold encrypted messages that all currently running nodes will download
 * 
 *  - Categorized raffle drawings (put tickets towards specific prizes) 
 *  - Non-resident check-in?
 *  - Signature pad for prize sheets? 
 * 
 * -------------------------
 * Proposed solution
 * - Android app that connects to this program
 *      - Touch-screen signature capture for prizes
 *      - Let residents choose categories without holding up check-in
 * 
 * -------------------------
 * Project Octagon
 *  Features
 *      Opportunity Drawing Categories
 *          i. Considerations:
 *              - Edit Event Pane
 *              - Create Event Pane
 *              - Android App? (thread safe)
 *              - Event Details Pane
 *  
 *      Building Specific Checkin
 *          - Stats by Building (Event details pane) (Recalculation costly, only when Edit event pane is used, else increment on check-in)
 *          - Remember Previous Selection?
 *          - New Error Message & Handling ("In database but not in selected building")
 *          - Make buildings HashSet implement TableModel. Protect from edits outside model.
 * 
 *      Variable Adjustment Screen
 *          Include: 
 *              Import: sample rows, ID length, programmable building name delimiter/location/length, leading/trailing Zero fixes
 *              Student ID checker adjustments. (make ViewerController.ExtractID a programmable feature)
 *          Consider:
 *              Thread safe & sync'd operations
 *              Bounds & limits
 *              Reconsider early programming assumptions
 * 
 *  Updates:
 *      Print important JTables (PDF? CSV?)
 *      
 *      Implement Resident Details Pane Properly
 * 
 *      Encryption:
 *          Memory Encryption & Security Manager
 *          New Database password restrictions
 *              - min length
 *              - chars & numbers?
 * 
 *      ID Attack Detection
 *          Implement properly.
 */
