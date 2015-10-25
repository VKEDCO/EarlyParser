package org.vkedco.nlp.earlyparser;
import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 * @author vladimir kulyukin
 */
public class Parser {
    final static CFGSymbol mDummyStartSymbol = new CFGSymbol("***lambda***");

    CFGrammar mCFG = null;
    ArrayList<ArrayList<ParserState>> mChart = null;
    private boolean mPredictorChartModifiedFlag = false;
    private boolean mCompleterChartModifiedFlag = false;
    private boolean mDebugFlag = false;

    public Parser(CFGrammar cfg) {
        this.mCFG = cfg;
    }

    public void initChart(int chartSize) {
        this.mChart = new ArrayList<ArrayList<ParserState>>();
        for (int i = 0; i <= chartSize; i++) {
            this.mChart.add(new ArrayList<ParserState>());
        }
        addDummyParserState();
    }

    public void displayGrammar() {
        mCFG.displayGrammar();
    }

    public void displayChart() {
        if (mChart != null) {
            Iterator<ArrayList<ParserState>> outerIter = mChart.iterator();
            Iterator<ParserState> innerIter = null;
            ArrayList<ParserState> curParseStates = null;
            int chartCounter = 0;
            while (outerIter.hasNext()) {
                curParseStates = outerIter.next();
                if (curParseStates != null) {
                    System.out.println("-----------------");
                    System.out.println(chartCounter);
                    innerIter = curParseStates.iterator();
                    while (innerIter.hasNext()) {
                        System.out.println(innerIter.next().toString());
                    }
                    System.out.println("-----------------");
                    chartCounter++;
                }
            }
        }
    }

