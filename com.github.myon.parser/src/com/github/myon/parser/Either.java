package com.github.myon.parser;

public class Either<A, B> {

	public static <A,B> Either<A,B> first(final A value) {
		return new First<>(value);
	}

	public static <A,B> Either<A,B> second(final B value) {
		return new Second<>(value);
	}

	private static class First<A,B> extends Either<A, B> {
		private final A value;
		public First(final A value) {
			this.value = value;
		}
	}

	private static class Second<A,B> extends Either<A, B> {
		private final B value;
		public Second(final B value) {
			this.value = value;
		}
	}

}
