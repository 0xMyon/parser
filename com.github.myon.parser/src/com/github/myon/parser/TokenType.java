package com.github.myon.parser;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class TokenType<T> implements Predicate<Token<T>> {

	private final Predicate<T> predicate;

	public TokenType(final Predicate<T> predicate) {
		this.predicate = predicate;
	}


	public Parser<T, Token<T>> parser() {
		return Parser.satisfy(this.predicate).whole()
				.map(s -> s.collect(Collectors.toList()))
				.filter(result -> !result.source.isEmpty()).map(this::create);
	}

	private Token<T> create(final List<T> word) {
		return new Token<>(this, word);
	}

	@Override
	public boolean equals(final Object other) {
		if (other instanceof TokenType) {
			final TokenType<?> that = (TokenType<?>) other;
			return this.predicate.equals(that.predicate);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.predicate);
	}

	@Override
	public boolean test(final Token<T> token) {
		return token.type.equals(this);
	}

}
