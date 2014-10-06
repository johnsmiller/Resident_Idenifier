/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sjsurha.resident_identifier;

import java.util.ArrayList;
import java.util.HashSet;


/**
 *
 * @author John
 */
public class StringExtractors {
    /**
     * Interface to define basic functions across different types of string
     * extractor classes.
     *  -sub-classes beyond 1st layer?
     * 
     * toString() Function of implementing classes modified based on type of 
     * extraction, but children toString function called with leading \t added.
     */
    public interface CustomStringExtractor {
        /**
         * Shows customized dialog to set up this extractor's parameters
         * @return true if this extractor was set up successfully, false otherwise
         */
        public boolean showSetUpDialog();

        /**
         * Extracts string based on implementation-based parameters. 
         * Returns null if extraction failed. 
         * 
         * Null returns should be expected, especially if there are multiple 
         * types of inputs used for reading. 
         * 
         * Input string should not modified by this function. --No duh, strings in Java don't get modified, just copied
         * 
         * Input string may be null.
         * 
         * @param inputString the string to apply this extractor's rules to and 
         * attempt to return a valid string. 
         * @return the extracted string or null if this function's rules failed.
         */
        public String extractString(String inputString);
        
        /**
         * Allows String Extractors to be nested, making layered rules for 
         * strings possible. 
         * 
         * Note to self: AND/OR actions?
         *  Self to note: SubExtractor == AND, Peer Extractor (same sub level) == OR
         * 
         * Behavior requirements:
         * 
         * If all Sub-CustomStringExtractor return null, the parent will return
         * null. 
         * 
         * If there are multiple SubStringExtractors, the first to return
         * a result != null is given back to the parent. Order is determined 
         * initially by insertion order (first to be added is first to be called)
         * 
         * The parent's extraction function is called first, then the child's
         * function is called on the parent function's result.
         * 
         * @param cse CustomStringExtractor to add as child
         */
        public void addChildExtractor(CustomStringExtractor cse);
        
        /**
         * Returns the private arraylist containing the children extractors
         * @return the children extractors
         */
        public ArrayList<CustomStringExtractor> getChildrenExtractors();
        
        /**
         * removes the nth child in the CustomStringExtractor ArrayList
         * @param index the child at index (starting at 0) to be removed
         */
        public void removeChildExtractor(int index);
        
        /**
         * Returns human-friendly name of extractor
         * 
         * @return custom name set by user
         */
        public String getExtractorName();
        
        /**
         * Sets this extractor's human-friendly name.
         * 
         * @param name string to set this extractor's name to.
         */
        public void setExtractorName(String name);
        
        
    }
    
    /**
     * nthOccurrence possible behaviors
     *  - Remove all text up to specified string
     *      - including/excluding the string itself
     *  - Remove all text after a specified string
     *      - including/excluding the string itself
     *  - No modification
     *      - Used only to detect if a string contains another string. Most 
     *          likely to be paired with sub-children
     *  - Behavior when string doesn't exist
     *      - Return null. Include instructions that "" == all strings.
     *  - Behavior with leading & trailing spaces?
     *      - research/test & note on setup screen
     *  - Attempting to remove more occurrences than exist in a string will result 
     *      in a null return
     * Detection options: 
     *      - first
     *      - nth
     *      - all
     *      - last
     */
    
    public class nthOccurrence implements CustomStringExtractor
    {
        private boolean removeTextBefore;
        private boolean removeTextAfter;
        private boolean removeOccurance;
        
        private boolean detectFirst;
        private boolean detectLast;
        private boolean detectAll;
        private int detectNth; //If greater than 0, treated as true
        
        private String occ;
        private String name;
        
        private ArrayList<CustomStringExtractor> children;
        
        public nthOccurrence(String Name)
        {
            name = Name;
            children = new ArrayList<>();
            detectNth = -1;
        }
        
        @Override
        public void addChildExtractor(CustomStringExtractor cse) {
            children.add(cse);
        }

        @Override
        public String getExtractorName() {
            return name;
        }

        @Override
        public void setExtractorName(String Name) {
            name = Name;
        }

        @Override
        public void removeChildExtractor(int index) {
            if(index >= 0 && index < children.size())
                children.remove(index);
        }
        
        
        @Override
        public boolean showSetUpDialog() {
            //JTextBox for string
            //Detect
                //All (JRadioButton) (clicking deselects all other detect options
                //first (checkbox)
                //last (checkbox)
                //nth (checkbox)
            //Behavior
                //remove all text before (checkbox)
                //remove the string itself (checkbox)
                //remove all text after (checkbox)
                //Do nothing. Use this to pass the string unmodified (will be passed to any existing children before returning) (JRadioButton) (Deselect all others if selected)
            return false;
        }

