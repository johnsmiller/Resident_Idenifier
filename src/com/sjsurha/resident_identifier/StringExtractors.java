/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sjsurha.resident_identifier;

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
         * Input string should not modified by this function. 
         * 
         * Input string may be null.
         * 
         * @param inputString the string to apply this extractor's rules to and 
         * attempt to return a valid string. Not modified by this function
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
        public void addSubExtractor(CustomStringExtractor cse);
        
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
     * Detection options: 
     *      - first
     *      - nth
     *      - all
     *      - last
     */
    
    public class nthOccurrence implements CustomStringExtractor
    {
        boolean removeTextBefore;
        boolean removeTextAfter;
        boolean removeOccurance;
        
        boolean detectFirst;
        boolean detectLast;
        boolean detectAll;
        int detectNth; //If greater than 0, treated as true
        
        String occ;
        
        public nthOccurrence()
        {
            detectNth = -1;
        }

        @Override
        public boolean showSetUpDialog() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public String extractString(String inputString) {
            String ret = "";
            
            if(detectAll)
            {
                ret = detectAllOcc(inputString);
                return (ret.equals("")? null : ret);
            }
            
            if(detectFirst)
            {
                
            }
        }
        
        private String detectAllOcc(String str)
        {
            String[] strs = str.split(occ);
            String ret = "";
            for(int i = 0; i < strs.length; i++)
            {
                if(removeTextBefore)
                    if(i != strs.length-1)
                        strs[i] = "";
                if(removeTextAfter)
                    if(i != 0)
                        strs[i] = "";
                ret = ret + strs[i] + ((removeOccurance)? "" : occ);
            }
            return ret;
        }
        
        private String detectFirstOcc(String str)
        {
            int occur = str.indexOf(occ);
            
        }

        @Override
        public void addSubExtractor(CustomStringExtractor cse) {
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
     * 
     * Detection options
     *  - if String is (int) length1 OR (int) length2 OR ...
     *  - if String is NOT (int) length1 AND NOT (int) length2 AND NOT ...
     */
    
    public class lengthOccurance implements CustomStringExtractor
    {

        @Override
        public boolean showSetUpDialog() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public String extractString(String inputString) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void addSubExtractor(CustomStringExtractor cse) {
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
        
    }
}
