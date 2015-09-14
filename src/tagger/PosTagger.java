package tagger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import trainer.ProblemInstance;
import data.Alphabet;
import data.Data;
import data.OffSets;
import data.Window;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;

public class PosTagger {
	private Data data = new Data();
	private Alphabet alphabet = new Alphabet();
	private OffSets offSets = new OffSets();
	private int windowSize = 2;
	private Model model ;

	private long time1 ;
	private long time2;

	// Setters and getters

	public Data getData() {
		return data;
	}
	public void setData(Data data) {
		this.data = data;
	}
	public Alphabet getAlphabet() {
		return alphabet;
	}
	public void setAlphabet(Alphabet alphabet) {
		this.alphabet = alphabet;
	}
	public OffSets getOffSets() {
		return offSets;
	}
	public void setOffSets(OffSets offSets) {
		this.offSets = offSets;
	}
	public int getWindowSize() {
		return windowSize;
	}
	public void setWindowSize(int windowSize) {
		this.windowSize = windowSize;
	}
	public Model getModel() {
		return model;
	}
	public void setModel(Model model) {
		this.model = model;
	}

	// Init
	public PosTagger(){
	}

	// Methods

	public void initPosTagger(String modelFile, int windowSize) throws IOException{
		time1 = System.currentTimeMillis();

		System.out.println("Set window size: " + windowSize);
		this.setWindowSize(windowSize);

		System.out.println("Load feature files:");
		this.getAlphabet().loadFeaturesFromFiles();

		System.out.println("Load label set:");
		this.getData().readLabelSet();

		System.out.println("Resetting non-used variables ...");
		this.getAlphabet().clean();

		System.out.println("Initialize offsets:");
		this.getOffSets().initializeOffsets(this.getAlphabet(), this.getWindowSize());
		System.out.println("\t"+this.getOffSets().toString());

		time2 = System.currentTimeMillis();
		System.out.println("System time (msec): " + (time2-time1)+"\n");

		time1 = System.currentTimeMillis();

		System.out.println("Load model file: " + modelFile);
		this.setModel(Model.load(new File(modelFile)));

		time2 = System.currentTimeMillis();
		System.out.println("System time (msec): " + (time2-time1));
	}

	/**
	 * The same as trainer.TrainerInMem.createWindowFramesFromSentence()!
	 * @throws IOException
	 */
	private void createWindowFramesFromSentence() throws IOException {
		// for each token t_i of current training sentence do
		// System.out.println("Sentence no: " + data.getSentenceCnt());
		int mod = 100000;
		for (int i = 0; i < this.getData().getSentence().getWordArray().length; i++){
			int labelIndex = this.getData().getSentence().getLabelArray()[i];
			// create local context for tagging t_i of size 2*windowSize+1 centered around t_i

			Window tokenWindow = new Window(this.getData().getSentence(), i, windowSize, data, alphabet);
			tokenWindow.setLabelIndex(labelIndex);

			this.getData().getInstances().add(tokenWindow);

			// Print how many windows are created so far, and pretty print every mod-th window
			if ((Window.windowCnt % mod) == 0) {
				System.out.println("\n************");
				System.out.println("# Window instances: " + Window.windowCnt);
			}
		}
	}

	/**
	 * Iterate through all window frames:
	 * - create the feature vector: train=false means: handle unknown words; adjust=true: means adjust feature indices
	 * - create a problem instance -> mainly the feature vector
	 * - and call the learner with model and feature vector
	 * - save the predicted label in the corresponding field of the word in the sentence.
	 * 
	 * Mainly the same as trainer.TrainerInMem.constructProblem(train, adjust), but uses predictor
	 */
	private void constructProblemAndTag(boolean train, boolean adjust) {
		int problemCnt = 0;
		int prediction = 0;

		for (int i = 0; i < data.getInstances().size();i++){
			// For each window frame of a sentence
			Window nextWindow = data.getInstances().get(i);
			// Fill the frame with all availablel features. First boolean sets 
			// training mode to false which means that unknown words are handled.
			nextWindow.fillWindow(train, adjust);
			// Create the feature vector
			ProblemInstance problemInstance = new ProblemInstance();
			problemInstance.createProblemInstanceFromWindow(nextWindow);
			problemCnt++;

			// Call the learner to predict the label
			prediction = (int) Linear.predict(this.getModel(), problemInstance.getFeatureVector());
//			System.out.println("Word: " + this.getData().getWordSet().getNum2label().get(this.getData().getSentence().getWordArray()[i]) +
//			"\tPrediction: " + this.getData().getLabelSet().getNum2label().get(prediction));

			//Here, I am assuming that sentence length equals # of windows
			// So store predicted label i to word i
			this.getData().getSentence().getLabelArray()[i]=prediction;
			nextWindow.clean();
		}	
	}

	/**
	 * A method for tagging a singel sentence given as list of tokens.
	 * @param tokens
	 * @throws IOException
	 */
	public void tagTokens(String[] tokens) throws IOException{
		this.time1 = System.currentTimeMillis();

		// create internal sentence object
		this.getData().generateSentenceObjectFromUnlabeledTokens(tokens);

		// create window frames from sentence and store in list
		this.createWindowFramesFromSentence();

		// create feature vector instance for each window frame and tag
		this.constructProblemAndTag(false, true);

		time2 = System.currentTimeMillis();
		System.out.println("System time (msec): " + (time2-time1)+"\n");
	}

