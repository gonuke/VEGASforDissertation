/*
 * Parser.java
 *
 * Created on February 4, 2004, 4:43 PM
 */

/**
 *
 * @author  joshua
 */

import java.text.*;
import java.lang.*;
import java.util.*;
import java.io.*;

public class Parser extends java.lang.Object {
    public int tokenIndex;
    //    public int lineIndex;
    private String inDir = "parser" + File.separator;
    private String inFileName = "input.txt";
    private LineNumberReader fileReader;
    public Hashtable keyValPairs;
    /** Creates a new instance of Parser */
    public Parser() {
    }
    public Parser(String fn){
        openFile(fn);
        keyValPairs = getValues((char)0);
        closeFile();
    }
    public Parser(String fn, String dir){
        inDir=dir + File.separator;
        openFile(fn);
        keyValPairs = getValues((char)0);
        closeFile();
    }
    public static void main(String args[]){
        System.out.println((new Date()).getTime());
        Parser testParser = new Parser("input.txt");
        System.out.println((new Date()).getTime());
        return;
    }
    /** creates a new Parser with the keyValuePairs contained in the composite value
     * associated with <tt>key</tt>.  the optional <tt>index</tt> specifies which occurence
     * of <tt>key</tt> the Parser should contain.
     * @param key the key to a composite value.
     * @return the new Parser object
     */
    public Parser getSub(String key){
        if(keyValPairs.get(key) == null) return null;
        Parser p = new Parser();
        p.keyValPairs = (Hashtable)keyValPairs.get(key);
        return p;
    }
    /** creates a new Parser with the keyValuePairs contained in the composite value
     * associated with <tt>key</tt>.  the optional <tt>index</tt> specifies which occurence
     * of <tt>key</tt> the Parser should contain.
     * @return the new Parser object
     * @param index the index of the occurence of <tt>key</tt> in the input file, starting with zero.
     * @param key the key to a composite value.
     */
    public Parser getSub(String key, int index){
        if(keyValPairs.get(key)==null) return null;
        if(((ArrayList)keyValPairs.get(key)).size()<=index) return null;
        Parser p = new Parser();
        p.keyValPairs = (Hashtable)((ArrayList)keyValPairs.get(key)).get(index);
        return p;
    }
    private void openFile(String fn){
        try{
            fileReader = new LineNumberReader(new FileReader(inDir + fn));
        }catch(IOException ioe){
            System.err.println("error opening file " + inFileName
            + " in directory " + inDir);
            System.err.println(ioe.toString());
        }
        inFileName = fn;
    }
    private void closeFile(){
        try{
            fileReader.close();
        }catch(IOException ioe){
            System.err.println("error closing file (should be \"" + inFileName
            + "\") in directory " + inDir);
            System.err.println(ioe.toString());
        }
    }
    private char readChar(){
        try{
            int thisByte = fileReader.read();
            if(thisByte<0) return (char)0;
            else return (char)thisByte;
        }catch(IOException ioe){
            System.err.println("error reading from file (" + inFileName
            + ") in directory " + inDir + " at line " +
            fileReader.getLineNumber() + ".");
            System.err.println(ioe.toString());
        }
        return (char)(-1);
    }
    
    private Hashtable getValues(char terminator){
        Hashtable table = new Hashtable();
        String token = getToken(terminator);
        while(!token.equals("!DONE!")){
            String vals[] = token.split("=");
            if(vals.length != 2){
                System.err.println("token '" + token + "' on line "
                + fileReader.getLineNumber() + " not valid in file ("
                + inFileName + ").");
                System.exit(-1);
            }
            Object val;
            if(vals[1].equals("!COMPOSITE!")) val = getValues('}');
            else if(vals[1].equals("!ARRAY!")) val = getList(')');
            else val = vals[1];
            if(vals[0].matches(".*?\\[\\d*?\\]$")){
                vals[0]=vals[0].replaceFirst("\\[\\d*?\\]$","");
                if(table.get(vals[0]) == null) table.put(vals[0], new ArrayList());
                ((ArrayList)table.get(vals[0])).add(val);
            }else table.put(vals[0], val);
            token = getToken(terminator);
        }
        return table;
    }
    
    private ArrayList getList(char terminator){
        String token = getToken(terminator);
        ArrayList list = new ArrayList();
        while(!token.equals("!DONE!")){
            list.add(token);
            token=getToken(terminator);
        }
        return list;
    }
    
