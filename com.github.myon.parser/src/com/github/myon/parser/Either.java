package com.github.myon.parser;

import java.util.Objects;

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

		@Override
		public boolean equals(final Object other) {
			if (other instanceof First) {
				final First<?, ?> that = (First<?, ?>) other;
				return Objects.equals(that.value, this.value);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.value);
		}

		@Override
		public String toString() {
			return this.value.toString()+"<";
		}
	}

	private static class Second<A,B> extends Either<A, B> {
		private final B value;
		public Second(final B value) {
			this.value = value;
		}

		@Override
		public boolean equals(final Object other) {
			if (other instanceof Second) {
				final Second<?, ?> that = (Second<?, ?>) other;
				return Objects.equals(that.value, this.value);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.value);
		}

		@Override
		public String toString() {
			return ">"+this.value.toString();
		}
	}

}
