package com.github.myon.parser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Util {

	@SafeVarargs
	static <T> Set<T> set(final T... elements) {
		final Set<T> result = new HashSet<>();
		for(final T elememt: elements) {
			result.add(elememt);
		}
		return result;
	}

	static List<Character> string(final String str) {
		final List<Character> result = new ArrayList<>();
		for(int i=0;i<str.length();i++) {
			result.add(str.charAt(i));
		}
		return result;
	}


	@SafeVarargs
	static <T> List<T> list(final T... elements) {
		final List<T> result = new LinkedList<>();
		for(final T element : elements) {
			result.add(element);
		}
		return result;
	}


}
