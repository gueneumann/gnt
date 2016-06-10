package tokenize;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GntTokenizer {

	public static String[] splitTokenizer(String string){
		//System.out.println(">>>"+string+"<<<");

		String delims = "[ |\\,|\\:|\\.|\\\"|\\(|\\)|\\!|\\?]+";
		delims = "[\\W]+";

		//TODO: may have empty tokens

		String[] tokens = string.split(delims);

		final List<String> list =  new ArrayList<String>();
		Collections.addAll(list, tokens); 
		list.remove("");
		tokens = list.toArray(new String[list.size()]);
		return tokens;
	}

}