    private String getToken(char terminator){
        String token = "";
        char currChar;
        do{
            //        while(currChar != terminator && currChar != 0){
            currChar = readChar();
            if(currChar == '#') currChar = advToEOL();
            if(currChar == terminator) return "!DONE!";
            if(currChar == 0){
                System.err.println("error: EOF reached on line "
                + fileReader.getLineNumber() + " before closing '"
                + terminator + "' in file (inFileName)");
                System.exit(-1);
            }
        }while(Character.isWhitespace(currChar));
        do{
            String thatChar = Character.toString(currChar);
            if(currChar == '#'){
                currChar = advToEOL();
                return token;
            }
            if(currChar == '{'){
                token=token.concat("!COMPOSITE!");
                return token;
            }
            if(currChar == '('){
                token=token.concat("!ARRAY!");
                return token;
            }
            if(currChar==terminator)
                return token;
            if(currChar == 0){
                System.err.println("error: EOF reached on line "
                + fileReader.getLineNumber() + " before closing '"
                + terminator + "' in file (" + inFileName + ").");
                System.exit(-1);
            }
            if(currChar == '"') token=token.concat(readQuotedString());
            else token=token.concat(Character.toString(currChar));
            currChar = readChar();
        }while(!Character.isWhitespace(currChar));
        return token;
    }
    private char advToEOL(){
        char c;
        do c = readChar();
        while(c != '\n' && c != 0);
        return c;
    }
    private String readQuotedString(){
        String quoted="";
        char nextChar;
        nextChar=readChar();
        while(nextChar != '"'){
            quoted=quoted.concat(Character.toString(nextChar));
            nextChar = readChar();
        }
        return quoted;
    }
    
    private static Object internalSetParam(Object r, Object t, String key, int index){
        Hashtable table = (Hashtable)t;
        String thing;
        if( table.get(key) == null ) return r;
        if( index < 0 ) thing = (String)table.get(key);
        else thing = (String)((ArrayList)table.get(key)).get(index);
        if(r.getClass().getName().equals("java.lang.String")) r = thing;
        else if(r.getClass().getName().equals("java.lang.Integer"))
            r = Integer.valueOf(thing);
        else if(r.getClass().getName().equals("java.lang.Double"))
            r = Double.valueOf(thing);
        else{
            System.err.println("setParam: i don't know what to do with" +
            "an object of type " + r.getClass().getName());
            System.exit(-1);
        }
        return r;
    }
    
    private static int setParam(int r, Object t, String key){
        Integer rObj = new Integer(r);
        rObj = (Integer)internalSetParam(rObj,t,key,-1);
        return rObj.intValue();
    }
    /** this function checks to see if the input file hase a value for <tt>key</tt>, and
     * returns it, if it exists.  otherwise it returns <tt>r</tt>.  this function is
     * intended to be called in the form:<br>
     *
     * <tt>param = thisParser.getParam(param,"mykey");</tt><br>
     *
     * thus only changing param if the input file specifies it.
     *
     * <p>the function also uses <tt>r</tt> to choose one of the overloadings and thus
     * to decide whether the value associated with <tt>key</tt> is an int, Integer,
     * double, Double, or String.
     *
     * <p>The optional <tt>index</tt> in some overloadings is used when multiple values are
     * specified with the same key in the input file using the <tt>[]</tt> suffix.
     * Specifying an <tt>index</tt> returns the <tt>index</tt>th occurence of <tt>key</tt> in
     * the input file.
     * @param r the parameter this function will conditionally modify
     * @param key the key whose value, if it exists, will be returned.
     * @return the value of <tt>key</tt>, if it exists.  otherwise, <tt>r</tt>
     */
    public int getParam(int r, String key){return setParam(r,keyValPairs,key);}
    
    private static double setParam(double r, Object t, String key){
        Double rObj = new Double(r);
        rObj = (Double)internalSetParam(rObj, t, key, -1);
        return rObj.doubleValue();
    }
    /** this function checks to see if the input file hase a value for <tt>key</tt>, and
     * returns it, if it exists.  otherwise it returns <tt>r</tt>.  this function is
     * intended to be called in the form:<br>
     *
     * <tt>param = thisParser.getParam(param,"mykey");</tt><br>
     *
     * thus only changing param if the input file specifies it.
     *
     * <p>the function also uses <tt>r</tt> to choose one of the overloadings and thus
     * to decide whether the value associated with <tt>key</tt> is an int, Integer,
     * double, Double, or String.
     *
     * <p>The optional <tt>index</tt> in some overloadings is used when multiple values are
     * specified with the same key in the input file using the <tt>[]</tt> suffix.
     * Specifying an <tt>index</tt> returns the <tt>index</tt>th occurence of <tt>key</tt> in
     * the input file.
     * @param r the parameter this function will conditionally modify
     * @param key the key whose value, if it exists, will be returned.
     * @return the value of <tt>key</tt>, if it exists.  otherwise, <tt>r</tt>
     */
    public double getParam(double r, String key){return setParam(r,keyValPairs,key);}
    