	/**
	 * A simple print out of a sentence in form of list of word/tag
	 * @return
	 */
	public String taggedSentenceToString(){
		String output ="";
		int mod = 10;
		int cnt = 0;
		for (int i=0; i < this.getData().getSentence().getWordArray().length;i++){
			output += this.getData().getWordSet().getNum2label().get(this.getData().getSentence().getWordArray()[i])+"/"+
					this.getData().getLabelSet().getNum2label().get(this.getData().getSentence().getLabelArray()[i])+" ";
			cnt++;
			if ((cnt % mod)==0) output+="\n";
		}
		return output;

	}
	
	/**
	 * For each internalized conll sentence:
	 * - compute window frames
	 * - fill windows using train=false mode
	 * - construct problem and call predictor
	 * - reset instance variable of data object, because predictor is called directly on windows of sentences
	 * @param conllReader
	 * @param max
	 * @throws IOException
	 */
	private void tagSentencesFromConllReader(BufferedReader conllReader, int max) throws IOException{
		String line = "";
		List<String[]> tokens = new ArrayList<String[]>();

		while ((line = conllReader.readLine()) != null) {
			if (line.isEmpty()) {
				// Stop if max sentences have been processed
				if  ((max > 0) && (data.getSentenceCnt() >= max)) break;
				
				// create internal sentence object and label maps
				// I use this here although labels will be late overwritten - but can u8se it in eval modus as well
				data.generateSentenceObjectFromConllLabeledSentence(tokens);

				// create window frames and store in list
				createWindowFramesFromSentence();
				
				System.out.println("In:  " + this.taggedSentenceToString());
				
				// create feature vector instance for each window frame and tag
				this.constructProblemAndTag(false, true);
				
				System.out.println("Out: " + this.taggedSentenceToString() + "\n");
				
				// reset instances - need to do this here, because learner is called directly on windows
				this.getData().setInstances(new ArrayList<Window>());

				// reset tokens
				tokens = new ArrayList<String[]>();
			}
			else {
				String[] tokenizedLine = line.split("\t");
				tokens.add(tokenizedLine);
			}
		}
		conllReader.close();
	}
	
	public void tagFromConllDevelFile(String sourceFileName, int max)
			throws IOException {
		long time1;
		long time2;

		BufferedReader conllReader = new BufferedReader(
				new InputStreamReader(new FileInputStream(sourceFileName),"UTF-8"));
		boolean train = false;
		boolean adjust = true;

		System.out.println("Do testing from file: " + sourceFileName);
		System.out.println("Train?: " + train + " Adjust?: " + adjust);

		time1 = System.currentTimeMillis();
		this.tagSentencesFromConllReader(conllReader, max);
		time2 = System.currentTimeMillis();
		System.out.println("System time (msec): " + (time2-time1));
		
		System.out.println("Offsets: " + this.getOffSets().toString());
		System.out.println("Sentences: " + this.getData().getSentenceCnt());
		System.out.println("Testing instances: " + Window.windowCnt);
		System.out.println("Approx. GB needed: " + ((ProblemInstance.cumLength/Window.windowCnt)*Window.windowCnt*8+Window.windowCnt)/1000000000.0);

	}

	public void tagSentenceTest(){
		String sentence ="This is the first call of the GNT-tagger . ";
		sentence = "Do not underestimate the effects of the Internet economy on load growth . "
				+ "I have been preaching the tremendous growth described below for the last year . "
				+ "The utility infrastructure simply can not handle these loads at the distribution level "
				+ "and ultimatley distributed generation will be required for power quality reasons . "
				+ "The City of Austin , TX has experienced 300 + MW of load growth this year due to server farms and technology companies . "
				+ "There is a 100 MW server farm trying to hook up to HL&P as we speak and "
				+ "they can not deliver for 12 months due to distribution infrastructure issues . "
				+ "Obviously , Seattle , Porltand , Boise , Denver , "
				+ "San Fran and San Jose in your markets are in for a rude awakening in the next 2 - 3 years . "
				+ "George Hopley 09/05/2000 11:41 AM Internet Data Gain Is a Major Power Drain on Local Utilities"
				+ "( September 05 , 2000 )"
				+ "In 1997 , a little - known Silicon Valley company called Exodus Communications opened a 15,000 - square - foot data center in Tukwila . "
				+ "The mission was to handle the Internet traffic and computer servers for the region 's growing number of dot - coms . "
				+ "Fast - forward to summer 2000 . "
				+ "Exodus is now wrapping up construction on a new 13 - acre , 576,000 - square - foot data center less than a mile from its original facility . "
				+ "Sitting at the confluence of several fiber optic backbones , the Exodus plant will consume "
				+ "enough power for a small town and eventually house Internet servers for firms such as Avenue A , Microsoft and Onvia.com . ";
		String[] tokens = sentence.split(" ");
		
		try {
			this.tagTokens(tokens);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("#Tokens: " + tokens.length);
		System.out.println(this.taggedSentenceToString());
	}

	public static void main(String[] args) throws IOException{
		int windowSize = 2;
		String modelFile1 = "/Users/gune00/data/wordVectorTests/testModel_L2R_LR.txt";
		String modelFile2 = "/Users/gune00/data/wordVectorTests/testModel_MCSVM_CS.txt";

		PosTagger posTagger = new PosTagger();

		posTagger.initPosTagger(modelFile2, windowSize);
		
		// posTagger.tagSentenceTest();
		
		posTagger.tagFromConllDevelFile("/Users/gune00/data/BioNLPdata/CoNLL2007/pbiotb/dev/english_pbiotb_dev.conll", -1);
		
		
	}

}
