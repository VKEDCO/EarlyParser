import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import opennlp.tools.cmdline.PerformanceMonitor;
import opennlp.tools.cmdline.postag.POSModelLoader;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSSample;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.tokenize.WhitespaceTokenizer;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.Span;

public class OpenNLPExample {

  // change this directory
	private static final String PATH = "C:/NetBeansProjects/OpenNLPExample/model_files/";
	private static final String SENTENCE = "hello, robot. come to my office and clean it.";
        static final String PARAGRAPH = "This office space consists of four offices: office A, office B, office C,\n "
                + "and office D." 
                + " Office A is connected to office B. Office C is connected to office D.";
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			POSTag();
			//to detect sentences
			SentenceDetect();
		
			//String tokenizer
			Tokenize();
			
			//To find person Names in sentences
			//findName();
		} catch (InvalidFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void POSTag() throws IOException {
                System.out.println(PATH + "en-pos-maxent.bin");
		POSModel model = new POSModelLoader()
				.load(new File(PATH+"en-pos-maxent.bin"));
		PerformanceMonitor perfMon = new PerformanceMonitor(System.err, "sent");
		POSTaggerME tagger = new POSTaggerME(model);
 
		String input = SENTENCE;
		ObjectStream<String> lineStream = new PlainTextByLineStream(
				new StringReader(input));
 
		perfMon.start();
		String line;
		while ((line = lineStream.read()) != null) {
 
			String whitespaceTokenizerLine[] = WhitespaceTokenizer.INSTANCE
					.tokenize(line);
			String[] tags = tagger.tag(whitespaceTokenizerLine);
 
			POSSample sample = new POSSample(whitespaceTokenizerLine, tags);
			System.out.println(sample.toString());
 
			perfMon.incrementCounter();
		}
		perfMon.stopAndPrintFinalResult();
	}

	public static void SentenceDetect() throws InvalidFormatException,
	IOException {
		

		// always start with a model, a model is learned from training data
	InputStream is = new FileInputStream(PATH+"en-sent.bin");
	SentenceModel model = new SentenceModel(is);
	SentenceDetectorME sdetector = new SentenceDetectorME(model);
	
	String sentences[] = sdetector.sentDetect(SENTENCE);
	
	for(String s: sentences)
			System.out.println(s);
	
	is.close();
	}
	
	public static void Tokenize() throws InvalidFormatException, IOException {
		InputStream is = new FileInputStream(PATH+"en-token.bin");
 
		TokenizerModel model = new TokenizerModel(is);
 
		Tokenizer tokenizer = new TokenizerME(model);
 
		String tokens[] = tokenizer.tokenize(SENTENCE);
 
		for (String a : tokens)
			System.out.println(a);
 
		is.close();
	}
	
	public static void findName() throws IOException {
		InputStream isName = new FileInputStream(PATH+"en-ner-person.bin");
 
		InputStream isToken = new FileInputStream(PATH+"en-token.bin");
		 
		TokenizerModel modeltoken = new TokenizerModel(isToken);
 
		Tokenizer tokenizer = new TokenizerME(modeltoken);
		
		TokenNameFinderModel modelName = new TokenNameFinderModel(isName);
		isName.close();
		isToken.close();
 
		NameFinderME nameFinder = new NameFinderME(modelName);
 
		String tokens[] = tokenizer.tokenize(SENTENCE);
 
			Span nameSpans[] = nameFinder.find(tokens);
 
			for(Span s: nameSpans)
				System.out.println(s.toString());
 
	}
	public static void findName1() throws IOException {
		InputStream is = new FileInputStream(PATH+"en-ner-person.bin");
 
		TokenNameFinderModel model = new TokenNameFinderModel(is);
		is.close();
 
		NameFinderME nameFinder = new NameFinderME(model);
 
		String []sentence = new String[]{
			    "Mike",
			    "Smith",
			    "is",
			    "a",
			    "good",
			    "person"
			    };
 
			Span nameSpans[] = nameFinder.find(sentence);
 
			for(Span s: nameSpans)
				System.out.println(s.toString());
 
	}
}

