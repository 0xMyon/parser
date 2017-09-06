package com.github.myon.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class InputPreperator implements Function<String, List<Character>> {

	@Override
	public List<Character> apply(final String str) {
		final List<Character> result = new ArrayList<>();
		for(int i=0;i<str.length();i++) {
			result.add(str.charAt(i));
		}
		return result;
	}

	public static final InputPreperator instance = new InputPreperator();

}
