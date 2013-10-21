
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
    
    // there is no need to have a chartupdate class.
    // all new states should be added at their upto positions.
    // In other words, addPS.getUptoPos().
    private ArrayList<ParserState> predict(ParserState ps) {
        // prediction code
        
    }

    // Have scanner return a list of updates for the chart.
    private ArrayList<ParserState> scan(ParserState ps, ArrayList<CFGSymbol> words) {
        // scanning code
        
    }


    private ArrayList<ParserState> complete(ParserState ps) {
        // completion code
           
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

    public static void main(String[] args) {
        parse_example_01();
        parse_example_02();
        parse_example_03();
        parse_example_04();
        parse_example_05();
        parse_example_06();
        parse_example_07();
    }

}

}
