package com.sjsurha.resident_identifier;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
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
import javax.swing.JTextPane;


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
        viewAttendees.addActionListener(displayCheckInActionListener(true));

        viewWaitlist = new JButton("View Waitlist");
        viewWaitlist.addActionListener(displayCheckInActionListener(false));

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

    private ActionListener displayCheckInActionListener(final Boolean attendeeTable) //Double clicking on attendee opens resident details panel
    {
        return new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                Event event = (Event) eventCombobox.getSelectedItem();    
                String[] columnNames = {"Check-in", "ID", "Last Name", "First Name", "Bedspace", "Tickets"};
                
                if(event == null || !model.authenticationPopup(LogEntry.Level.Administrator, "Event " + (attendeeTable? "Attendee Table" : "Waitinglist Table")  + " viewed: " + event.toString()))
                    return;                 

                final JTable checkInTable = new JTable(model.getEventJTable((attendeeTable? event.getAttendees() : event.getWaitinglist()), event.getTickets()), columnNames){ 
                    @Override 
                    public boolean isCellEditable(int row, int column){  
                        return false;
                    }
                };
                checkInTable.setAutoCreateRowSorter(true);
                checkInTable.setPreferredScrollableViewportSize(model.JTablePopUpSize);
                checkInTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
                checkInTable.setFillsViewportHeight(true);
                
                checkInTable.getRowSorter().toggleSortOrder(0);

                final JButton secureAddTicketsButton = new JButton("Add tickets to selected resident");
                secureAddTicketsButton.addActionListener(secureAddTickets(checkInTable, 1));

                final JButton saveAttendeesButton = new JButton("Save list to PDF");
                saveAttendeesButton.setEnabled(false);
                
                JPanel buttonPanel = new JPanel(new FlowLayout());
                
                buttonPanel.add(secureAddTicketsButton);
                buttonPanel.add(saveAttendeesButton);

                Object[] message = {new JScrollPane(checkInTable)};
                Object[] options = {"OK", secureAddTicketsButton, saveAttendeesButton};
                
                JOptionPane pane = new JOptionPane(message, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null, options);
                JDialog diag = pane.createDialog((attendeeTable? "Attendees" : "Waiting List"));
                diag.setResizable(true);
                diag.setVisible(true);
            }
        };
    }

    private ActionListener addTicketsActionListener()
    {
        return new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                Event event = (Event) eventCombobox.getSelectedItem();
                event.ticketWindowPopup(null);
            }
        };
    }

    private ActionListener secureAddTickets(final JTable mssage, final int idColumn)
    {
        return new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if(mssage.getSelectedRow() != -1) {
                    Event event = (Event) eventCombobox.getSelectedItem();
                    if(event.ticketWindowPopup((String)mssage.getModel().getValueAt(mssage.getSelectedRow(), idColumn)))
                    {
                        
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

/*

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
        viewAttendees.addActionListener(displayCheckInActionListener(true));

        viewWaitlist = new JButton("View Waitlist");
        viewWaitlist.addActionListener(displayCheckInActionListener(false));

        addTickets = new JButton("Add Tickets");
        addTickets.addActionListener(addTicketsActionListener());

        JPanel view_buttons = new JPanel();
        view_buttons.add(viewAttendees);
        view_buttons.add(viewWaitlist);
        view_buttons.add(addTickets);
        this.add(view_buttons);
    }
*/