    private static Double setParam(Double r, Object t, String key, int index){
        return (Double)internalSetParam(r, t, key, index);
    }
    /** this function checks to see if the input file hase a value for <tt>key</tt>, and
     * returns it, if it exists.  otherwise it returns <tt>r</tt>.  this function is
     * intended to be called in the form:<br>
     *
     * <tt>param = thisParser.getParam(param,"mykey");</tt><br>
     *
     * thus only changing param if the input file specifies it.
     *
     * <p>the function also uses <tt>r</tt> to choose one of the overloadings and thus
     * to decide whether the value associated with <tt>key</tt> is an int, Integer,
     * double, Double, or String.
     *
     * <p>The optional <tt>index</tt> in some overloadings is used when multiple values are
     * specified with the same key in the input file using the <tt>[]</tt> suffix.
     * Specifying an <tt>index</tt> returns the <tt>index</tt>th occurence of <tt>key</tt> in
     * the input file.
     * @param r the parameter this function will conditionally modify
     * @param key the key whose value, if it exists, will be returned.
     * @return the value of <tt>key</tt>, if it exists.  otherwise, <tt>r</tt>
     */
    public Double getParam(Double r, String key, int index){return setParam(r,keyValPairs,key,index);}
    
    private static Integer setParam(Integer r, Object t, String key, int index){
        return (Integer)internalSetParam(r,t,key,index);
    }
    /** this function checks to see if the input file hase a value for <tt>key</tt>, and
     * returns it, if it exists.  otherwise it returns <tt>r</tt>.  this function is
     * intended to be called in the form:<br>
     *
     * <tt>param = thisParser.getParam(param,"mykey");</tt><br>
     *
     * thus only changing param if the input file specifies it.
     *
     * <p>the function also uses <tt>r</tt> to choose one of the overloadings and thus
     * to decide whether the value associated with <tt>key</tt> is an int, Integer,
     * double, Double, or String.
     *
     * <p>The optional <tt>index</tt> in some overloadings is used when multiple values are
     * specified with the same key in the input file using the <tt>[]</tt> suffix.
     * Specifying an <tt>index</tt> returns the <tt>index</tt>th occurence of <tt>key</tt> in
     * the input file.
     * @param r the parameter this function will conditionally modify
     * @param key the key whose value, if it exists, will be returned.
     * @return the value of <tt>key</tt>, if it exists.  otherwise, <tt>r</tt>
     */
    public Integer getParam(Integer r, String key, int index){return setParam(r,keyValPairs,key,index);}
    
    private static String setParam(String r, Object t, String key, int index){
        return (String)internalSetParam(r,t,key,index);
    }
    /** this function checks to see if the input file hase a value for <tt>key</tt>, and
     * returns it, if it exists.  otherwise it returns <tt>r</tt>.  this function is
     * intended to be called in the form:<br>
     *
     * <tt>param = thisParser.getParam(param,"mykey");</tt><br>
     *
     * thus only changing param if the input file specifies it.
     *
     * <p>the function also uses <tt>r</tt> to choose one of the overloadings and thus
     * to decide whether the value associated with <tt>key</tt> is an int, Integer,
     * double, Double, or String.
     *
     * <p>The optional <tt>index</tt> in some overloadings is used when multiple values are
     * specified with the same key in the input file using the <tt>[]</tt> suffix.
     * Specifying an <tt>index</tt> returns the <tt>index</tt>th occurence of <tt>key</tt> in
     * the input file.
     * @param r the parameter this function will conditionally modify
     * @param key the key whose value, if it exists, will be returned.
     * @return the value of <tt>key</tt>, if it exists.  otherwise, <tt>r</tt>
     */
    public String getParam(String r, String key, int index){return setParam(r,keyValPairs,key,index);}
    
