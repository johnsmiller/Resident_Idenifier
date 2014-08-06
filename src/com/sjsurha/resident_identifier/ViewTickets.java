/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sjsurha.resident_identifier;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author John
 */
public final class ViewTickets extends JPanel
{
    private final Model model;
    private final Event_Selection_Pane eventPane;
    private Resident_Selection_Pane residentPane;

    public ViewTickets(Model ModelIn)
    {
        super();
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        
        model = ModelIn;

        eventPane = new Event_Selection_Pane();
        residentPane = new Resident_Selection_Pane();

        this.add(eventPane);
        this.add(residentPane);

    }

    private void changeView()
    {
        eventPane.setVisible(!eventPane.isVisible());
        residentPane.setVisible(!eventPane.isVisible());
    }

    private void rebuildResidentPane(Random rand, ArrayList<String> tickets) //Pretty poor programming. Try to fix
    {
        this.remove(residentPane);
        residentPane = new Resident_Selection_Pane();
        this.add(residentPane);
        residentPane.setRandom(rand);
        residentPane.setTickets(tickets);
    }

    private class Event_Selection_Pane extends JPanel
    {
        private final JTable eventList;
        private final JCheckBox includeCheckins;
        private final JCheckBox includeWaitlist;
        private final JButton submit;
        private Object[] eventArr;

        public Event_Selection_Pane()
        {
            super();
            this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            this.setVisible(true);

            eventList = getMyTable();

            includeCheckins = new JCheckBox("Include tickets for signing into an event", true);
            includeWaitlist = new JCheckBox("Include tickets for waitlisting for an event", true);
            submit = new JButton("Submit");

            submit.addActionListener(submitActionListener());

            this.add(new JScrollPane(eventList));
            this.add(includeCheckins);
            this.add(includeWaitlist);
            this.add(submit);
        }

        private JTable getMyTable()
        {
            JTable ret = model.getEventsJTable();
            ret.setAutoCreateRowSorter(true);
            ret.setPreferredScrollableViewportSize(new Dimension(500, 200));
            ret.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
            ret.setFillsViewportHeight(true);
            return ret;
        }

