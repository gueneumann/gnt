package gnt;

import java.io.IOException;

import caller.RunTagger;

public class TestRunPosTagger {

	public static void main(String[] args) throws IOException{
		
		RunTagger.runner(
				"src/main/resources/dataProps/EnNerTagger.xml", 
				"src/main/resources/corpusProps/EnNerTagger.xml");		
	}
}
