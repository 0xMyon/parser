package com.github.myon.parser;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.github.myon.tuple.Tuple;


/**
 *
 * A Parser converts a {@code List} of input to a {@code Set} of output.
 * The output {@code Tuple} consists of the actual result and the not processed {@code List} of inputs.
 *
 * @author 0xMyon
 *
 * @param <I> type of input symbols
 * @param <O> type of output symbols
 *
 * 1 2 3 -> OKAY -> Call current rule on current input
 * 1 3 -> MISSING -> Call next rule on current input
 * 1 1 3 -> WRONG -> Call next rule on next input
 * 1 A 2 3 -> ADDITIONAL -> Call current rule on next input
 *
 * (1,23) -> (12,3)
 * (1,3) -> (1E,3)
 * (1,13) -> (1E,3)
 * (1,A23) -> (1E,23)
 *
 *
 */
public interface Parser<I,O> extends Function<List<I>, Stream<Tuple<O,List<I>>>> {

	//TODO line info and errors

	/**
	 * @return an elementary {@link Parser} that never returns a result.
	 */
	public static <I,O> @NonNull Parser<I,O> empty() {
		return word -> Stream.empty();
	}

	/**
	 * @return an elementary {@code Parser} that succeeds with an empty result set
	 */
	public static <I,O> @NonNull Parser<I,Stream<O>> epsilon() {
		return Parser.succeed(Stream::empty);
	}


	/**
	 * @param supplier of the output
	 * @return an elementary {@link Parser} with supplied output that does not consume any input.
	 */
	public static <I,O> @NonNull Parser<I,O> succeed(
			final @NonNull Supplier<? extends O> supplier
			) {
		return word -> Stream.of(Tuple.of(supplier.get(), word));
	}

	/**
	 * @param predicate to be satisfied
	 * @return an elementary {@code Parser} that consumes the first input symbol and and returns it as output,
	 * If and only if a {@link Predicate} is satisfied. Empty result otherwise.
	 */
	public static <IO> @NonNull Parser<IO,IO> satisfy(
			final @NonNull Predicate<? super IO> predicate
			) {
		return word ->
		!word.isEmpty() && predicate.test(word.get(0)) ?
				Stream.of(Tuple.of(word.get(0), word.subList(1, word.size())))
				: Stream.empty();
	}

	public static <IO,E> @NonNull Parser<IO,Either<IO,E>> satisfy(
			final @NonNull Predicate<? super IO> predicate,
			final @NonNull E error
			) {
		return Parser.recursive(p -> word ->
		word.isEmpty() ? Stream.of(/*Unexpected end*/) : predicate.test(word.get(0)) ? // CASE 0: EMPTY
				Stream.of(Tuple.of(Either.first(word.get(0)), word.subList(1, word.size()))) // CASE 1: OKAY
				: Stream.concat(
						Stream.of(
								Tuple.of(Either.second(error), word), // CASE 2: MISSING
								Tuple.of(Either.second(error), word.subList(1, word.size())) // CASE 3: WRONG
								),
						p.apply(word.subList(1, word.size())) // CASE 4: JUMP
						)
				);
	}

	/**
	 * Unites the result set of two {@link Parser}s of the same input and output types
	 * @param that left hand {@link Parser} to be united with
	 * @return a {@code Parser} that unites the output of given {@link Parser}s
	 * @see #alternative(Parser) for different output types
	 * @see #choice(Parser...) for multiple {@link Parser}s
	 */
	public default @NonNull Parser<I,O> choice(
			final @NonNull Parser<I,? extends O> that
			) {
		return Parser.choice(this, that);
	}

	/**
	 * Unites the result set of two {@link Parser}s with different output types
	 * @param that left hand {@link Parser} to be united with
	 * @return a {@link Parser} that unites the output of given {@link Parser}s
	 * @see #choice(Parser) for same output types
	 */
	public default <T> @NonNull Parser<I,Either<O,T>> alternative(
			final @NonNull Parser<I,T> that
			) {
		return word -> Stream.concat(
				this.apply(word).map(Tuple.left(Either::first)),
				that.apply(word).map(Tuple.left(Either::second))
				);
	}

	/**
	 * Unites the result set of multiple {@link Parser}s with same input and output types
	 * @param parsers to be united
	 * @return a {@code Parser} that unites the output of given {@link Parser}s
	 * @see #choice(Parser, Parser) for binary operation
	 */
	@SafeVarargs
	public static <I,O> @NonNull Parser<I,O> choice(
			final @NonNull Parser<I, ? extends O>... parsers
			) {
		return Stream.of(parsers).reduce(
				Parser.empty(),
				(a,b) -> Parser.choice(a,b),
				(a,b) -> Parser.choice(a,b)
				);
	}

