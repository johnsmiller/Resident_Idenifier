/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sjsurha.resident_identifier;

import com.sjsurha.resident_identifier.Exceptions.CEDuplicateAttendeeException;
import com.sjsurha.resident_identifier.Exceptions.CEMaximumAttendeesException;
import com.sjsurha.resident_identifier.Exceptions.CENonResidentException;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.JTextComponent;

/**
 *
 * @author John
 */
public final class ViewSignIn extends JPanel
{
    private final long resetInterval = 2 * 1000;
    
    private final Model model; //Perhaps move away from storing model for security reasons?
    private final JTextField idInput;
    private final JComboBox event_combobox;
    private final ViewEventDetails eventDetailsPane;
    private final JCheckBox suppressRecheckinPrompt;

    private final JTextPane messagePane;
    
    private static Long resetThreadID =(long) 0;

    public ViewSignIn(Model ModelIn)
    {
        super();
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
                
        model = ModelIn;

        idInput = new JTextField();                                    
        idInput.setPreferredSize(new Dimension(400,24));//preferred size
        idInput.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        idInput.addActionListener(submitActionListener());//submit on enter, Issue with no focus when program first runs
        
        suppressRecheckinPrompt = new JCheckBox("Supress Re-checkin Prompt");
        
        final JPanel checkinPane = new JPanel();
        checkinPane.add(idInput);
        checkinPane.add(suppressRecheckinPrompt);
        this.add(checkinPane);

        event_combobox = model.getEventsJComboBox();
        event_combobox.setPreferredSize(new Dimension(500,23));
        if(event_combobox.getItemCount()>0)
            event_combobox.setSelectedIndex(0);
        this.add(event_combobox);

        messagePane = new JTextPane();
        messagePane.setEditable(false);
        messagePane.setText("Select event and swipe card");
        messagePane.setFont(new Font(Font.DIALOG, Font.BOLD, 24));
        this.add(messagePane);
        
        eventDetailsPane = new ViewEventDetails(ModelIn, event_combobox);
        idInput.addActionListener(eventDetailsPane.Get_Display_Listener());
        this.add(eventDetailsPane);    
                
    }
    
    @Override
    public void setBackground(Color color)
    {
        super.setBackground(color);
        if(messagePane != null)
            messagePane.setBackground(color);
        if(eventDetailsPane != null)
            eventDetailsPane.setBackground(color);
    }

    @Override
    public void repaint()
    {
        if(idInput!=null)
            idInput.requestFocusInWindow();
        if(messagePane != null)
            messagePane.repaint();
        if(eventDetailsPane != null)
            eventDetailsPane.repaint();
        super.repaint();
    }
    
    private void setText(String text)
    {
        messagePane.setText(text);
        messagePane.repaint();
    }

    private ActionListener submitActionListener()
    {
        return new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if(idInput.getText().length()<9){
                        setDisplay("Card read error. Please try again", Color.YELLOW);
                        return;
                    }
                    
                    if(event_combobox.getSelectedItem()==null){
                        JOptionPane.showMessageDialog(null, "Please select an event.", "Sign-in error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    
                    String id = ViewerController.extractID(idInput.getText());
                    Event event = (Event)event_combobox.getSelectedItem();
                    
                    if(model.addAttendee(id, event, suppressRecheckinPrompt.isSelected())){
                        setDisplay("Resident Sign-in Successful", Color.GREEN);
                    }
                } catch (CEDuplicateAttendeeException ex) {
                    setDisplay("Error: Resident has already signed in for this event", Color.RED);
                } catch (CENonResidentException ex) {
                    setDisplay("Error: Resident not found. This could be a database error. \nTry typing in the ID manually\nAdmin users can manually add a resident after verifying they are residents", Color.RED);
                } catch (CEMaximumAttendeesException ex) {
                    setDisplay("There was an error adding this resident. Please try again.", Color.YELLOW);
                } finally {
                    idInput.setText("");
                    reset();
                }
            }
        };
    }
    
    private void setDisplay(String text, Color color)
    {
        setText(text);
        setBackground(color);
        repaint();
    }
    
    private void reset()
    {
        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                Long threadIDCopy;
                synchronized(resetThreadID) {
                    resetThreadID = System.nanoTime();
                    threadIDCopy = resetThreadID;
                }
                
                try {   Thread.sleep(resetInterval);  } 
                catch (InterruptedException ex) {} 
                finally {
                    if(!threadIDCopy.equals(resetThreadID)) //Check if another thread has launched since reset was scheduled
                        return;
                    
                    setText("Swipe ID to Check-in");
                    setBackground(Color.WHITE);
                    repaint();
                }
            }
        });
        
        t1.start();
    }
}

//public static 

