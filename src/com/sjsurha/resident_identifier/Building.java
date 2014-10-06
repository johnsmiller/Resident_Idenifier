package com.sjsurha.resident_identifier;

import java.io.Serializable;

public final class Building implements Comparable<Building>, Serializable, TreeSet_TableModel_ComboBoxModel.TableModel_ComboModel_Interface
{
    private static final long serialVersionUID = 1;
    
    private String name;

    public Building(String Name)
    {
        name = Name;
    }

    @Override
    public Object getColumn(int i) {
        if(i == 1)
            return this;
        return null;
    }

    @Override
    public String toString()
    {
        return name;
    }

    @Override
    public int compareTo(Building o) {
        return name.compareToIgnoreCase(o.name);
    }

    @Override
    public boolean equals(Object o)
    {
        if(o == null)
            return false;

        try {
            Building b = (Building) o;
            return name.equalsIgnoreCase(b.name);
        } catch (NullPointerException ex)
        {
            return false;
        }
    }

    @Override
    public int hashCode()
    {
        return name.hashCode();
    }
    
    public String getName()
    {
        return name;
    }
    
    public void setName(String str)
    {
        name = str;
    }

}