/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sjsurha.resident_identifier;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import javax.swing.BoxLayout;
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
    private final Model model; //Perhaps move away from storing model for security reasons?
    private final JTextField idInput;
    private final JComboBox event_combobox;
    private final ViewEventDetails eventDetailsPane;

    private final JTextPane messagePane;

    public ViewSignIn(Model ModelIn)
    {
        super();
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
                
        model = ModelIn;
        
        idInput = new JTextField();                                    
        idInput.setPreferredSize(new Dimension(200,24));//preferred size
        idInput.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        idInput.addActionListener(submitActionListener());//submit on enter, Issue with no focus when program first runs
        this.add(idInput);

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
        final Component parentComponent = this;
        return new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if(idInput.getText().length()<9){
                        setBackground(Color.YELLOW);
                        setText("Card read error. Please try again");
                        return;
                    }
                    if(event_combobox.getSelectedItem()==null){
                        JOptionPane.showMessageDialog(null, "Please select an event.", "Sign-in error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    String id = ViewerController.extractID(idInput.getText());
                    Event event = (Event)event_combobox.getSelectedItem();
                    if(model.addAttendee(id, event)){
                        setBackground(Color.GREEN);
                        setText("Resident Sign-in Successful");
                    }
                } catch (CEDuplicateAttendeeException ex) {
                    setBackground(Color.RED);
                    setText("Error: Resident has already signed in for this event");
                } catch (CENonResidentException ex) {
                    setBackground(Color.RED);
                    setText("Error: Resident not found. This could be a database error. \nTry typing in the ID manually\nAdmin users can manually add a resident after verifying they are residents");
                } catch (CEMaximumAttendeesException ex) {
                    setBackground(Color.YELLOW);
                    setText("There was an error adding this resident. Please try again.");
                } finally {
                    idInput.setText("");
                    
                    JTextComponent[] textUpdate = {messagePane};
                    Component[] colorUpdate = {parentComponent};
                    ViewerController.setRunnableColorTextUpdate(textUpdate, colorUpdate, "Swipe ID to Check-in", Color.WHITE, 2000);
                }
            }
        };
    }

}