    private static Double setParam(Double r, Object t, String key){
        return (Double)internalSetParam(r, t, key, -1);
    }
    /** this function checks to see if the input file hase a value for <tt>key</tt>, and
     * returns it, if it exists.  otherwise it returns <tt>r</tt>.  this function is
     * intended to be called in the form:<br>
     *
     * <tt>param = thisParser.getParam(param,"mykey");</tt><br>
     *
     * thus only changing param if the input file specifies it.
     *
     * <p>the function also uses <tt>r</tt> to choose one of the overloadings and thus
     * to decide whether the value associated with <tt>key</tt> is an int, Integer,
     * double, Double, or String.
     *
     * <p>The optional <tt>index</tt> in some overloadings is used when multiple values are
     * specified with the same key in the input file using the <tt>[]</tt> suffix.
     * Specifying an <tt>index</tt> returns the <tt>index</tt>th occurence of <tt>key</tt> in
     * the input file.
     * @param r the parameter this function will conditionally modify
     * @param key the key whose value, if it exists, will be returned.
     * @return the value of <tt>key</tt>, if it exists.  otherwise, <tt>r</tt>
     */
    public Double getParam(Double r, String key){return setParam(r,keyValPairs,key);}
    
    private static Integer setParam(Integer r, Object t, String key){
        return (Integer)internalSetParam(r, t, key, -1);
    }
    /** this function checks to see if the input file hase a value for <tt>key</tt>, and
     * returns it, if it exists.  otherwise it returns <tt>r</tt>.  this function is
     * intended to be called in the form:<br>
     *
     * <tt>param = thisParser.getParam(param,"mykey");</tt><br>
     *
     * thus only changing param if the input file specifies it.
     *
     * <p>the function also uses <tt>r</tt> to choose one of the overloadings and thus
     * to decide whether the value associated with <tt>key</tt> is an int, Integer,
     * double, Double, or String.
     *
     * <p>The optional <tt>index</tt> in some overloadings is used when multiple values are
     * specified with the same key in the input file using the <tt>[]</tt> suffix.
     * Specifying an <tt>index</tt> returns the <tt>index</tt>th occurence of <tt>key</tt> in
     * the input file.
     * @param r the parameter this function will conditionally modify
     * @param key the key whose value, if it exists, will be returned.
     * @return the value of <tt>key</tt>, if it exists.  otherwise, <tt>r</tt>
     */
    public Integer getParam(Integer r, String key){return setParam(r,keyValPairs,key);}
    
    private static String setParam(String r, Object t, String key){
        return (String)internalSetParam(r,t,key, -1);
    }
    /** this function checks to see if the input file hase a value for <tt>key</tt>, and
     * returns it, if it exists.  otherwise it returns <tt>r</tt>.  this function is
     * intended to be called in the form:<br>
     *
     * <tt>param = thisParser.getParam(param,"mykey");</tt><br>
     *
     * thus only changing param if the input file specifies it.
     *
     * <p>the function also uses <tt>r</tt> to choose one of the overloadings and thus
     * to decide whether the value associated with <tt>key</tt> is an int, Integer,
     * double, Double, or String.
     *
     * <p>The optional <tt>index</tt> in some overloadings is used when multiple values are
     * specified with the same key in the input file using the <tt>[]</tt> suffix.
     * Specifying an <tt>index</tt> returns the <tt>index</tt>th occurence of <tt>key</tt> in
     * the input file.
     * @param r the parameter this function will conditionally modify
     * @param key the key whose value, if it exists, will be returned.
     * @return the value of <tt>key</tt>, if it exists.  otherwise, <tt>r</tt>
     */
    public String getParam(String r, String key){return setParam(r,keyValPairs,key);}
        
    public static int getListSize(String s, Object t){
        if(((ArrayList)((Hashtable)t).get(s))==null) return(-1);
        else return ((ArrayList)((Hashtable)t).get(s)).size();
    }
    public int getListSize(String s){ return getListSize(s, keyValPairs); }
    
    public boolean hasKey(String key){
        if(keyValPairs.get(key)==null) return false;
        return true;
    }
    
    private static Hashtable getHashEl(Object table, String key, int index){
        return (Hashtable)((ArrayList)((Hashtable)table).get(key)).get(index);
    }
    
    private Hashtable getHashEl(String key, int index){
        return (Hashtable)((ArrayList)keyValPairs.get(key)).get(index);
    }
    
    private Hashtable getHashV(Object table, String key){
        return (Hashtable)((Hashtable)table).get(key);
    }
    
    private Hashtable getHashV(String key){
        return (Hashtable)keyValPairs.get(key);
    }
 
}
