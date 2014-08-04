package com.sjsurha.resident_identifier;

import java.io.Serializable;

public final class Resident implements Serializable{
    private static final long serialVersionUID = 1L;
    
    private String id;
    private String first_name;
    private String last_name;
    private String bedspace;
    
    /**
     *
     * @param ID
     * @param First_Name
     * @param Last_Name
     * @param BedSpace
     */
    public Resident(String ID, String First_Name, String Last_Name, String BedSpace)
    {
        id = ID;
        first_name = First_Name;
        last_name = Last_Name;
        bedspace = BedSpace;       
    }
    
    
    @Override
    public String toString()
    {
        return id + " " + first_name + " " + last_name + " " + bedspace;
    }
    //Student ID, Name, Building, Room number, list of events resident has attended
    //flag for user-enetered or edited instance?
    //list of events Resident has attended.
    //Mainly used to make sure student doesn't get duplicate tickets, etc for an event
    //Securely Storing this information?
    //Password protected run-time?
    /**
     *
     * @return
     */
    public String getId() {
        return id;
    }

    /**
     *
     * @param id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     *
     * @return
     */
    public String getName() {
        return first_name + " " + last_name;
    }

    public String getFirst_name() {
        return first_name;
    }

    public void setFirst_name(String first_name) {
        this.first_name = first_name;
    }

    public String getLast_name() {
        return last_name;
    }

    public void setLast_name(String last_name) {
        this.last_name = last_name;
    }

    /**
     *
     * @return
     */
    public String getBedspace() {
        return bedspace;
    }

    /**
     *
     * @param bedspace
     */
    public void setBedspace(String bedspace) {
        this.bedspace = bedspace;
    }
    
    
}
