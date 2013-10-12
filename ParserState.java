package org.vkedco.nlp.earlyparser;
import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 * @author vladimir kulyukin
 */
public class ParserState extends RecognizerState {

    static int mCount = 0;
    int mID = 0; // this is the unique id of this ParserState
    ArrayList<ParserState> mParentParserStates = null; // IDs of parent parser states
    String mOrigin = "N"; // it can be N (None), S (Scanner), P (Predictor), C (Completer)

    public ParserState(int ruleNum, int dotPos, int fromPos, int uptoPos, CFProduction r) {
        super(ruleNum, dotPos, fromPos, uptoPos, r);
        //System.out.println("PS Constructor " + count);
        mID = mCount;
        mCount = mCount + 1;
        mParentParserStates = new ArrayList<ParserState>();
    }

    String getID() {
        return "PS" + mID;
    }

    void addPreviousState(ParserState ps) {
        mParentParserStates.add(ps);
    }

    // add all previous states of ps into the this state.
    void addPreviousStatesOf(ParserState ps) {
        if ( ps.mParentParserStates.isEmpty() ) return;

        Iterator<ParserState> iter = ps.mParentParserStates.iterator();
        while ( iter.hasNext() ) {
            addPreviousState(iter.next());
        }
    }
    
    void setOrigin(String origin) {
        mOrigin = origin;
    }

    @Override
    public String toString() {
        String rslt = "[PS" + mID;
        rslt += " | dp=";
        rslt += mDotPos;
        rslt += " from=";
        rslt += mInputFromPos;
        rslt += " upto=";
        rslt += mUptoPos;
        rslt += " | ";
        rslt += mOrigin;
        rslt += " | ";
        rslt += mTrackedRule.toString();
        if ( mParentParserStates.isEmpty() ) {
            rslt += "]";
            return rslt;
        }
        rslt += " | ";
        Iterator<ParserState> iter = mParentParserStates.iterator();
        while ( iter.hasNext() ) {
            rslt += iter.next().getID() + " ";
        }
        rslt += "]";
        return rslt;
    }

}
