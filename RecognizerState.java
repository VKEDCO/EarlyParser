package org.vkedco.nlp.earlyparser;

/**
 *
 * @author vladimir kulyukin
 */
public class RecognizerState {
    int mRuleNum;  // number of a cfg rule that this state tracks.
    int mDotPos;   // dot position in the rhs of the rule.
    int mInputFromPos;  // where in the input the rule starts.
    int mUptoPos;  // where in the input the rule ends.
    CFProduction mTrackedRule; // the actual CFG rule being tracked; the rule's number == ruleNum.

    @Override
    public String toString() {
        String rslt = "[RS";
        rslt += " dp=";
        rslt += mDotPos;
        rslt += " from=";
        rslt += mInputFromPos;
        rslt += " upto=";
        rslt += mUptoPos;
        rslt += " ";
        rslt += mTrackedRule.toString();
        rslt += "]";
        return rslt;
    }

    public CFGSymbol nextCat() {
        if ( mDotPos < mTrackedRule.mRHS.size() )
            return mTrackedRule.mRHS.get(mDotPos);
        else
            return null;
    }

    public boolean isComplete() {
        return mDotPos == mTrackedRule.mRHS.size();
    }

    public int getDotPos() { return mDotPos; }
    public int getFromPos() { return mInputFromPos; }
    public int getUptoPos() { return mUptoPos; }
    public CFProduction getCFGRule() { return mTrackedRule; }
    public int getRuleNum() { return mRuleNum; }

    public RecognizerState() {
        mRuleNum = -1;
        mDotPos = -1;
        mInputFromPos = -1;
        mUptoPos = -1;
        mTrackedRule = null;
    }

    public RecognizerState(int ruleNum, int dotPos, int fromPos, int uptoPos, CFProduction r) {
        this.mRuleNum = ruleNum;
        this.mDotPos = dotPos;
        this.mInputFromPos = fromPos;
        this.mUptoPos = uptoPos;
        this.mTrackedRule = new CFProduction(r);
    }

    public boolean isEqual(RecognizerState ps) {
        return this.mRuleNum == ps.mRuleNum &&
               this.mInputFromPos == ps.mInputFromPos &&
               this.mUptoPos == ps.mUptoPos &&
               this.mDotPos  == ps.mDotPos;
    }
}
