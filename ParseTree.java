package org.vkedco.nlp.earlyparser;
import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 * @author vladimir kulyukin
 */
public class ParseTree {

    public CFProduction mCFProduction = null;
    public ArrayList<ParseTree> children = null;

    public ParseTree(CFProduction cfgRule) {
        this.mCFProduction = cfgRule;
        children = new ArrayList<ParseTree>();
    }

    public void addChild(ParseTree pt) {
        children.add(pt);
    }

    public void display() {
        displayWithIndentation(this, 0);
    }

    private void displayWithIndentation(ParseTree pt, int numSpaces) {
        String indentStr = "";
        for(int i = 0; i < numSpaces; i++)
            indentStr += " ";
        System.out.print(indentStr);
        System.out.println(pt.mCFProduction.mLHS.toString() + " ");
        if ( pt.children.size() == 0 ) {
            System.out.println(indentStr + " " + pt.mCFProduction.mRHS.get(0).toString());
            return;
        }
        Iterator<ParseTree> iter = pt.children.iterator();
        while ( iter.hasNext() ) {
            displayWithIndentation(iter.next(), numSpaces+1);
        }
    }
}
 
