package org.vkedco.nlp.earlyparser;

import java.util.TreeMap;
import java.util.Set;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map.Entry;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;

/**
 *
 * @author vladimir kulyukin
 */
public class CFGrammar {

    protected ArrayList<String> mVariables; // sorted array of variables
    protected ArrayList<String> mTerminals; // sorted array of terminals
    protected TreeMap<String, ArrayList<CFProduction>> mProductions; // variable to rules where
    // variable is lhs
    protected CFGSymbol mStartSymbol;
    protected ArrayList<CFProduction> mIdToProductionMap; // given an id, find a rule.
    protected ArrayList<String> mPosVars; // sorted lists of parts of speech.
    protected TreeMap<String, ArrayList<CFGSymbol>> mTerminalsToPosVarsMap;

    class compareRuleId implements java.util.Comparator {

        public int compare(Object x, Object y) {
            CFProduction rule1 = (CFProduction) x;
            CFProduction rule2 = (CFProduction) y;
            if (rule1.mID < rule2.mID) {
                return -1;
            } else if (rule1.mID == rule2.mID) {
                return 0;
            } else {
                return 1;
            }
        }
    }

    public CFGrammar() {
        mVariables = new ArrayList<String>();
        mTerminals = new ArrayList<String>();
        mProductions = new TreeMap<String, ArrayList<CFProduction>>();
        mIdToProductionMap = new ArrayList<CFProduction>();
        mPosVars = new ArrayList<String>();
        mTerminalsToPosVarsMap = new TreeMap<String, ArrayList<CFGSymbol>>();
        mStartSymbol = null;
    }

    public CFGrammar(String grammarFilePath) {
        this();
        compileCFGGrammar(grammarFilePath);
    }

    private void addProduction(String line) {
        String[] tokens = line.split("\\s+");
        
        if ( tokens == null ) {
            System.err.println("Cannot parse production: " + line);
            return;
        }
        else if ( tokens.length < 3 ) {
            System.err.println("Cannot parse production: " + line);
            return;
        }
        else if ( !"::=".equals(tokens[1]) ) {
            System.err.println("Cannot parse production: " + line);
            System.err.println("Second symbol must be ::=");
            return;
        }
        for(int i = 0; i < tokens.length; i++) {
            if ( !(isVariable(tokens[i]) || isTerminal(tokens[i]) ||
                    "::=".equals(tokens[i])) )  {
                System.err.println("Illegal symbol in production: " + line);
                return;
            }
        }
        CFGSymbol lhs = new CFGSymbol(tokens[0]);
        ArrayList<CFGSymbol> rhs = new ArrayList<CFGSymbol>();
        for(int i = 2; i < tokens.length; i++) {
            rhs.add(new CFGSymbol(tokens[i]));
        }
        addCFGRule(new CFProduction(lhs, rhs));
    }

    private void addCFGRule(CFProduction r) {
        if (mProductions == null) {
            return;
        }
        String lhsStr = r.mLHS.mSymbolName;
        ArrayList<CFProduction> curRules = mProductions.get(lhsStr);
        if (curRules == null) {
            curRules = new ArrayList<CFProduction>();
            curRules.add(r);
            mProductions.put(lhsStr, curRules);
        } else {
            curRules.add(r);
        }
        r.mID = mIdToProductionMap.size();
        mIdToProductionMap.add(r);
    }

    private boolean isInPosVars(String s) {
        Iterator<String> iter = mPosVars.iterator();
        while ( iter.hasNext() ) {
            if ( s.equals(iter.next()) )
                return true;
        }
        return false;
    }

