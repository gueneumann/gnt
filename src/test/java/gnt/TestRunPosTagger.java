package gnt;

import java.io.IOException;

import caller.RunTagger;

public class TestRunPosTagger {

	public static void main(String[] args) throws IOException{
		
		RunTagger.runner(
				"resources/models/model_DETWEETPOS_2_0iw-1sent_FTTT_MCSVM_CS.zip", 
				"src/main/resources/corpusProps/DeTweetPosTagger.xml");		
	}
}