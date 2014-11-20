package com.sjsurha.resident_identifier;

import com.sjsurha.resident_identifier.Exceptions.CEDuplicateAttendeeException;
import com.sjsurha.resident_identifier.Exceptions.CEMaximumAttendeesException;
import com.sjsurha.resident_identifier.Exceptions.CENonResidentException;
import static com.sjsurha.resident_identifier.Exceptions.CENonResidentException.CEUnpermittedBuildingException;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTextPane;

/**
 *
 * @author John
 */
public final class ViewSignIn extends JPanel
{
    private final long RESET_INTERVAL_DEFAULT_VALUE = 2 * 1000;
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
        suppressRecheckinPrompt.addKeyListener(getKeyListener());
        
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
        eventDetailsPane.addKeyListener(getKeyListener());
        this.add(eventDetailsPane);    
        
        //add KeyListener that puts keytyped events into the idInput text box and key-pressed events that are equivalent to return (enter) to submit
        for(Component c : this.getComponents())
            if(c != null){
                c.setFocusable(true);
                c.addKeyListener(getKeyListener());
            }
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
                String id = null;
                long reset_interval = RESET_INTERVAL_DEFAULT_VALUE;
                try {
                    if(event_combobox.getSelectedItem()==null){
                        JOptionPane.showMessageDialog(null, "Please select an event.", "Sign-in error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    
                    id = ViewerController.extractID(idInput.getText());
                    
                    if(id==null){
                        setDisplay("Card read error. Please try again", Color.YELLOW);
                        return;
                    }
                    
                    Event event = (Event)event_combobox.getSelectedItem();
                    
                    if(event.validAttendee(id, model, suppressRecheckinPrompt.isSelected())){
                        setDisplay(model.extractBuilding(id) + " Resident Sign-in Successful", Color.GREEN);
                    }
                } catch (CEDuplicateAttendeeException ex) { //Why not just extract Exception mssg and save myself all these different exception types?
                    setDisplay("Error: Resident has already signed in for this event", Color.RED); //Black text on red BG hard to see
                } catch (CENonResidentException ex) {
                    setDisplay("Error: Resident not found. This could be a database error. \nTry typing in the ID manually", Color.RED);
                    reset_interval = 3 * 1000;
                } catch (CEMaximumAttendeesException ex) {
                    setDisplay("There was an error adding this resident. Please try again.", Color.YELLOW);
                } catch (CEUnpermittedBuildingException ex) {
                    setDisplay(model.extractBuilding(id) + " Residents not allowed to check into this event", Color.YELLOW);
                    reset_interval = 3 * 1000;
                } finally {
                    idInput.setText("");
                    reset(reset_interval);
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
    
    private void reset(final long resetInterval)
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
                    if(threadIDCopy.equals(resetThreadID)) {
                        setDisplay("Swipe ID to Check-in", Color.WHITE);
                    }
                }
            }
        });
        t1.start();
    }
    
    private KeyListener getKeyListener()
    {
        return new KeyListener(){

            @Override
            public void keyTyped(KeyEvent e) {
                String str = idInput.getText();
                if((int)e.getKeyChar() == 8)
                {
                    if(str.length()>0)
                        idInput.setText(str.substring(0, str.length()-1));
                }
                else if((int)e.getKeyChar() == 10) //Line Feed detection
                    return;
                else
                    idInput.setText(idInput.getText() + e.getKeyChar());
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if(e.getKeyCode() == KeyEvent.VK_ENTER)
                    for(ActionListener l : idInput.getActionListeners())
                        l.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ""));
            }

            @Override
            public void keyReleased(KeyEvent e) {
                //do nothing
            }
        };
    }
}