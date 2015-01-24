package com.sjsurha.resident_identifier;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyListener;
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
    
    private final JComboBox eventCombobox;
    private final JTextPane eventDetail;
    private final JTextPane eventBuildingDetail;
    private final JButton viewAttendees;
    private final JButton viewWaitlist;
    private final JButton addTickets;
    
    public ViewEventDetails(JComboBox EventJComboBox)
    {
        super();
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        eventCombobox = EventJComboBox;
        eventCombobox.addActionListener(Get_Display_Listener());

        eventDetail = new JTextPane();
        eventDetail.setEditable(false);
        eventDetail.setPreferredSize(new Dimension(500,200));
        //this.add(eventDetail);
        
        eventBuildingDetail = new JTextPane();
        eventBuildingDetail.setEditable(false);
        
        JPanel textJPanel = new JPanel(new BorderLayout());
        textJPanel.add(eventDetail, BorderLayout.CENTER);
        textJPanel.add(eventBuildingDetail, BorderLayout.EAST);
        this.add(textJPanel);

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
        
        repaint();
    }
    
    @Override
    public void addKeyListener(KeyListener l)
    {
        super.addKeyListener(l);
        for(Component c : this.getComponents())
            if(c != null){
                c.setFocusable(true);
                c.addKeyListener(l);
            }
    }

    @Override
    public void repaint()
    {
        if(eventCombobox != null && eventCombobox.getSelectedItem()!=null){
            String[] temp = ((Event)eventCombobox.getSelectedItem()).Get_Details();
            eventDetail.setText(temp[0]);
            eventBuildingDetail.setText(temp[1]);
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
                String[] temp = ((Event)eventCombobox.getSelectedItem()).Get_Details();
                eventDetail.setText(temp[0]);
                eventBuildingDetail.setText(temp[1]);
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
                
                if(event == null || !Model.getInstance().authenticationModule(LogEntry.Level.Administrator, "Event " + (attendeeTable? "Attendee Table" : "Waitinglist Table")  + " viewed: " + event.toString()))
                    return;                 

                final JTable checkInTable = (attendeeTable? event.getAttendeesJTable(Model.getInstance()) : event.getWaitlistJTable(Model.getInstance()));
                checkInTable.setAutoCreateRowSorter(true);
                checkInTable.setPreferredScrollableViewportSize(ViewerController.JTablePopUpSize);
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
                if(eventCombobox.getSelectedItem() != null){
                    Event event = (Event) eventCombobox.getSelectedItem();
                    event.ticketWindowPopup(null);
                }
            }
        };
    }

    private ActionListener secureAddTickets(final JTable mssage, final int idColumn)
    {
        return new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if(mssage.getSelectedRow() != -1 && eventCombobox.getSelectedItem() != null) {
                    Event event = (Event) eventCombobox.getSelectedItem();
                    if(event.ticketWindowPopup((String)mssage.getValueAt(mssage.getSelectedRow(), idColumn)))
                    {
                        //Refresh window
                        mssage.getModel().notifyAll();
                    }
                }
            }
        };
    }
    
    @Override
    public void setBackground(Color color) {
        if(eventDetail != null && eventBuildingDetail != null){
            eventDetail.setBackground(color);
            eventBuildingDetail.setBackground(color);
        }
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