	/**
	 * Unites the result set of two {@link Parser}s with same input and output type with commun base type
	 * @param first {@link Parser} to be applied
	 * @param second {@link Parser} to be applied
	 * @return a {@link Parser} that unites the output of given {@link Parser}s
	 * @see #choice(Parser...) for any number of combination
	 * @see #choice(Parser) for object-orinated version
	 */
	public static <I,O> @NonNull Parser<I,O> choice(
			final @NonNull Parser<I, ? extends O> first,
			final @NonNull Parser<I, ? extends O> second
			) {
		return word -> Stream.concat(
				first.apply(word).map(Tuple.left(o->(O)o)),
				second.apply(word).map(Tuple.left(o->(O)o))
				);
	}

	/**
	 * Combines two parsers by user defined {@link Function}
	 * @param that second {@link Parser}
	 * @param function the binary function that is used to combine the results
	 * @return a {@link Parser} with combined output
	 */
	public default <T,R> @NonNull Parser<I,R> concat(
			final @NonNull Parser<I,T> that,
			final @NonNull BiFunction<? super O, ? super T, R> function
			) {
		return word -> this.apply(word)
				.map(a -> that.apply(a.target)
						.map(Tuple.left(s -> function.apply(a.source, s))))
				.reduce(Stream.empty(), Stream::concat);
	}

	/**
	 * Combined two parsers into a {@link Tuple}
	 * @param that second {@link Parser}
	 * @return a {@link Parser} with {@link Tuple}ed output
	 */
	public default <T> @NonNull Parser<I, Tuple<O,T>> concat(
			final @NonNull Parser<I,T> that
			) {
		return this.concat(that, Tuple::of);
	}

	/**
	 * Applies a {@code Function} to the result
	 * @param function that is applied to every single result
	 * @return a {@code Parser} with mapped output
	 */
	public default <T> @NonNull Parser<I,T> map(
			final @NonNull Function<? super O, T> function
			) {
		return word -> this.apply(word).map(Tuple.left(function));
	}

	/**
	 * Filters the result set by {@code BiPredicate}
	 * @param predicate {@code BiPredicate} to filter the result set
	 * @return a {@code Parser<I,O>} with filtered output
	 */
	public default @NonNull Parser<I,O> filter(
			final @NonNull Predicate<? super Tuple<O, List<I>>> predicate
			) {
		return word -> this.apply(word).filter(predicate);
	}

	/**
	 * Filters the result set for the most consumed result sets
	 * @return a greedy {@link Parser}
	 */
	public default @NonNull Parser<I,O> greedy() {
		return word -> this.apply(word)
				.min((a,b) -> a.target.size() - b.target.size())
				.map(Stream::of).orElseGet(Stream::empty);
	}


	/**
	 * @param clazz the satisfying by {@code Class#isInstance(Object)}
	 * @return an elementary {@code Parser} that is satisfied by {@link Class}
	 * @see #satisfy(Predicate)
	 */
	public static <IO> @NonNull Parser<IO,IO> satisfy(
			final @NonNull Class<? super IO> clazz
			) {
		return Parser.satisfy(clazz::isInstance);
	}

	/**
	 * @param prototype the satisfying by {@code Object#equals(Object)}
	 * @return an elementary {@code Parser} that is satisfied by equality
	 * @see #satisfy(Predicate)
	 */
	public static <IO> @NonNull Parser<IO,IO> satisfy(
			final @Nullable IO prototype
			) {
		return Parser.satisfy(current -> Objects.equals(current, prototype));
	}


	/**
	 * Sequentializes any number of {@link Parser}s.
	 * @param parsers that are sequentialized
	 * @return a {@code Parser} that aggregates results of parsers in a sequence or {@link #epsilon()} when empty.
	 */
	@SafeVarargs
	public static <I,O> @NonNull Parser<I,Stream<O>> sequence(
			final @NonNull Parser<I,O>... parsers
			) {
		return Stream.of(parsers).parallel().<Parser<I,Stream<O>>>reduce(
				Parser.epsilon(),
				(a,b) -> a.concat(b.<Stream<O>>map(Stream::of), Stream::concat),
				(a,b) -> a.concat(b, Stream::concat)
				);
	}

	/**
	 * Prepends a single to a sequential {@link Parser}
	 * @param that sequential parser to be appended
	 * @return a sequential {@link Parser}
	 */
	public default @NonNull Parser<I,Stream<O>> appent(
			final @NonNull Parser<I,Stream<O>> that
			) {
		return this.concat(that, (l,r) -> Stream.concat(Stream.of(l), r));
	}