    private void addTerminalToTerminalsToPosVarsMap(CFProduction r) {
        CFGSymbol term = r.mRHS.get(0);
        CFGSymbol posVar = r.mLHS;
        ArrayList<CFGSymbol> syms = mTerminalsToPosVarsMap.get(term.mSymbolName);
        if ( syms == null ) {
            syms = new ArrayList<CFGSymbol>();
            syms.add(new CFGSymbol(posVar));
            mTerminalsToPosVarsMap.put(term.mSymbolName, syms);
        }
        else {
            boolean inFlag = false;
            Iterator<CFGSymbol> iter = syms.iterator();
            CFGSymbol curSym = null;
            while ( iter.hasNext() ) {
                curSym = iter.next();
                if( curSym.isEqual(posVar) ) {
                    inFlag = true;
                    break;
                }
            }
            if ( !inFlag ) {
                syms.add(new CFGSymbol(posVar));
            }
        }
    }

    private void addPartsOfSpeech() {
        //go thru the rules and add the lhs'
        //that are parts of speech and sort posVars.
        Iterator<CFProduction> iterator = mIdToProductionMap.iterator();
        CFProduction curRule = null;
        while (iterator.hasNext()) {
            curRule = iterator.next();
            if (isPartOfSpeechRule(curRule)) {
                if ( !isInPosVars(curRule.mLHS.mSymbolName) )
                    mPosVars.add(curRule.mLHS.mSymbolName);
                addTerminalToTerminalsToPosVarsMap(curRule);
            }
        }
        Collections.sort(mPosVars);
    }

    private void addTerminal(CFGSymbol term) {
        Iterator<String> iterator = mTerminals.iterator();
        boolean flag = false;
        while (iterator.hasNext()) {
            if (term.mSymbolName.equals(iterator.next())) {
                flag = true;
            }
        }
        if (!flag) {
            mTerminals.add(term.mSymbolName);
        }
    }

    private void addVariable(CFGSymbol var) {
        Iterator<String> iterator = mVariables.iterator();
        boolean flag = false;
        while (iterator.hasNext()) {
            if (var.mSymbolName.equals(iterator.next())) {
                flag = true;
            }
        }
        if (!flag) {
            mVariables.add(var.mSymbolName);
        }
    }

    public boolean isVariable(CFGSymbol s) {
        return isVariable(s.mSymbolName);
    }

