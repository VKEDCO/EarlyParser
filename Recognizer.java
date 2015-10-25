package org.vkedco.nlp.earlyparser;

import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 * @author vladimir kulyukin
 */
public class Recognizer {

    private CFGrammar cfg = null;
    private ArrayList<ArrayList<RecognizerState>> chart = null;
    private boolean predictorChartModifiedFlag = false;
    private boolean completerChartModifiedFlag = false;
    private static CFGSymbol dummySymbol = new CFGSymbol("***lambda***");

    public Recognizer(CFGrammar cfg, int chartSize) {
        this.cfg = cfg;
        initChart(chartSize);
    }

    // displays the context free grammar the recognizer uses
    // to accept/reject the inputs.
    public void displayGrammar() {
        cfg.displayGrammar();
    }

    // display the entire chart.
    public void displayChart() {
        if (chart != null) {
            Iterator<ArrayList<RecognizerState>> outerIter = chart.iterator();
            Iterator<RecognizerState> innerIter = null;
            ArrayList<RecognizerState> curRecStates = null;
            int chartCounter = 0;
            while (outerIter.hasNext()) {
                curRecStates = outerIter.next();
                if (curRecStates != null) {
                    System.out.println("-----------------");
                    System.out.println(chartCounter);
                    innerIter = curRecStates.iterator();
                    while (innerIter.hasNext()) {
                        System.out.println(innerIter.next().toString());
                    }
                    System.out.println("-----------------");
                    chartCounter++;
                }
            }
        }
    }

