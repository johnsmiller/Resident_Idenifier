/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sjsurha.resident_identifier;

import java.awt.HeadlessException;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 *
 * @author John
 */
public class CSV_Import {
    
    //CSV import variables
    private static File defaultPath; //DOESN'T IMPLEMENT SERIALIZABLE
    private static int ID_COLUMN_NUMBER = 0; //Location of SJSU User ID column in excel sheet rows
    private static int FIRST_NAME_COLUMN_NUMBER = 4; //Location of SJSU First Name column in excel sheet rows
    private static int LAST_NAME_COLUMN_NUMBER = 2; //Location of SJSU Last Name column in excel sheet rows
    private static int BEDSPACE_COLUMN_NUMBER = 17; //Location of SJSU Bedspace column in excel sheet rows
    private static boolean leadingZeroFix = true;
    private final static int ID_CELL_LENGTH = 9; //Length of SJSU User ID to locate valid student ID entries
    private final static int SAMPLE_COLUMNS = 30; //Number of columns to pull for user sample
    private final static int SAMPLE_ROWS = 30; //Number of rows to pull for user sample
    private final static int SKIP_ROW_COUNT = 14; //Number of cells to skip before pulling data for user sample
    private final static HashSet<String> INVALID_ID_STRINGS = new HashSet<>(Arrays.asList("CLOSED", "LAST NAME", "SJSU ID"));; //Strings that indicate the current row is not a resident
    
    private final static String[] messageStrings = {
            "Please select ONE cell that contains a user ID", 
            "Please select ONE cell that contains a LAST name",
            "Please select ONE cell that contains a FIRST name",
            "Please select ONE cell that contains a BedSpace (ex: CVA-000)"
        };
    
    
    public static HashSet<Resident> importCSV()
    {
        HashSet<Resident> ret = new HashSet<>();
        Integer[] values = new Integer[messageStrings.length];
        
        Scanner scanner = null;
        
        try {
            JFileChooser fileChooserGUI = new JFileChooser(); //File Chooser window for user
            String[] acceptedFileTypes = {"csv"}; //restricts files to .csv files
            fileChooserGUI.setFileFilter(new FileNameExtensionFilter(null, acceptedFileTypes)); //restrict file types
            
            JTable selectorTable; //Table used to display sample of rows/columns
            Object[][] tableData = new Object[SAMPLE_ROWS][]; //populated with non-empty rows/columns of sample sets
            Object[] tableHeaders; //Column headers (numbers 1 to # of columns)
           
            if(defaultPath != null && defaultPath.exists()) //set default fileChooser location to previous location if valid
                fileChooserGUI.setCurrentDirectory(defaultPath);
            
            if(fileChooserGUI.showOpenDialog(null) != JFileChooser.APPROVE_OPTION //Check for invalid file
                    || fileChooserGUI.getSelectedFile() == null 
                    || !fileChooserGUI.getSelectedFile().exists())
            { return null; }
            

            defaultPath = fileChooserGUI.getSelectedFile(); //update previous selection location
            
            //Get scanner instance
            scanner = new Scanner(fileChooserGUI.getSelectedFile());

            //Set the delimiter used in file
            //scanner.useDelimiter(",");
            for(int i = 0; (scanner.hasNextLine() && i<SAMPLE_ROWS); i++) //populates row & column sets
            {
                tableData[i] = scanner.nextLine().split(",");
            }
            
            if(tableData.length < 4)
            {
                //error message
                return null;
            }
            
            int numOfColumns = tableData[0].length;
            
            tableHeaders = new Object[numOfColumns]; //HAVE TO ASSUME ALL LINES ARE SAME LENGTH
            
            for(int i = 0; i<tableHeaders.length; i++) //create table headers
                tableHeaders[i] = i+1;
            
            selectorTable = new JTable(tableData, tableHeaders) { 
                @Override
                public boolean isCellEditable(int i, int j)
                {
                    return false;
                }
            };
            
            selectorTable.setPreferredScrollableViewportSize(ViewerController.JTablePopUpSize); //MAGIC NUMBERS
            selectorTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            selectorTable.setFillsViewportHeight(true);
            
            for(int i = 0; i < messageStrings.length; i++){
                int[] temp = ViewerController.jTableDialog(selectorTable, messageStrings[i]);
                if(temp == null || temp[1] > numOfColumns){
                    ViewerController.showTimedInfoDialog("Action Cancelled", "Import Cancelled or Invalid Selection", 3);
                    return null;
                }
                values[i] = temp[1];
            }
            ID_COLUMN_NUMBER = values[0];
            LAST_NAME_COLUMN_NUMBER = values[1];
            FIRST_NAME_COLUMN_NUMBER = values[2];
            BEDSPACE_COLUMN_NUMBER = values[3];           
            
            scanner.close();
            
            scanner = new Scanner(fileChooserGUI.getSelectedFile());

            String[] splitLineStrings;
            String ID;
            
            while(scanner.hasNextLine())
            {
                splitLineStrings = scanner.nextLine().split(",");
                
                if(splitLineStrings.length >= numOfColumns && !INVALID_ID_STRINGS.contains(splitLineStrings[ID_COLUMN_NUMBER])) {
                    //Leading '0' fix for SJSU. To be converted to an option under new program settings window
                    while(leadingZeroFix && splitLineStrings[ID_COLUMN_NUMBER].length()<ID_CELL_LENGTH)
                        splitLineStrings[ID_COLUMN_NUMBER] = "0" + splitLineStrings[ID_COLUMN_NUMBER];
                    //Store ID string for readability
                    ID = splitLineStrings[ID_COLUMN_NUMBER];
                    //Create new resident 
                    Resident resident = new Resident(ID, splitLineStrings[FIRST_NAME_COLUMN_NUMBER], splitLineStrings[LAST_NAME_COLUMN_NUMBER], splitLineStrings[BEDSPACE_COLUMN_NUMBER]);
                    //Save new resident
                    ret.add(resident);
                }
            }
            
        }   
        catch (HeadlessException | FileNotFoundException e) 
        {
            JOptionPane.showMessageDialog(null, "An Error Occured while attempting to import."
                    + "\nPlease check that this is a valid CSV file."
                    + "\nIt may help to use 'Save As' in Excel and create a new CSV file to attempt to import from", 
                    "Internal Error Occured", JOptionPane.ERROR_MESSAGE);
        }
        finally
        {
            if(scanner != null)
                scanner.close();
            return ret;
        }
    }
}
