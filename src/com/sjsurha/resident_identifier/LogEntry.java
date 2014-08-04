/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sjsurha.resident_identifier;

import java.io.Serializable;
import java.util.GregorianCalendar;

/**
 *
 * @author John
 */
public final class LogEntry implements Comparable<LogEntry>, Serializable{
    private GregorianCalendar time;
    private String id;
    private String message;
    private Result result;
    private Level level;
    
    public LogEntry(GregorianCalendar Time, String ID, String Message, Result Res, Level Levl)
    {
        time = Time;
        id = ID;
        message = Message;
        result = Res;
        level = Levl;
    }
    
    public LogEntry(String ID, String Message, Result Res, Level Levl)
    {
        time = new GregorianCalendar();
        id = ID;
        message = Message;
        result = Res;
        level = Levl;
    }
    
    public enum Result
    {
        Success, Failure;
    }
    
    public enum Level
    {
        Administrator, User;
    }

    public GregorianCalendar getTime() {
        return time;
    }

    public String getID() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public Result getResult() {
        return result;
    }

    public Level getLevel() {
        return level;
    }
    
    @Override
    public int compareTo(LogEntry l) 
    {
        if(l.getTime().compareTo(time) == 0)
        {
            if(id.compareTo(l.getID()) == 0)
            {
                if(message.compareTo(l.getMessage()) == 0)
                    return result.compareTo(l.getResult());
                return message.compareTo(l.getMessage());
            }
            return id.compareTo(l.getID());
        }
        return l.getTime().compareTo(time);
    }
    
}
