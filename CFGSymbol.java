package org.vkedco.nlp.earlyparser;

/**
 *
 * @author vladimir kulyukin
 *
 * 
 */

public class CFGSymbol {
    String mSymbolName;

    public CFGSymbol() { mSymbolName = ""; }

    public CFGSymbol(String n) { 
        mSymbolName = new String(n); }

    public CFGSymbol(CFGSymbol s) {
        mSymbolName = new String(s.mSymbolName);
    }

    @Override
    public String toString() {
        return mSymbolName;
    }

    public boolean isEqual(CFGSymbol s) {
        if ( s == null )
            return false;
        else
            return this.mSymbolName.equals(s.mSymbolName);
    }
}
