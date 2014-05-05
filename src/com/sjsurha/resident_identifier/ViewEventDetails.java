/*
 * 
 */
//Good first draft BUT...
//REWRITE FOR CODE READABILITY
package com.sjsurha.resident_identifier;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.table.TableModel;

/**
 *
 * @author John
 */
public final class ViewEventDetails extends JPanel{
    
    private final Model model;
    private final JComboBox eventCombobox;
    private final JTextPane eventDetail;
    private final JButton viewAttendees;
    private final JButton viewWaitlist;
    private final JButton addTickets;

    public ViewEventDetails(Model ModelIn)
    {
        super();
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        
        model = ModelIn;
        
        eventCombobox = model.getEventsJComboBox();
        eventCombobox.setPreferredSize(new Dimension(500,23));
        eventCombobox.addActionListener(Get_Display_Listener());
        this.add(eventCombobox);

        eventDetail = new JTextPane();
        eventDetail.setEditable(false);
        eventDetail.setPreferredSize(new Dimension(500,200));
        if(eventCombobox.getSelectedItem() != null)
            eventDetail.setText(((Event)eventCombobox.getSelectedItem()).Get_Details());
        this.add(eventDetail);

        viewAttendees = new JButton("View Attendees");
        viewAttendees.addActionListener(displayAttendeesActionListener());

        viewWaitlist = new JButton("View Waitlist");
        viewWaitlist.addActionListener(displayWaitinglistActionListener());

        addTickets = new JButton("Add Tickets");
        addTickets.addActionListener(addTicketsActionListener());

        JPanel view_buttons = new JPanel();
        view_buttons.add(viewAttendees);
        view_buttons.add(viewWaitlist);
        view_buttons.add(addTickets);
        this.add(view_buttons);
    }
    
    public ViewEventDetails(Model ModelIn, JComboBox EventJComboBox)
    {
        super();
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        
        model = ModelIn;

        eventCombobox = EventJComboBox;
        eventCombobox.addActionListener(Get_Display_Listener());

        eventDetail = new JTextPane();
        eventDetail.setEditable(false);
        eventDetail.setPreferredSize(new Dimension(500,200));
        if(eventCombobox.getSelectedItem() != null)
            eventDetail.setText(((Event)eventCombobox.getSelectedItem()).Get_Details());
        this.add(eventDetail);

        viewAttendees = new JButton("View Attendees");
        viewAttendees.addActionListener(displayAttendeesActionListener());

        viewWaitlist = new JButton("View Waitlist");
        viewWaitlist.addActionListener(displayWaitinglistActionListener());

        addTickets = new JButton("Add Tickets");
        addTickets.addActionListener(addTicketsActionListener());

        JPanel view_buttons = new JPanel();
        view_buttons.add(viewAttendees);
        view_buttons.add(viewWaitlist);
        view_buttons.add(addTickets);
        this.add(view_buttons);
    }

    @Override
    public void repaint()
    {
        if(eventCombobox != null && eventCombobox.getSelectedItem()!=null){
            String temp = ((Event)eventCombobox.getSelectedItem()).Get_Details();
            eventDetail.setText(temp);
        }
        super.repaint();
    }