	/**
	 * Applies the {@link Parser} multiple times including zero-times
	 * @return a recursive star {@code Parser}
	 */
	public default @NonNull Parser<I, Stream<O>> any() {
		return Parser.recursive(
				self -> this.appent(self).choice(Parser.epsilon())
				);
	}

	/**
	 * Applies the {@link Parser} multiple times but at least one time
	 * @return a recursive iteration {@code Parser}
	 */
	public default @NonNull Parser<I, Stream<O>> many() {
		return this.appent(this.any());
	}

	/**
	 * Creates a list of any length separated by single input
	 * @param separator of two elements
	 * @return {@link Parser} for any list length
	 * @see #any() for list without separator
	 */
	public default Parser<I, Stream<O>> list(final I separator) {
		return Parser.recursive(
				self -> this.appent(Parser.satisfy(separator).right(self).choice(Parser.epsilon())).choice(Parser.epsilon())
				);
	}

	/**
	 * Encloses the input with left and right borders thats output is ignored
	 * @param left border
	 * @param right border
	 * @return
	 * @see #enclose(Parser, Parser) for general parser enclosement
	 */
	public default Parser<I, O> enclose(
			final I left,
			final I right
			) {
		return this.enclose(Parser.satisfy(left), Parser.satisfy(right));
	}

	/**
	 * Encloses the input with left and right borders thats output is ignored
	 * @param left border
	 * @param right border
	 * @return
	 * @see #enclose(Object, Object) for special parser enclosement with {@link Object#equals(Object)}
	 */
	public default Parser<I,O> enclose(
			final @NonNull Parser<I,?> left,
			final @NonNull Parser<I,?> right
			) {
		return left.right(this).left(right);
	}

	/**
	 * References an other parser by lazy evaluation
	 * @param supplyer for referenced parser
	 * @return
	 */
	public static <I,O> @NonNull Parser<I,O> reference(
			final @NonNull Supplier<Parser<I,O>> supplyer
			) {
		return word -> supplyer.get().apply(word);
	}

	/**
	 * Throws an exception for test issues
	 * @param exception that is thrown
	 * @throws RuntimeException
	 */
	public static <I,O> @NonNull Parser<I,O> fail(
			final @NonNull String message
			) {
		return word -> { throw new RuntimeException(message); };
	}

	/**
	 * Applies the {@link Parser} one or zero times
	 * @return an optional {@code Parser}
	 */
	public default @NonNull Parser<I, Stream<O>> option() {
		return this.map(Stream::of).choice(Parser.succeed(Stream::empty));
	}

	/**
	 * @param that {@link Parser} thats output is ignored
	 * @return a {@link Parser} that ignoring the right output but consuming the input
	 * @see #left(Parser)
	 */
	public default @NonNull Parser<I,O> left(
			final @NonNull Parser<I,?> that
			) {
		return this.concat(that, (l,r) -> l);
	}

	/**
	 * @param that {@link Parser} thats output is not ignored
	 * @return a {@link Parser} that ignoring the left output but consuming the input
	 * @see #left(Parser)
	 */
	public default <T> @NonNull Parser<I,T> right(
			final @NonNull Parser<I,T> that
			) {
		return this.concat(that, (l,r) -> r);
	}



	/**
	 * Filters the result set for completely consumed input
	 * @return a {@link Parser} that always returns output with completely consumed input
	 */
	public default @NonNull Parser<I,O> just() {
		return this.filter(result -> result.target.isEmpty());
	}


	/**
	 * @return a greedy recursive iterative {@link Parser}
	 * @see #greedy()
	 * @see #many()
	 */
	public default @NonNull Parser<I, Stream<O>> whole() {
		return this.many().greedy();
	}

	/**
	 * @return a greedy recursive star {@link Parser}
	 * @see #greedy()
	 * @see #any()
	 */
	public default @NonNull Parser<I, Stream<O>> all() {
		return this.any().greedy();
	}

	/**
	 * @return a greedy optional {@link Parser}
	 * @see #greedy()
	 * @see #option()
	 */
	public default @NonNull Parser<I, Stream<O>> consume() {
		return this.option().greedy();
	}

	/**
	 * Helper-function to create recursive higher-order functions
	 * @param recursive function that is applied
	 * @return a recursive {@link Parser}
	 */
	public static <I,O> @NonNull Parser<I,O> recursive(
			final @NonNull Function<Parser<I,O>,Parser<I,O>> recursive
			) {
		return ((RecursiveFunction<Parser<I,O>>) f -> f.apply(f))
				.apply(f -> recursive.apply(x -> f.apply(f).apply(x)));
	}



}