        private ActionListener submitActionListener()
        {
            return new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    ArrayList<String> selectedTickets = new ArrayList<>();
                    for(int i = 0; i<eventList.getRowCount(); i++)
                    {
                        if((boolean)eventList.getValueAt(i, 0))
                            selectedTickets.addAll(((Model.Event)eventList.getModel().getValueAt(i, 5)).getTickets(includeCheckins.isSelected(), includeWaitlist.isSelected()));
                    }                     
                    if(selectedTickets.size()>0 && model.authenticationPopup(LogEntry.Level.User, "Entered Opportunity Drawing Mode")){
                        Random rand = new Random();
                        randomizeArr(selectedTickets, rand);
                        rebuildResidentPane(rand, selectedTickets);
                        changeView();
                    }
                }
            };
        }

        private void randomizeArr(ArrayList<String> arr, Random rand)
        {
            for(int i = 0; i<arr.size()-1; i++){
                String rem = arr.remove(i);
                arr.add(rand.nextInt(arr.size()), rem);
            }
        }
    }

    private class Resident_Selection_Pane extends JPanel
    {
        private Random rand;
        private ArrayList<String> tickets;
        private ArrayList<String> winners;


        private final MyTable winnerHistory;
        private final JButton prevView;
        private final JButton randomTicket;
        private final JButton viewWinners;
        private final JButton saveWinnerHistory;
        private final ButtonGroup group;
        private final JRadioButton removeAll;
        private final JRadioButton removeOne;
        private final JRadioButton removeNone;

        public Resident_Selection_Pane()
        {
            super();
            this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            this.setVisible(false);

            Object[] headers = {"Prize", "Name", "Room", "Cell Phone Number", "Mailbox Number", "Event Name", "Signature"};

            winners = new ArrayList<>();

            winnerHistory = new MyTable(new Object[0][headers.length], headers);
            prevView = new JButton("Discard & return to previous view");
            randomTicket = new JButton("Draw Random Ticket");
            viewWinners = new JButton("View full details");
            saveWinnerHistory = new JButton("Save");
            removeAll = new JRadioButton("Remove all entries of this student. Will never be drawn again (Current Session only)");
            removeOne = new JRadioButton("Remove one entry of this student. Less change of being drawn again (Current Session only");
            removeNone = new JRadioButton("Remove no entries of this student. Same chance to be drawn again (Current Session only)");

            winnerHistory.getModel().addTableModelListener(getTableModelListener());
            prevView.addActionListener(prevViewActionListener());
            randomTicket.addActionListener(drawRandomActionListener());
            viewWinners.addActionListener(viewWinnersActionListener());
            saveWinnerHistory.addActionListener(saveWinnersActionListener());
            removeAll.setSelected(true);
            removeOne.setSelected(false);
            removeNone.setSelected(false);

            group = new ButtonGroup();
            group.add(removeAll);
            group.add(removeOne);
            group.add(removeNone);

            JPanel buttons = new JPanel();
            buttons.add(prevView);
            buttons.add(randomTicket);
            buttons.add(viewWinners);
            buttons.add(saveWinnerHistory);

            winnerHistory.setRowHeight(40);
            winnerHistory.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
            
            saveWinnerHistory.setEnabled(false); //Create way to export to pdf, csv
            //Signature capture?

            this.add(new JScrollPane(winnerHistory));
            this.add(buttons);
        }

        public void setRandom(Random Rand)
        {
            rand = Rand;
        }

        public void setTickets(ArrayList<String> Tickets)
        {
            tickets = Tickets;
        }

        private ActionListener prevViewActionListener()
        {
            return new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    changeView();
                }
            };
        }
        
        //final JButton get_tickets;
            //opens new window where user can select multiple events to pull tickets from
            //option to include check-ins as tickets
            //Print relevent stats
                //JTable from high to low of residents ticket amounts
                //Resort for check-in only amounts
            //Select & remove random resident (for prizes)
            //Select & keep random resident (for??)
        //Move to model to get rid of unsecured 'model.getNameBedspace' function
        private ActionListener drawRandomActionListener()
        {
            return new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    //Display JDialog
                    String ID = ((tickets.size()>0)? tickets.get(rand.nextInt(tickets.size())) : null);
                    String[] choosen = ((ID != null)? model.getNameBedspace(ID) : null);
                    if(choosen == null)
                    {
                        JOptionPane.showMessageDialog(null, "No tickets found or invalid resident choosen. Please try again.", "Pick ticket error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    Object[] message = {"Winner selected: " + choosen[1] + " " + choosen[0] + " lives in " + choosen[2].substring(0, 3)
                                + "\nPress ok to accept or cancel to discard", removeAll, removeOne, removeNone};
                    if(JOptionPane.showConfirmDialog(null, message, "Random Ticket Drawn", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
                        return;
                    else{
                        DefaultTableModel model = (DefaultTableModel)winnerHistory.getModel();
                        Object[] newRow = {"", choosen[1] + " " + choosen[0], choosen[2].substring(0, 3), "", "", "", ""};
                        model.addRow(newRow);
                        winnerHistory.repaint();
                        winners.add(ID);

                        if(removeAll.getModel() == group.getSelection())
                            while(tickets.contains(ID)){tickets.remove(ID);}
                        else if(removeOne.getModel() == group.getSelection())
                            tickets.remove(ID);
                    }
                }
            };
        }

        private ActionListener viewWinnersActionListener() //make printable??
        {
            return new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    if(model.authenticationPopup(LogEntry.Level.Administrator, "View Opportunity Drawing Winners' Full Details")){
                        String text = "";
                        Iterator<String> itr = winners.iterator();
                        while(itr.hasNext())
                        {
                            String id = itr.next();
                            String[] temp = model.getNameBedspace(id);
                            text += id + " " + temp[1] + " " + temp[0] + " " + temp[2] + "\n";
                        }
                        JTextPane message = new JTextPane();
                        message.setText(text);
                        message.setEditable(false);
                        JOptionPane.showMessageDialog(null, message, "Winners", JOptionPane.INFORMATION_MESSAGE);
                    }
                }

            };
        }

        private ActionListener saveWinnersActionListener()
        {
            return new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    winnerHistory.save();
                }
            };
        }

        private TableModelListener getTableModelListener()
        {
            return new TableModelListener() {

                @Override
                public void tableChanged(TableModelEvent e) {
                    winnerHistory.repaint();
                }
            };
        }

        private class MyTable extends JTable
        {
            public MyTable(Object[][] data, Object[] headers)
            {
                super(new DefaultTableModel(data, headers));
            }

            public MyTable()
            {
                super(new DefaultTableModel());
            }
            
            /**
             * THIS FUNCTION AND ALL OTHER USES OF ITEXTPDF TO BE REMOVED IN FUTURE VERSIONS
             * Research into alternative approaches. If I can do it, it'd be better
             */
            public void save()
            {
                
            }

                @Override
                public void repaint()
                {
                    super.repaint();
                }

                @Override
                public Class getColumnClass(int c) {
                    return getValueAt(0, c).getClass();
                }

                @Override
                public boolean isCellEditable(int row, int col) {
                    if (col == 1 || col == 2) {
                        return false;
                    } else {
                        return true;
                    }
                }
        }
    }    
}       
