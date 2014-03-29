/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sjsurha.resident_identifier;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.TreeSet;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextPane;

/**
 *
 * @author John
 */
public final class ViewResidentDetails extends JPanel
{
    private final Model model;
    private final JTextPane textArea;
    private final JButton checkResident;

    public ViewResidentDetails(Model ModelIn)
    {
        super();
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        
        model = ModelIn;

        textArea = new JTextPane();
        textArea.setEditable(false);

        JPanel buttonPanel = new JPanel();

        checkResident = new JButton("Check if Resident");
        checkResident.addActionListener(checkRes());
        buttonPanel.add(checkResident);

        this.add(textArea);
        this.add(buttonPanel);
    }

    private ActionListener checkRes()
    {
        return new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                String ID = (String)JOptionPane.showInputDialog(null,"Please swipe your ID card:", "Swipe ID to Check",JOptionPane.QUESTION_MESSAGE);
                if((ID=ViewerController.extractID(ID))==null){
                    textArea.setText("Invalid Card");
                    return;
                }
                if(model.checkResident(ID)){
                    TreeSet<Event> attended = model.getAttendedEvents(ID);
                    if(attended.size() == 0)
                        textArea.setText("Is resident\nNo event records");
                    else {
                        String message = "Is resident. Attended following events:";
                        for(Event ev : attended)
                            message += "\n\t" + ev.getName() + " " + ev.getLongDate() + " " + ev.getTime();
                        textArea.setText(message);
                    }
                    
                    
                }
                else
                    textArea.setText("Is NOT resident");
            }
        };
    }

}