        @Override
        public String extractString(String inputString) { 
            
            if(detectAll)
            {
                inputString = detectAllOcc(inputString);
            }
            else {
                if(detectNth > 0)
                {
                    inputString = detectNthOcc(inputString);
                }
                
                if(detectFirst)
                {
                    inputString = detectFirstOcc(inputString);
                }

                if(detectLast)
                {
                    inputString = detectLastOcc(inputString);
                }
            }
            
            if(children.isEmpty() || inputString == null || inputString.equals(""))
            {
                return ((inputString == null || inputString.equals(""))? null : inputString);
            }
            else
            {
                String temp;
                for(CustomStringExtractor c : children)
                {
                    temp = c.extractString(inputString);
                    if(temp != null)
                        return temp;
                }
                return null;
            }
            
        }
        
        private String detectNthOcc(String str)
        {
            String ret = "";
            int curIndex = 0;
            for(int i = 0; i < detectNth && curIndex != -1; i++)
            {
                curIndex = str.indexOf(occ, curIndex)+1;
            }
            if(curIndex == -1)
            {
                return null;
            }
            
            curIndex--;
            
            if(!removeTextBefore)
            {
                ret += str.substring(0, curIndex);
            }
            
            if(!removeOccurance)
            {
                ret += occ;
            }
            
            if(!removeTextAfter)
            {
                ret += str.substring(curIndex+occ.length());
            }
            
            return ret;
        }
        
        private String detectAllOcc(String str)
        {
            String[] strs;
            String ret = "";
            
            if(str == null || (strs = str.split(occ)).length <= 1)
            {
                return null;
            }
            
            for(int i = 0; i < strs.length; i++)
            {
                if(removeTextBefore && i != strs.length-1)
                    strs[i] = "";
                if(removeTextAfter && i != 0)
                    strs[i] = "";
                ret += strs[i] + ((!removeOccurance)? occ : "");
            }
            return ret;
        }
        
        private String detectFirstOcc(String str)
        {
            int occur;
            String ret = "";
            
            if(str == null || (occur = str.indexOf(occ)) == -1)
            {
                return null;
            }
            
            if(!removeTextBefore)
            {
                ret += str.substring(0, occur);
            }
            if(!removeOccurance)
            {
                ret += occ;
            }
            if(!removeTextAfter)
            {
                ret += str.substring(occur+occ.length());
            }
            
            return ret;
        }
        
        private String detectLastOcc(String str)
        {
            int occur;
            String ret = "";
            
            if(str == null || (occur = str.lastIndexOf(occ)) == -1)
            {
                return null;
            }
            
            if(!removeTextBefore)
            {
                ret += str.substring(0, occur);
            }
            if(!removeOccurance)
            {
                ret += occ;
            }
            if(!removeTextAfter)
            {
                ret += str.substring(occur+occ.length());
            }
            
            return ret;
        }        

        @Override
        public ArrayList<CustomStringExtractor> getChildrenExtractors() {
            return children;
        }
    }
    
    /**
     * lengthOccurance behaviors:
     *  - remove all before
     *      - including/excluding the nthCharacter
     *  - remove all after
     *      - including/excluding the nthCharacter
     *  - No modification
     *      - Used only to detect if a string is a specific length. Most 
     *          likely to be paired with sub-children
     *  - replacement/greater than/less than provisions?
     * 
     * Detection options
     *  - if String is (int) length1 OR (int) length2 OR ...
     *  - if String is NOT (int) length1 AND NOT (int) length2 AND NOT ...
     */
    
    public class lengthOccurance implements CustomStringExtractor
    {
        private int greaterThan;
        private int lessThan;
        private HashSet<Integer> notEqualTo;
        private HashSet<Integer> equalTo;
        

        @Override
        public boolean showSetUpDialog() {
            //Detection Options
                //If string length is (Length1) OR if string length is (Length 2) OR...
                //or
                //If string length is not (Length1) AND not (length2), AND not...
                //and/or
                //greater/less than provisions????
            
            //behavior
                //remove n characters from beginning (replace??)
                //remove characters i to j (inclusive??) (replace text??)
                //remove n characters from the end (replace??)
            return false;
        }

        @Override
        public String extractString(String inputString) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void addChildExtractor(CustomStringExtractor cse) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public String getExtractorName() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void setExtractorName(String name) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void removeChildExtractor(int index) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public ArrayList<CustomStringExtractor> getChildrenExtractors() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
        
        public class checkRawInput implements CustomStringExtractor
        {
            private final String name = "Don't Modify Input"; //better name!
            
            public checkRawInput()
            {
                
            }

            @Override
            public boolean showSetUpDialog() {
                return true;
            }

            @Override
            public String extractString(String inputString) {
                return inputString;
            }

            @Override
            public void addChildExtractor(CustomStringExtractor cse) {
                return;
            }

            @Override
            public void removeChildExtractor(int index) {
                return;
            }

            @Override
            public String getExtractorName() {
                return name;
            }

            @Override
            public void setExtractorName(String name) {
                return;
            }

            @Override
            public ArrayList<CustomStringExtractor> getChildrenExtractors() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
            
        }
        
    }
}