    protected ActionListener Get_Display_Listener()
    {
        return new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if(eventCombobox.getSelectedItem() == null){
                    eventDetail.setText("");
                    return;
                }
                String temp = ((Event)eventCombobox.getSelectedItem()).Get_Details();
                eventDetail.setText(temp);
            }
        };
    }

    private ActionListener displayAttendeesActionListener() //Double clicking on attendee opens resident details panel
    {
        return new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if(eventCombobox.getSelectedItem()==null || !model.Admin_Authentication_Popup())
                    return;

                String[] columnNames = {"Check-in", "ID", "Last Name", "First Name", "Bedspace", "Tickets"};
                Event event = (Event) eventCombobox.getSelectedItem();                  

                final JTable attendeesTable = new JTable(model.getEventAttendeeJTable(event), columnNames);

                attendeesTable.setAutoCreateRowSorter(true);
                attendeesTable.setPreferredScrollableViewportSize(new Dimension(600, 400));
                ColumnsAutoSizer.sizeColumnsToFit(attendeesTable);
                attendeesTable.setFillsViewportHeight(true);

                final JButton secureAddTicketsButton = new JButton("Add tickets to selected resident");
                secureAddTicketsButton.addActionListener(secureAddTickets(attendeesTable, 1));

                final JButton saveAttendeesButton = new JButton("Save list to PDF");
                saveAttendeesButton.setEnabled(false);
                
                JPanel buttonPanel = new JPanel();
                
                buttonPanel.add(secureAddTicketsButton);
                buttonPanel.add(saveAttendeesButton);

                Object[] message = {new JScrollPane(attendeesTable), buttonPanel};
                JOptionPane.showConfirmDialog(null, message, "Event Attendee list", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
            }
        };
    }

    private ActionListener displayWaitinglistActionListener() //double clicking on attendee opens resident details panel
    {
        return new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if(eventCombobox.getSelectedItem()==null || !model.Admin_Authentication_Popup())
                    return;
                String[] columnNames = {"Check-in", "ID", "Last Name", "First Name", "Bedspace", "Tickets"};
                Event event = (Event) eventCombobox.getSelectedItem();

                final JTable waitinglistTable = new JTable(model.getEventWaitlistJTable(event), columnNames);
                waitinglistTable.setAutoCreateRowSorter(true);
                waitinglistTable.setPreferredScrollableViewportSize(new Dimension(600, 400));
                ColumnsAutoSizer.sizeColumnsToFit(waitinglistTable);
                waitinglistTable.setFillsViewportHeight(true);         

                final JButton secureAddTicketsButton = new JButton("Add tickets to selected resident");
                secureAddTicketsButton.addActionListener(secureAddTickets(waitinglistTable, 1));
                
                final JButton saveWaitingListButton = new JButton("Save list to PDF");
                saveWaitingListButton.setEnabled(false);
                
                JPanel buttonPanel = new JPanel();
                
                buttonPanel.add(secureAddTicketsButton);
                buttonPanel.add(saveWaitingListButton);

                Object[] message = {new JScrollPane(waitinglistTable), buttonPanel};
                JOptionPane.showConfirmDialog(null, message, "Event Waitinglist", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
            }
        };
    }

    private ActionListener addTicketsActionListener()
    {
        return new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                final JTextField ID_Textfield = new JTextField();
                final JTextField Increase_Field = new JTextField();
                
                Increase_Field.setEditable(false);
                
                Object[] message = {"Swipe ID: ", ID_Textfield, "Add tickets: ", Increase_Field};
                Object[] options = {"OK", "Cancel"};
                
                JOptionPane pane = new JOptionPane(message, JOptionPane.INFORMATION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null, options, ID_Textfield);
                JDialog diag = pane.createDialog("Add Tickets");
                
                ID_Textfield.addActionListener(verifyIDSwapEditable(ID_Textfield, Increase_Field));
                Increase_Field.addActionListener(ViewerController.disposeDialogActionListener(diag));
                
                diag.setVisible(true);
                
                if(Increase_Field.getText() == null || Increase_Field.getText().length() == 0 || pane.getValue() == null || pane.getValue().equals(options[1]))
                    return;
                
                try{
                    Integer Increase_Int = Integer.parseInt(Increase_Field.getText());
                    Event event = (Event) eventCombobox.getSelectedItem();
                    event.addTickets(ID_Textfield.getText(), Increase_Int);     
                } catch (NumberFormatException ex){
                    JOptionPane.showMessageDialog(null, "Error: improper input for Add Tickets. \nTickets not added.", "Add Tickets Error", JOptionPane.ERROR_MESSAGE);
                } catch (NullPointerException ex) {
                    JOptionPane.showMessageDialog(null, "Error: improper input for Add Tickets or invalid Event selected", "Add Tickets Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
    }
    
    private ActionListener verifyIDSwapEditable(final JTextField ID_Textfield, final JTextField otherTextField)
    {
        return (new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if(ID_Textfield.getText() == null || ViewerController.extractID(ID_Textfield.getText()) == null || !model.checkResident(ViewerController.extractID(ID_Textfield.getText()))){
                    ID_Textfield.setText("");
                    return;
                }
                ID_Textfield.setText(ViewerController.extractID(ID_Textfield.getText()));
                ID_Textfield.setEditable(false);
                otherTextField.setEditable(true);
                otherTextField.grabFocus();
            }
        });
    }

    private ActionListener secureAddTickets(final JTable mssage, final int idColumn)
    {
        return new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                JTextField increaseField = new JTextField();
                Object[] temp = { "Add tickets: ", increaseField}; //SET FOCUS TO increaseField
                if(mssage.getSelectedRow() != -1 && JOptionPane.showConfirmDialog(null, temp, "Add Tickets", JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION){
                    try{
                        TableModel model = mssage.getModel();
                        Integer Increase_Int = Integer.parseInt(increaseField.getText());
                        Event event = (Event) eventCombobox.getSelectedItem();
                        event.addTickets((String)model.getValueAt(mssage.getSelectedRow(), idColumn), Increase_Int);     
                    } catch (NumberFormatException ex){
                        JOptionPane.showMessageDialog(null, "Error: improper input for Add Tickets. \nTickets not added.", "Add Tickets Error", JOptionPane.ERROR_MESSAGE);
                    } catch (NullPointerException ex) {
                        JOptionPane.showMessageDialog(null, "Error: improper input for Add Tickets or invalid Event selected", "Add Tickets Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        };
    }
    
    @Override
    public void setBackground(Color color) {
        if(eventDetail != null)
            eventDetail.setBackground(color);
        super.setBackground(color); 
    }
}