    // get the element at chart[i, j].
    public RecognizerState getChartElem(int i, int j) {
        if (i < chart.size()) {
            ArrayList<RecognizerState> rstates = chart.get(i);
            if (j < rstates.size()) {
                return rstates.get(j);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private void initChart(int chartSize) {
        this.chart = new ArrayList<ArrayList<RecognizerState>>();
        for (int i = 0; i <= chartSize; i++) {
            this.chart.add(new ArrayList<RecognizerState>());
        }
        addDummyRecognizerState();
    }

    // check if the RecognizerState rs in in chart[pos].
    private boolean isInChart(RecognizerState rs, int pos) {
        if (chart == null) {
            return false;
        }
        if (pos > chart.size() - 1) {
            return false;
        }
        ArrayList<RecognizerState> rStates = chart.get(pos);
        if (rStates.size() == 0) {
            return false;
        }
        Iterator<RecognizerState> iter = rStates.iterator();
        while (iter.hasNext()) {
            if (rs.isEqual(iter.next())) {
                return true;
            }
        }
        return false;
    }

    // this method is used by addScannedUpdates to chart.
    // the scanner does not modify the chart at a given pos.
    // it always adds to a postion in the chart to the right of pos.
    private void addToChart(RecognizerState rs, int pos) {
        if (pos >= chart.size()) {
            return;
        }
        if (!isInChart(rs, pos)) {
            System.out.println("Adding " + rs.toString() + " at " + pos);
            chart.get(pos).add(rs);
        }
    }

    // predictorChartModified flag is set only if this
    // method adds a new parse state at a particular position.
    private void addPredictedUpdateToChart(RecognizerState ps, int pos) {
        if (pos >= chart.size()) {
            return;
        }
        if (!isInChart(ps, pos)) {
            System.out.println("Adding " + ps.toString() + " at " + pos);
            chart.get(pos).add(ps);
            predictorChartModifiedFlag = true;
        }
    }

    // completerChartModified flag is set only if this method
    // adds a new parse state at a given position.
    private void addCompletedUpdateToChart(RecognizerState rs, int pos) {
        if (pos >= chart.size()) {
            return;
        }
        if (!isInChart(rs, pos)) {
            System.out.println("Adding " + rs.toString() + " at " + pos);
            chart.get(pos).add(rs);
            completerChartModifiedFlag = true;
        }
    }

    private void addPredictedUpdatesToChart(ArrayList<RecognizerState> updates) {
        if (updates == null) {
            return;
        }
        Iterator<RecognizerState> iter = updates.iterator();
        RecognizerState rs = null;
        while (iter.hasNext()) {
            rs = iter.next();
            addPredictedUpdateToChart(rs, rs.getUptoPos());
        }
    }

    private void addCompletedUpdatesToChart(ArrayList<RecognizerState> updates) {
        if (updates == null) {
            return;
        }
        Iterator<RecognizerState> iter = updates.iterator();
        RecognizerState rs = null;
        while (iter.hasNext()) {
            rs = iter.next();
            addCompletedUpdateToChart(rs, rs.getUptoPos());
        }
    }

    private void addScannedUpdatesToChart(ArrayList<RecognizerState> updates) {
        if (updates == null) {
            return;
        }
        Iterator<RecognizerState> iter = updates.iterator();
        RecognizerState rs = null;
        while (iter.hasNext()) {
            rs = iter.next();
            addToChart(rs, rs.getUptoPos());
        }
    }

    private boolean sameNextCat(CFGSymbol s, RecognizerState rs) {
        return s.isEqual(rs.nextCat());
    }

    // RecognizerState(int ruleNum, int dotPos, int fromPos, int uptoPos, CFGRule r)
    private void addDummyRecognizerState() {
        CFGSymbol dlhs = dummySymbol;
        ArrayList<CFGSymbol> drhs = new ArrayList<CFGSymbol>();
        drhs.add(cfg.getStartSymbol());
        CFProduction dr = new CFProduction(dlhs, drhs);
        dr.mID = -1;
        RecognizerState dummyRecState = new RecognizerState(-1, 0, 0, 0, dr);
        CFProduction r = dummyRecState.getCFGRule();
        addToChart(dummyRecState, 0);
    }

    // there is no need to have a chartupdate class.
    // all new states should be added at their upto positions.
    // In other words, addPS.getUptoPos().
    private ArrayList<RecognizerState> predict(RecognizerState rs) {
        System.out.println("Predicting on " + rs.toString());
        ArrayList<RecognizerState> chartUpdates = null;
        System.out.println("Getting rules for " + rs.nextCat().toString());
        ArrayList<CFProduction> rules = cfg.getRulesForLHS(rs.nextCat());
        if (rules == null) {
            System.out.println("no rules found");
            return null;
        } else {
            System.out.println(rules.size() + " rules found");
        }
        Iterator<CFProduction> iter = rules.iterator();
        RecognizerState addRS = null;
        CFProduction curRule = null;
        while (iter.hasNext()) {
            curRule = iter.next();
            addRS = new RecognizerState(curRule.mID, 0, rs.getUptoPos(),
                    rs.getUptoPos(), curRule);
            if (chartUpdates == null) {
                chartUpdates = new ArrayList<RecognizerState>();
            }
            chartUpdates.add(addRS);
        }
        System.out.println(chartUpdates.size() + " updates generated");
        return chartUpdates;
    }

    // Have scanner return a list of updates.
    private ArrayList<RecognizerState> scan(RecognizerState rs, ArrayList<CFGSymbol> words) {
        System.out.println("Scanning " + rs.toString());
        if (rs.getUptoPos() >= words.size()) {
            return null;
        }

        ArrayList<RecognizerState> updates = null;
        //CFGSymbol sj = words.get(ps.getUptoPos());
        CFGSymbol var = rs.nextCat();
        CFGSymbol term = words.get(rs.getUptoPos());
        if (cfg.isInPartsOfSpeech(var, term)) {
            RecognizerState addRS = null;
            // I need to find the rule ps.NextCat() ::= words.get(ps.getUptoPos());
            CFProduction rule = cfg.getPartOfSpeechRule(var, term);
            if (rule == null) {
                System.err.println("Rule " + var + " ::= " + term +
                        " is not in grammar");
                return null;
            }
            addRS = new RecognizerState(rule.mID, 1,
                    rs.getUptoPos(), rs.getUptoPos() + 1, rule);
            //addToChart(addPS, ps.getUptoPos()+1);
            System.out.println("Scanner adding " + addRS.toString());
            updates = new ArrayList<RecognizerState>();
            updates.add(addRS);
        }
        return updates;
    }

    // RecognizerState(int ruleNum, int dotPos, int fromPos, int uptoPos, CFGRule r)
    private ArrayList<RecognizerState> complete(RecognizerState rs) {
        System.out.println("Completing " + rs.toString());
        ArrayList<RecognizerState> chartUpdates = null;
        ArrayList<RecognizerState> parseStates = chart.get(rs.getFromPos());
        Iterator<RecognizerState> iter = parseStates.iterator();
        CFGSymbol completedSymbol = rs.getCFGRule().mLHS;
        RecognizerState addRS = null;
        RecognizerState curRS = null;
        while (iter.hasNext()) {
            curRS = iter.next();
            if (sameNextCat(completedSymbol, curRS)) {
                addRS = new RecognizerState(curRS.getRuleNum(),
                        curRS.getDotPos() + 1,
                        curRS.getFromPos(),
                        rs.getUptoPos(),
                        curRS.getCFGRule());
                if (chartUpdates == null) {
                    chartUpdates = new ArrayList<RecognizerState>();
                }
                chartUpdates.add(addRS);
            }
        }
        return chartUpdates;
    }

    private void fillInChartOnceAt(ArrayList<CFGSymbol> words, int pos) {
        // Set both predictor and completer modification flags to false;
        predictorChartModifiedFlag = false;
        completerChartModifiedFlag = false;
        ArrayList<RecognizerState> recStates = chart.get(pos);
        if (recStates == null) {
            return;
        }
        Iterator<RecognizerState> iter = recStates.iterator();
        Iterator<RecognizerState> innerIter = null;
        RecognizerState curRS = null;
        ArrayList<RecognizerState> predictedUpdates = null;
        ArrayList<RecognizerState> scanUpdates = null;
        ArrayList<RecognizerState> completedUpdates = null;
        ArrayList<RecognizerState> currentPredictedUpdates = null;
        ArrayList<RecognizerState> currentScanUpdates = null;
        ArrayList<RecognizerState> currentCompletedUpdates = null;
        while (iter.hasNext()) {
            curRS = iter.next();
            System.out.println("curPS == " + curRS.toString());
            if (!curRS.isComplete()) {
                //System.out.println("state not complete");
                if (!cfg.isPartOfSpeech(curRS.nextCat())) {
                    //System.out.println("not part of speech");
                    currentPredictedUpdates = predict(curRS);
                    // if there are predicted updates, add them
                    // to the result predicted updates.
                    if (currentPredictedUpdates != null) {
                        if (predictedUpdates == null) {
                            predictedUpdates = new ArrayList<RecognizerState>();
                        }
                        innerIter = currentPredictedUpdates.iterator();
                        while (innerIter.hasNext()) {
                            predictedUpdates.add(innerIter.next());
                        }
                    }
                } else {
                    currentScanUpdates = scan(curRS, words);
                    // if there are current scan updates,
                    // add them to the result predicted updates.
                    if (currentScanUpdates != null) {
                        if (scanUpdates == null) {
                            scanUpdates = new ArrayList<RecognizerState>();
                        }
                        innerIter = currentScanUpdates.iterator();
                        while (innerIter.hasNext()) {
                            scanUpdates.add(innerIter.next());
                        }
                    }
                }
            } else {
                currentCompletedUpdates = complete(curRS);
                // if there are current completed updates,
                // add them to the result completed updates.
                if (currentCompletedUpdates != null) {
                    if (completedUpdates == null) {
                        completedUpdates = new ArrayList<RecognizerState>();
                    }
                    innerIter = currentCompletedUpdates.iterator();
                    while (innerIter.hasNext()) {
                        completedUpdates.add(innerIter.next());
                    }
                }
            }
        }
        if (predictedUpdates != null) {
            //System.out.println("predictedUpdates not null");
            addPredictedUpdatesToChart(predictedUpdates);
        //predictorChartModifiedFlag = true;
        }
        if (scanUpdates != null) {
            addScannedUpdatesToChart(scanUpdates);
        }
        if (completedUpdates != null) {
            addCompletedUpdatesToChart(completedUpdates);
        //completerChartModifiedFlag = true;
        }
    }

    private void fillInChartAt(ArrayList<CFGSymbol> words, int pos) {
        fillInChartOnceAt(words, pos);
        while (isPredictorChartModified() ||
                isCompleterChartModified()) {
            fillInChartOnceAt(words, pos);
            System.out.println();
            displayChart();
        }
    }

    private boolean isPredictorChartModified() {
        return predictorChartModifiedFlag;
    }

    private boolean isCompleterChartModified() {
        return completerChartModifiedFlag;
    }

    private void processCFGSymbols(ArrayList<CFGSymbol> input) {
        for (int i = 0; i <= input.size(); i++) {
            fillInChartAt(input, i);
        }
    }

    /*
     * a recognizer state is final iff
     * 1. fromPos == 0 and uptoPos == inputLen
     * 2. the rule's right-hand side has been completed
     * 3. the left hand side of the state's rule is the start symbol
     * of the grammar.
     */
    private boolean isFinal(RecognizerState ps, int inputLen) {
        return (ps.mInputFromPos == 0 &&
                ps.mUptoPos == inputLen &&
                ps.mDotPos == ps.mTrackedRule.mRHS.size() &&
                ps.mTrackedRule.mLHS.isEqual(cfg.getStartSymbol()));
    }

    /*
     * the input is accepted when there is at least one final state
     * in the last row of the chart.
     */
    private boolean isInputAccepted(int inputLen) {
        if ( chart == null ) return false;
        ArrayList<RecognizerState> lastColumn = chart.get(inputLen);
        if ( lastColumn == null ) return false;
        Iterator<RecognizerState> iter = lastColumn.iterator();
        RecognizerState rs = null;
        while ( iter.hasNext() ) {
            rs = iter.next();
            if ( isFinal(rs, inputLen) ) {
                return true;
            }
        }
        return false;
    }

    public static Recognizer factory(String grammarFilePath, int inputLength) {
        CFGrammar cfg = new CFGrammar(grammarFilePath);
        Recognizer rec = new Recognizer(cfg, inputLength);
        return rec;
    }

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

    public boolean recognize(String input) {
        ArrayList<CFGSymbol> symbols = splitStringIntoSymbols(input);
        processCFGSymbols(symbols);
        return isInputAccepted(symbols.size());
    }

    public boolean recognize(ArrayList<String> input) {
        ArrayList<CFGSymbol> symbols = new ArrayList<CFGSymbol>();
        if ( input == null ) {
            processCFGSymbols(symbols);
        }
        else {
            Iterator<String> iter = input.iterator();
            while ( iter.hasNext() ) {
                symbols.add(new CFGSymbol(iter.next()));
            }
            processCFGSymbols(symbols);
        }
        return isInputAccepted(symbols.size());
    }


    // ==================== TESTS ===================================
    static void recognize_test_01() {
        CFGSymbol s1 = new CFGSymbol("book");
        CFGSymbol s2 = new CFGSymbol("that");
        CFGSymbol s3 = new CFGSymbol("flight");
        ArrayList<CFGSymbol> words = new ArrayList<CFGSymbol>();
        words.add(s1);
        words.add(s2);
        words.add(s3);
        String gfPath = "flight_grammar.txt";
        Recognizer erc = Recognizer.factory(gfPath, words.size());
        erc.displayChart();
    }

    static void recognize_test_02() {
        CFGSymbol s1 = new CFGSymbol("book");
        CFGSymbol s2 = new CFGSymbol("that");
        CFGSymbol s3 = new CFGSymbol("flight");
        ArrayList<CFGSymbol> words = new ArrayList<CFGSymbol>();
        words.add(s1);
        words.add(s2);
        words.add(s3);
        String gfPath = "flight_grammar.txt";
        Recognizer erc = Recognizer.factory(gfPath, words.size());
        //epr.displayChart();
        erc.fillInChartOnceAt(words, 0);
        erc.displayChart();
    }

    static void recognize_test_03() {
        CFGSymbol s1 = new CFGSymbol("book");
        CFGSymbol s2 = new CFGSymbol("that");
        CFGSymbol s3 = new CFGSymbol("flight");
        ArrayList<CFGSymbol> words = new ArrayList<CFGSymbol>();
        words.add(s1);
        words.add(s2);
        words.add(s3);
        String gfPath = "flight_grammar.txt";
        Recognizer epr = Recognizer.factory(gfPath, words.size());
        //epr.displayChart();
        epr.fillInChartOnceAt(words, 0);
        while (epr.isPredictorChartModified() ||
                epr.isCompleterChartModified()) {
            epr.fillInChartOnceAt(words, 0);
            System.out.println();
            epr.displayChart();
        }
        epr.displayChart();
    }

   static void recognize_test_04() {
        CFGSymbol s1 = new CFGSymbol("book");
        CFGSymbol s2 = new CFGSymbol("that");
        CFGSymbol s3 = new CFGSymbol("flight");
        ArrayList<CFGSymbol> words = new ArrayList<CFGSymbol>();
        words.add(s1);
        words.add(s2);
        words.add(s3);
        String gfPath = "flight_grammar.txt";
        Recognizer epr = Recognizer.factory(gfPath, words.size());
    //RecognizerState ps = new RecognizerState();
    //epr.cfg.getRulesForLHS(s3)
    }

    static void recognize_test_05() {
        CFGSymbol s1 = new CFGSymbol("book");
        CFGSymbol s2 = new CFGSymbol("that");
        CFGSymbol s3 = new CFGSymbol("flight");
        ArrayList<CFGSymbol> words = new ArrayList<CFGSymbol>();
        words.add(s1);
        words.add(s2);
        words.add(s3);
        String gfPath = "flight_grammar.txt";
        Recognizer epr = Recognizer.factory(gfPath, words.size());
        //epr.displayChart();
        epr.fillInChartAt(words, 0);
        System.out.println("Chart at 0:");
        epr.displayChart();
        epr.fillInChartAt(words, 1);
        System.out.println("Chart at 1:");
        epr.displayChart();
        //System.out.println(epr.getChartElem(1, 0).isComplete());
        epr.fillInChartAt(words, 2);
        System.out.println("Chart at 2:");
        epr.displayChart();
        // This does not work. I get ArrayOutOfBounds. I need to
        // fix it. The scanner should be checking for that.
        epr.fillInChartAt(words, 3);
        System.out.println("Chart at 3:");
        epr.displayChart();
    }

    static void recognize_test_06() {
        CFGSymbol s1 = new CFGSymbol("book");
        CFGSymbol s2 = new CFGSymbol("that");
        CFGSymbol s3 = new CFGSymbol("flight");
        ArrayList<CFGSymbol> words = new ArrayList<CFGSymbol>();
        words.add(s1);
        words.add(s2);
        words.add(s3);
        String gfPath = "flight_grammar.txt";
        Recognizer epr = Recognizer.factory(gfPath, words.size());
        epr.processCFGSymbols(words);
        epr.displayChart();
    }

    static void recognize_test_07() {
        CFGSymbol s1 = new CFGSymbol("include");
        CFGSymbol s2 = new CFGSymbol("a");
        CFGSymbol s3 = new CFGSymbol("meal");
        ArrayList<CFGSymbol> words = new ArrayList<CFGSymbol>();
        words.add(s1);
        words.add(s2);
        words.add(s3);
        String gfPath = "flight_grammar.txt";
        Recognizer epr = Recognizer.factory(gfPath, words.size());
        epr.processCFGSymbols(words);
        epr.displayChart();
    }

    static void recognize_test_08() {
        CFGSymbol s1 = new CFGSymbol("does");
        CFGSymbol s2 = new CFGSymbol("this");
        CFGSymbol s3 = new CFGSymbol("flight");
        CFGSymbol s4 = new CFGSymbol("include");
        CFGSymbol s5 = new CFGSymbol("a");
        CFGSymbol s6 = new CFGSymbol("meal");
        ArrayList<CFGSymbol> words = new ArrayList<CFGSymbol>();
        words.add(s1);
        words.add(s2);
        words.add(s3);
        words.add(s4);
        words.add(s5);
        words.add(s6);
        String gfPath = "flight_grammar.txt";
        Recognizer epr = Recognizer.factory(gfPath, words.size());
        epr.processCFGSymbols(words);
        epr.displayChart();
    }

    static void recognize_test_09() {
        CFGSymbol s1 = new CFGSymbol("a");
        CFGSymbol s2 = new CFGSymbol("*");
        CFGSymbol s3 = new CFGSymbol("a");
        CFGSymbol s4 = new CFGSymbol("+");
        CFGSymbol s5 = new CFGSymbol("a");
        CFGSymbol s6 = new CFGSymbol("*");
        CFGSymbol s7 = new CFGSymbol("a");
        ArrayList<CFGSymbol> words = new ArrayList<CFGSymbol>();
        words.add(s1);
        words.add(s2);
        words.add(s3);
        words.add(s4);
        words.add(s5);
        words.add(s6);
        words.add(s7);
        String gfPath = "early_grammar.txt";
        Recognizer epr = Recognizer.factory(gfPath, words.size());
        epr.displayGrammar();
        epr.processCFGSymbols(words);
        epr.displayChart();
    }

    static void recognize_test_10() {
        CFGSymbol s1 = new CFGSymbol("a");
        CFGSymbol s2 = new CFGSymbol("-");
        CFGSymbol s3 = new CFGSymbol("b");
        CFGSymbol s4 = new CFGSymbol("*");
        CFGSymbol s5 = new CFGSymbol("c");
        ArrayList<CFGSymbol> words = new ArrayList<CFGSymbol>();
        words.add(s1);
        words.add(s2);
        words.add(s3);
        words.add(s4);
        words.add(s5);
        String gfPath = "ambiguous_grammar.txt";
        Recognizer epr = Recognizer.factory(gfPath, words.size());
        epr.displayGrammar();
        epr.processCFGSymbols(words);
        epr.displayChart();
        System.out.println(epr.isInputAccepted(words.size()));
    }

    static void recognize_test_11() {
        CFGSymbol s1 = new CFGSymbol("(");
        CFGSymbol s2 = new CFGSymbol(")");
        ArrayList<CFGSymbol> words = new ArrayList<CFGSymbol>();
        words.add(s1);
        words.add(s2);
        String gfPath = "matched_paren_grammar.txt";
        Recognizer epr = Recognizer.factory(gfPath, words.size());
        epr.displayGrammar();
        epr.processCFGSymbols(words);
        epr.displayChart();
        System.out.println(epr.isInputAccepted(words.size()));
    }

    static void recognize_example_01() {
        String gfPath = "matched_paren_grammar.txt";
        Recognizer epr = Recognizer.factory(gfPath, 2);
        epr.displayGrammar();
        epr.recognize("( )");
        epr.displayChart();
        System.out.println(epr.isInputAccepted(2));
    }
    

    static void recognize_example_02() {
        String gfPath = "ambiguous_grammar.txt";
        Recognizer epr = Recognizer.factory(gfPath, 5);
        epr.recognize("a - b * c");
        epr.displayChart();
        System.out.println(epr.isInputAccepted(5));
    }
    
    static void test_recognizer(String grammar_file_path, String input, int chart_len) {
        Recognizer epr = Recognizer.factory(grammar_file_path, chart_len);
        epr.displayGrammar();
        epr.recognize(input);
        epr.displayChart();
        System.out.println(epr.isInputAccepted(chart_len));
    }

    public static void main(String[] args) {
        //test_recognizer("matched_paren_grammar.txt", "( ( ) ( ) )", 6);
        //test_recognizer("matched_paren_grammar_2.txt", "( ( ) ( ) )", 6);
        //test_recognizer("matched_paren_grammar_2.txt", "( ( ( ) ( ) ) ( ( ) ) )", 12);
        //test_recognizer("ambiguous_grammar.txt", "a - b * c", 5);
        //test_recognizer("flight_grammar.txt", "does this flight include a meal", 6);
        //test_recognizer("flight_grammar.txt", "will this flight include a meal", 6);
        //test_recognizer("flight_grammar.txt", "does TWA fly to Houston", 5);
    }
}