    public boolean isVariable(String s) {
        int i = Collections.binarySearch(mVariables, s);
        if (i != -1) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isPartOfSpeech(CFGSymbol s) {
        return isInPosVars(s.mSymbolName);
    }

    public boolean isPartOfSpeech(String s) {
        return isInPosVars(s);
    }

    public boolean isTerminal(CFGSymbol s) {
        return isTerminal(s.mSymbolName);
    }

    public boolean isTerminal(String s) {
        int i = Collections.binarySearch(mTerminals, s);
        if (i < 0) {
            return false;
        } else {
            return true;
        }
    }
    
    public boolean isPartOfSpeechRule(CFProduction r) {
        if (r.mRHS.size() == 1 && isTerminal(r.mRHS.get(0))) {
            return true;
        } else {
            return false;
        }
    }

    public void displayGrammar() {
        System.out.print("Start: " + mStartSymbol.toString());
        Iterator<String> iterator = mVariables.iterator();
        System.out.println("\nVARIABLES:");
        while (iterator.hasNext()) {
            System.out.print(iterator.next() + " ");
        }
        System.out.println("\nTERMINALS:");
        iterator = mTerminals.iterator();
        while (iterator.hasNext()) {
            System.out.print(iterator.next() + " ");
        }
        System.out.println("\nPARTS OF SPEECH:");
        iterator = mPosVars.iterator();
        while (iterator.hasNext()) {
            System.out.print(iterator.next() + " ");
        }
        System.out.println("\nPRODUCTIONS:");
        Iterator<CFProduction> iterator2 = mIdToProductionMap.iterator();
        while (iterator2.hasNext()) {
            System.out.println(iterator2.next().toString());
        }
        System.out.println("\nTERMINALS TO PARTS OF SPEECH:");
        Set<Entry<String, ArrayList<CFGSymbol>>> set = mTerminalsToPosVarsMap.entrySet();
        Iterator<Entry<String, ArrayList<CFGSymbol>>> iterator3 = set.iterator();
        while ( iterator3.hasNext() ) {
            Map.Entry<String, ArrayList<CFGSymbol>> me =
                    (Map.Entry<String, ArrayList<CFGSymbol>>) iterator3.next();
            System.out.print(me.getKey().toString() + " : ");
            Iterator<CFGSymbol> iterator4 =
                    ((ArrayList<CFGSymbol>) me.getValue()).iterator();
            while ( iterator4.hasNext() ) {
                System.out.print(iterator4.next().toString() + " ");
            }
            System.out.println();
        }

        System.out.println();
    }

    public void compileCFGGrammar(String filePath) {
        BufferedReader inputStream = null;
        boolean vFlag = false;
        boolean tFlag = false;
        boolean sFlag = false;
        boolean pFlag = false;

        try {
            inputStream = new BufferedReader(new FileReader(filePath));
         
            String line;
            while ((line = inputStream.readLine()) != null) {
                if ( line.length() == 0 ) {
                    continue;
                }
                else if ( "<V>".equals(line.trim()) ) {
                    vFlag = true;
                }
                else if ( "</V>".equals(line.trim()) ) {
                    vFlag = false;
                    Collections.sort(mVariables);
                }
                else if ( "<T>".equals(line.trim()) ) {
                    tFlag = true;
                }
                else if ( "</T>".equals(line.trim()) ) {
                    tFlag = false;
                    Collections.sort(mTerminals);
                }
                else if ( "<S>".equals(line.trim()) ) {
                    sFlag = true;
                }
                else if ( "</S>".equals(line.trim()) ) {
                    sFlag = false;
                }
                else if ( "<P>".equals(line.trim()) ) {
                    pFlag = true;
                }
                else if ( "</P>".equals(line.trim()) ) {
                    pFlag = false;
                }
                else if ( vFlag ) {
                    addVariable(new CFGSymbol(line.trim()));
                }
                else if ( sFlag ) {
                    mStartSymbol = new CFGSymbol(line.trim());
                    addVariable(mStartSymbol);
                }
                else if ( tFlag ) {
                    addTerminal(new CFGSymbol(line.trim()));
                }
                else if ( pFlag ) {
                    addProduction(line.trim());
                }
                else {
                    System.err.println("Illegal parsing state");
                }
            }
            addPartsOfSpeech();
        }
        catch ( IOException ex ) {
            System.err.println(ex.toString());
        }
        finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                }
                catch ( IOException ex ) {
                    System.err.println(ex.toString());
                }
            }
        }
    }

    public ArrayList<CFProduction> getRulesForLHS(CFGSymbol lhs) {
        ArrayList<CFProduction> rslt = mProductions.get(lhs.mSymbolName);
        return rslt;
    }

    public ArrayList<CFGSymbol> getPartsOfSpeechForTerminal(CFGSymbol s) {
        if ( !isTerminal(s) ) return null;
        return mTerminalsToPosVarsMap.get(s.toString());
    }

    public boolean isInPartsOfSpeech(CFGSymbol var, CFGSymbol term) {
        if ( !isVariable(var) || !isTerminal(term) ) return false;
        ArrayList<CFGSymbol> pos = getPartsOfSpeechForTerminal(term);
        Iterator<CFGSymbol> iter = pos.iterator();
        while ( iter.hasNext() ) {
            if ( var.isEqual(iter.next()) )
                return true;
        }
        return false;
    }

    // Retrieve the rule var ::= term
    public CFProduction getPartOfSpeechRule(CFGSymbol var, CFGSymbol term) {
        if ( !isVariable(var) || !isTerminal(term) ) return null;
        ArrayList<CFProduction> cfgrules = getRulesForLHS(var);
        Iterator<CFProduction> iter = cfgrules.iterator();
        CFProduction curRule = null;
        while ( iter.hasNext() ) {
            curRule = iter.next();
            if ( isPartOfSpeechRule(curRule) ) {
                if ( curRule.mLHS.isEqual(var) &&
                        curRule.mRHS.get(0).isEqual(term) )
                    return curRule;
            }
        }
        return null;
    }

    public CFGSymbol getStartSymbol() {
        return new CFGSymbol(mStartSymbol);
    }
}

