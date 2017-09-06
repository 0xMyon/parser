package com.github.myon.parser;

import java.util.List;
import java.util.Objects;

public class Token<T> {

	public final TokenType<T> type;
	private final List<T> word;


	public Token(final TokenType<T> type, final List<T> word) {
		this.type = type;
		this.word = word;
	}

	@Override
	public boolean equals(final Object other) {
		if (other instanceof Token) {
			final Token<?> that = (Token<?>) other;
			return this.type.equals(that.type) && this.word.equals(that.word);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.type, this.word);
	}

	@Override
	public String toString() {
		return this.word.toString();
	}

}