    // get the element at chart[i,j].
    private ParserState getChartElem(int i, int j) {
        if (i < mChart.size()) {
            ArrayList<ParserState> pstates = mChart.get(i);
            if (j < pstates.size()) {
                return pstates.get(j);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public void setDebugFlag(boolean flagVal) {
        mDebugFlag = flagVal;
    }

    public boolean debugFlagOn() { return mDebugFlag; }

    // the input is split into symbols on space.
    public static ArrayList<CFGSymbol> splitStringIntoSymbols(String input) {
        if ( input.length() == 0 ) return null;
        ArrayList<CFGSymbol> symbols = new ArrayList<CFGSymbol>();
        String[] tokens = input.split("\\s+");
        for(int i = 0; i < tokens.length; i++) {
            symbols.add(new CFGSymbol(tokens[i]));
        }
        return symbols;
    }

    // check if the ParserState ps is in chart[pos].
    private boolean isInChart(ParserState ps, int pos) {
        if (mChart == null) {
            return false;
        }
        if (pos > mChart.size() - 1) {
            return false;
        }
        ArrayList<ParserState> pStates = mChart.get(pos);
        if (    pStates.isEmpty()   ) {
            return false;
        }
        Iterator<ParserState> iter = pStates.iterator();
        while (iter.hasNext()) {
            if (ps.isEqual(iter.next())) {
                return true;
            }
        }
        return false;
    }

    // this method is used by addScannedUpdates to chart.
    // the scanner does not modify the chart at a given pos.
    // it always adds to a postion in the chart to the right of pos.
    private void addToChart(ParserState ps, int pos) {
        if (pos >= mChart.size()) {
            return;
        }
        if (!isInChart(ps, pos)) {
            if ( debugFlagOn() ) {
                System.out.println("Adding " + ps.toString() + " at " + pos);
            }
            mChart.get(pos).add(ps);
        }
    }

    // predictorChartModified flag is set only if this
    // method adds a new parse state at a particular position.
    private void addPredictedUpdateToChart(ParserState ps, int pos) {
        if (pos >= mChart.size()) {
            return;
        }
        if (!isInChart(ps, pos)) {
            if ( debugFlagOn() ) {
                System.out.println("Adding " + ps.toString() + " at " + pos);
            }
            ps.setOrigin("P");
            mChart.get(pos).add(ps);
            mPredictorChartModifiedFlag = true;
            
        }
    }

    // completerChartModified flag is set only if this method
    // adds a new parse state at a given position.
    private void addCompletedUpdateToChart(ParserState ps, int pos) {
        if (pos >= mChart.size()) {
            return;
        }
        if (!isInChart(ps, pos)) {
            if ( debugFlagOn() ) {
                System.out.println("Adding " + ps.toString() + " at " + pos);
            }
            ps.setOrigin("C");
            mChart.get(pos).add(ps);
            mCompleterChartModifiedFlag = true;
        }
    }

    private void addPredictedUpdatesToChart(ArrayList<ParserState> updates) {
        if (updates == null) {
            return;
        }
        Iterator<ParserState> iter = updates.iterator();
        ParserState ps = null;
        while (iter.hasNext()) {
            ps = iter.next();
            addPredictedUpdateToChart(ps, ps.getUptoPos());
        }
    }

    private void addCompletedUpdatesToChart(ArrayList<ParserState> updates) {
        if (updates == null) {
            return;
        }
        Iterator<ParserState> iter = updates.iterator();
        ParserState ps = null;
        while (iter.hasNext()) {
            ps = iter.next();
            addCompletedUpdateToChart(ps, ps.getUptoPos());
        }
    }

    private void addScannedUpdatesToChart(ArrayList<ParserState> updates) {
        if (updates == null) {
            return;
        }
        Iterator<ParserState> iter = updates.iterator();
        ParserState ps = null;
        while (iter.hasNext()) {
            ps = iter.next();
            ps.setOrigin("S");
            addToChart(ps, ps.getUptoPos());
        }
    }

    private boolean sameNextCat(CFGSymbol s, ParserState ps) {
        return s.isEqual(ps.nextCat());
    }

    // RecognizerState(int ruleNum, int dotPos, int fromPos, int uptoPos, CFGRule r)
    private void addDummyParserState() {
        CFGSymbol dlhs = mDummyStartSymbol;
        ArrayList<CFGSymbol> drhs = new ArrayList<CFGSymbol>();
        drhs.add(mCFG.getStartSymbol());
        CFProduction dr = new CFProduction(dlhs, drhs);
        dr.mID = -1;
        ParserState dummyParserState = new ParserState(-1, 0, 0, 0, dr);
        CFProduction r = dummyParserState.getCFGRule();
        addToChart(dummyParserState, 0);
    }

    // there is no need to have a chartupdate class.
    // all new states should be added at their upto positions.
    // In other words, addPS.getUptoPos().
    private ArrayList<ParserState> predict(ParserState ps) {
        if ( debugFlagOn() )
            System.out.println("Predicting on " + ps.toString());
        ArrayList<ParserState> chartUpdates = null;
        if ( debugFlagOn() )
            System.out.println("Getting rules for " + ps.nextCat().toString());
        ArrayList<CFProduction> rules = mCFG.getRulesForLHS(ps.nextCat());
        if (rules == null) {
            if ( debugFlagOn() )
                System.out.println("no rules found");
            return null;
        } else {
            if ( debugFlagOn() )
                System.out.println(rules.size() + " rules found");
        }
        Iterator<CFProduction> iter = rules.iterator();
        ParserState addPS = null;
        CFProduction curRule = null;
        while (iter.hasNext()) {
            curRule = iter.next();
            addPS = new ParserState(curRule.mID, 0, ps.getUptoPos(),
                    ps.getUptoPos(), curRule);
            if (chartUpdates == null) {
                chartUpdates = new ArrayList<ParserState>();
            }
            chartUpdates.add(addPS);
        }
        if ( debugFlagOn() )
            System.out.println(chartUpdates.size() + " updates generated");
        return chartUpdates;
    }

    // Have scanner return a list of updates.
    private ArrayList<ParserState> scan(ParserState ps, ArrayList<CFGSymbol> words) {
        if ( debugFlagOn() )
            System.out.println("Scanning " + ps.toString());
        if (ps.getUptoPos() >= words.size()) {
            return null;
        }

        ArrayList<ParserState> updates = null;
        //CFGSymbol sj = words.get(ps.getUptoPos());
        CFGSymbol var = ps.nextCat();
        CFGSymbol term = words.get(ps.getUptoPos());
        if (mCFG.isInPartsOfSpeech(var, term)) {
            ParserState addPS = null;
            // we need to find the rule ps.NextCat() ::= words.get(ps.getUptoPos());
            CFProduction rule = mCFG.getPartOfSpeechRule(var, term);
            if (rule == null) {
                System.err.println("Rule " + var + " ::= " + term +
                        " is not in grammar");
                return null;
            }
            addPS = new ParserState(rule.mID, 1,
                    ps.getUptoPos(), ps.getUptoPos() + 1, rule);
            if ( debugFlagOn() )
                System.out.println("Scanner adding " + addPS.toString());
            updates = new ArrayList<ParserState>();
            updates.add(addPS);
        }
        return updates;
    }

    // Work on complete
    private ArrayList<ParserState> complete(ParserState ps) {
        if ( debugFlagOn() )
            System.out.println("Completing " + ps.toString());
        ArrayList<ParserState> chartUpdates = null;
        ArrayList<ParserState> parseStates = mChart.get(ps.getFromPos());
        Iterator<ParserState> iter = parseStates.iterator();
        CFGSymbol completedSymbol = ps.getCFGRule().mLHS;
        ParserState addPS = null;
        ParserState curPS = null;
        ArrayList<ParserState> prevStates = null;
        while (iter.hasNext()) {
            curPS = iter.next();
            if (sameNextCat(completedSymbol, curPS)) {
                addPS = new ParserState(curPS.getRuleNum(),
                        curPS.getDotPos() + 1,
                        curPS.getFromPos(),
                        ps.getUptoPos(),
                        curPS.getCFGRule());
                addPS.addPreviousStatesOf(curPS);
                addPS.addPreviousState(ps);
                if (chartUpdates == null) {
                    chartUpdates = new ArrayList<ParserState>();
                }
                chartUpdates.add(addPS);
            }
        }
        return chartUpdates;
    }

    private void fillInChartOnceAt(ArrayList<CFGSymbol> words, int pos) {
        // Set both predictor and completer modification flags to false;
        mPredictorChartModifiedFlag = false;
        mCompleterChartModifiedFlag = false;
        ArrayList<ParserState> parseStates = mChart.get(pos);
        if (parseStates == null) {
            return;
        }
        Iterator<ParserState> iter = parseStates.iterator();
        Iterator<ParserState> innerIter = null;
        ParserState curPS = null;
        ArrayList<ParserState> predictedUpdates = null;
        ArrayList<ParserState> scanUpdates = null;
        ArrayList<ParserState> completedUpdates = null;
        ArrayList<ParserState> currentPredictedUpdates = null;
        ArrayList<ParserState> currentScanUpdates = null;
        ArrayList<ParserState> currentCompletedUpdates = null;
        while (iter.hasNext()) {
            curPS = iter.next();
            if ( debugFlagOn() )
                System.out.println("curPS == " + curPS.toString());
            if (!curPS.isComplete()) {
                if ( debugFlagOn() ) System.out.println("state not complete");
                if (!mCFG.isPartOfSpeech(curPS.nextCat())) {
                    if ( debugFlagOn() ) System.out.println("not part of speech");
                    currentPredictedUpdates = predict(curPS);
                    // if there are predicted updates, add them
                    // to the result predicted updates.
                    if (currentPredictedUpdates != null) {
                        if (predictedUpdates == null) {
                            predictedUpdates = new ArrayList<ParserState>();
                        }
                        innerIter = currentPredictedUpdates.iterator();
                        while (innerIter.hasNext()) {
                            predictedUpdates.add(innerIter.next());
                        }
                    }
                } else {
                    currentScanUpdates = scan(curPS, words);
                    // if there are current scan updates,
                    // add them to the result predicted updates.
                    if (currentScanUpdates != null) {
                        if (scanUpdates == null) {
                            scanUpdates = new ArrayList<ParserState>();
                        }
                        innerIter = currentScanUpdates.iterator();
                        while (innerIter.hasNext()) {
                            scanUpdates.add(innerIter.next());
                        }
                    }
                }
            } else {
                currentCompletedUpdates = complete(curPS);
                // if there are current completed updates,
                // add them to the result completed updates.
                if (currentCompletedUpdates != null) {
                    if (completedUpdates == null) {
                        completedUpdates = new ArrayList<ParserState>();
                    }
                    innerIter = currentCompletedUpdates.iterator();
                    while (innerIter.hasNext()) {
                        completedUpdates.add(innerIter.next());
                    }
                }
            }
        }
        if (predictedUpdates != null) {
            addPredictedUpdatesToChart(predictedUpdates);
        }
        if (scanUpdates != null) {
            addScannedUpdatesToChart(scanUpdates);
        }
        if (completedUpdates != null) {
            addCompletedUpdatesToChart(completedUpdates);
        }
    }

    private void fillInChartAt(ArrayList<CFGSymbol> words, int pos) {
        fillInChartOnceAt(words, pos);
        while (isPredictorChartModified() ||
                isCompleterChartModified()) {
            fillInChartOnceAt(words, pos);
            if ( debugFlagOn() ) {
                System.out.println();
                displayChart();
            }
        }
    }

    private boolean isPredictorChartModified() {
        return mPredictorChartModifiedFlag;
    }

    private boolean isCompleterChartModified() {
        return mCompleterChartModifiedFlag;
    }

    private void parseCFGSymbols(ArrayList<CFGSymbol> input) {
        for (int i = 0; i <= input.size(); i++) {
            fillInChartAt(input, i);
        }
    }

    /*
     * a parser state is final iff
     * 1. fromPos == 0 and uptoPos == inputLen
     * 2. the rule's right-hand side has been completed
     * 3. the left hand side of the state's rule is the start symbol
     * of the grammar.
     */
    private boolean isFinal(ParserState ps, int inputLen) {
        return (ps.mInputFromPos == 0 &&
                ps.mUptoPos == inputLen &&
                ps.mDotPos == ps.mTrackedRule.mRHS.size() &&
                ps.mTrackedRule.mLHS.isEqual(mCFG.getStartSymbol()));
    }

    /*
     * the input is accepted when there is at least one final state
     * in the last row of the chart.
     */
    private boolean isInputAccepted(int inputLen) {
        if ( mChart == null ) return false;
        ArrayList<ParserState> lastColumn = mChart.get(inputLen);
        if ( lastColumn == null ) return false;
        Iterator<ParserState> iter = lastColumn.iterator();
        ParserState ps = null;
        while ( iter.hasNext() ) {
            ps = iter.next();
            if ( isFinal(ps, inputLen) ) {
                return true;
            }
        }
        return false;
    }

    private ArrayList<ParseTree> getAllParseTrees(ArrayList<CFGSymbol> words) {
        parseCFGSymbols(words);
        if ( !isInputAccepted(words.size()) )
            return null;

        ArrayList<ParserState> pstates = getFinalStates(words.size());
        ArrayList<ParseTree> ptrees = new ArrayList<ParseTree>();
        Iterator<ParserState> iter = pstates.iterator();
        while ( iter.hasNext() ) {
            ptrees.add(toParseTree(iter.next()));
        }
        return ptrees;
    }

    public ArrayList<ParseTree> parse(String input) {
        return getAllParseTrees((splitStringIntoSymbols(input)));
    }

    public ArrayList<ParseTree> parse(ArrayList<String> input) {
        ArrayList<CFGSymbol> symbols = new ArrayList<CFGSymbol>();
        if ( input == null ) {
            return getAllParseTrees(symbols);
        }
        else {
            Iterator<String> iter = input.iterator();
            while ( iter.hasNext() ) {
                symbols.add(new CFGSymbol(iter.next()));
            }
            return getAllParseTrees(symbols);
        }
    }

    public void displayParseTrees(ArrayList<ParseTree> ptrees) {
        if ( ptrees == null ) {
            System.out.println("no parse trees");
            return;
        }

        Iterator<ParseTree> iter = ptrees.iterator();
        while ( iter.hasNext() )  {
            System.out.println("Parse Tree:");
            iter.next().display();
            System.out.println();
        }
    }

    public static Parser factory(String grammarFilePath, int inputLength) {
        CFGrammar cfg = new CFGrammar(grammarFilePath);
        Parser eparser = new Parser(cfg);
        eparser.initChart(inputLength);
        return eparser;
    }

    private ArrayList<ParserState> getFinalStates(int inputLen) {
        if ( mChart == null ) return null;
        ArrayList<ParserState> lastColumn = mChart.get(inputLen);
        if ( lastColumn == null ) return null;
        Iterator<ParserState> iter = lastColumn.iterator();
        ArrayList<ParserState> finalStates = new ArrayList<ParserState>();
        ParserState ps = null;
        while ( iter.hasNext() ) {
            ps = iter.next();
            if ( isFinal(ps, inputLen) ) {
                finalStates.add(ps);
            }
        }
        
        System.out.println("FINAL PARSER STATES:");
        Iterator<ParserState> it = finalStates.iterator();
        ParserState currentPS = null; 
        while ( it.hasNext() ) {
            currentPS = it.next();
            System.out.println(currentPS.toString());
        }
        
        return finalStates;
    }

    private ParseTree toParseTree(ParserState ps) {
        if ( ps == null ) return null;
        ParseTree pt = new ParseTree(ps.getCFGRule());
        if ( ps.mParentParserStates.isEmpty() ) return pt;
        Iterator<ParserState> iter = ps.mParentParserStates.iterator();
        while ( iter.hasNext() ) {
            pt.addChild(toParseTree(iter.next()));
        }
        return pt;
    }

   static void parse_example_01() {
        System.err.println("======== parse_example_01 ===========");
        String gfPath = "sample_grammar.txt";
        Parser epr = Parser.factory(gfPath, 3);
        ArrayList<ParseTree> ptrees = epr.parse("book that flight");
        epr.displayChart();
        epr.displayParseTrees(ptrees);
    }

    static void parse_example_02() {
        System.err.println("======== parse_example_02 ===========");
        String gfPath = "ambiguous_grammar.txt";
        Parser epr = Parser.factory(gfPath, 5);
        ArrayList<ParseTree> ptrees = epr.parse("a - b * c");
        epr.displayChart();
        epr.displayParseTrees(ptrees);
    }

    static void parse_example_03() {
        System.err.println("======== parse_example_03 ===========");
        String gfPath = "matched_paren_grammar.txt";
        Parser epr = Parser.factory(gfPath, 4);
        ArrayList<ParseTree> ptrees = epr.parse("( ( ) )");
        epr.displayParseTrees(ptrees);
    }

    static void parse_example_04() {
        System.err.println("======== parse_example_04 ===========");
        String gfPath = "matched_paren_grammar.txt";
        Parser epr = Parser.factory(gfPath, 2);
        ArrayList<ParseTree> ptrees = epr.parse("( )");
        epr.displayParseTrees(ptrees);
    }

    static void parse_example_05() {
        System.err.println("======== parse_example_05 ===========");
        String gfPath = "left_rec_grammar.txt";
        Parser epr = Parser.factory(gfPath, 4);
        ArrayList<ParseTree> ptrees = epr.parse("a a a a");
        epr.displayParseTrees(ptrees);
    }
    
    static void parse_example_06() {
        System.err.println("======== parse_example_06 ===========");
        System.err.println("parse_example_07");
        String gfPath = "ambiguous_grammar.txt";
        Parser epr = Parser.factory(gfPath, 7);
        ArrayList<ParseTree> ptrees = epr.parse("( a - b ) * c");
        epr.displayParseTrees(ptrees);
        epr.displayChart();
    }
    
    static void parse_example_07() {
        System.err.println("======== parse_example_07 ===========");
        String gfPath = "ambiguous_grammar.txt";
        Parser epr = Parser.factory(gfPath, 3);
        ArrayList<ParseTree> ptrees = epr.parse("a * b");
        epr.displayParseTrees(ptrees);
        epr.displayChart();
    }
    
    static void test_parser(String grammar_file_path, String input, int chart_len) {
        Parser epr = Parser.factory(grammar_file_path, chart_len);
        ArrayList<ParseTree> ptrees = epr.parse(input);
        epr.displayParseTrees(ptrees);
        //epr.displayChart();
    }

    // some examples
    public static void main(String[] args) {
       //test_parser("matched_paren_grammar.txt", "( ( ) ( ) )", 6);
       //test_parser("matched_paren_grammar_2.txt", "( ( ) ( ) )", 6);
       //test_parser("matched_paren_grammar_2.txt", "( ( ( ) ( ) ) ( ( ) ) )", 12);
       //test_parser("ambiguous_grammar.txt", "a - b * c", 5);
       test_parser("flight_grammar.txt", "does this flight include a meal", 6);
       //test_parser("flight_grammar.txt", "will this flight include a meal", 6);
       //test_parser("flight_grammar.txt", "does TWA fly to Houston", 5);
    }

}
