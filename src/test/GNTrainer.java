package test;

import java.io.IOException;

import data.ModelInfo;
import data.OffSets;
import data.Window;
import features.WordDistributedFeatureFactory;
import trainer.ProblemInstance;
import trainer.TrainerInMem;

public class GNTrainer {

	private TrainerInMem trainer;
	private long time1 ;
	private long time2;

	public TrainerInMem getTrainer() {
		return trainer;
	}
	public void setTrainer(TrainerInMem trainer) {
		this.trainer = trainer;
	}

	public GNTrainer(int dim) {
		this.trainer = new TrainerInMem(dim);
	}
	
	public GNTrainer(ModelInfo modelInfo) {
		this.trainer = new TrainerInMem(modelInfo);
	}
	
	public GNTrainer(ModelInfo modelInfo, int dim) {
		this.trainer = new TrainerInMem(modelInfo, dim);
	}

	// test methods

	private void createWordVectors(int dim) throws IOException{
		WordDistributedFeatureFactory dwvFactory = new WordDistributedFeatureFactory();

		dwvFactory.createAndWriteDistributedWordFeaturesSparse(dim);
	}

	private void gntTrainingFromConllFile(String trainingFileName, int maxExamples) throws IOException{
		System.out.println("Load feature files:");
		time1 = System.currentTimeMillis();

		this.getTrainer().getAlphabet().loadFeaturesFromFiles();
		
		System.out.println("Resetting not used storage:");
		this.getTrainer().getAlphabet().clean();

		this.getTrainer().getOffSets().initializeOffsets(this.getTrainer().getAlphabet(), this.getTrainer().getWindowSize());
		time2 = System.currentTimeMillis();
		System.out.println("System time (msec): " + (time2-time1));

		System.out.println("Create windows:");
		time1 = System.currentTimeMillis();
		
		this.getTrainer().trainFromConllTrainingFileInMemory(trainingFileName, maxExamples);

		time2 = System.currentTimeMillis();
		System.out.println("System time (msec): " + (time2-time1));

		this.getTrainer().getProblem().n = OffSets.windowVectorSize;
		this.getTrainer().getProblem().l=Window.windowCnt;
		
		System.out.println("Offsets: " + this.getTrainer().getOffSets().toString());
		System.out.println("Sentences: " + this.getTrainer().getData().getSentenceCnt());
		System.out.println("Feature instances size: " + this.getTrainer().getProblem().n);
		System.out.println("Average window vector lenght: " + ProblemInstance.cumLength/Window.windowCnt);
		System.out.println("Training instances: " + this.getTrainer().getProblem().l);
		System.out.println("Approx. GB needed: " + ((ProblemInstance.cumLength/Window.windowCnt)*Window.windowCnt*8+Window.windowCnt)/1000000000.0);
	}

	private void gntTrainingWithDimensionFromConllFile(String trainingFileName, int dim, int maxExamples) throws IOException{
		System.out.println("Create wordVectors:");
		time1 = System.currentTimeMillis();

		this.createWordVectors(dim);

		time2 = System.currentTimeMillis();
		System.out.println("System time (msec): " + (time2-time1));

		this.gntTrainingFromConllFile(trainingFileName, maxExamples);
	}
	
	public static void main(String[] args) throws IOException{
		ModelInfo modelInfo = new ModelInfo("MDP", 2);
		modelInfo.setModelFile("/Users/gune00/data/wordVectorTests/testModel100iw10k_MCSVM_CS.txt");
		GNTrainer gnTrainer = new GNTrainer(modelInfo);
		String trainingFileName = "/Users/gune00/data/MLDP/english/english-train.conll";
		int numberOfSentences = 10000;

		
		gnTrainer.gntTrainingWithDimensionFromConllFile(trainingFileName, 100, numberOfSentences);

	}

}