package com.github.myon.parser;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNull;
import org.junit.Assert;
import org.junit.Test;

import com.github.myon.tuple.Tuple;


public class ParserTest {

	public static <I,O> Function<Tuple<Stream<O>,List<I>>, Tuple<List<O>,List<I>>> toList() {
		return t -> Tuple.<Stream<O>,List<I>,List<O>>left(s -> s.collect(Collectors.toList())).apply(t);
	}

	@Test
	public void test() {


		final List<Character> w = Util.string("abc");

		final Parser<Character,Character> a = Parser.satisfy('a');
		final Parser<Character,Character> b = Parser.satisfy('b');

		Assert.assertEquals(
				Util.set(Tuple.of('a', Util.string("bc"))),
				a.apply(w).collect(Collectors.toSet()));

		Assert.assertEquals(
				Util.set(
						Tuple.of(Util.string(""), Util.string("aaab")),
						Tuple.of(Util.string("a"), Util.string("aab")),
						Tuple.of(Util.string("aa"), Util.string("ab")),
						Tuple.of(Util.string("aaa"), Util.string("b"))
						),
				a.any().apply(Util.string("aaab")).map(ParserTest.toList()).collect(Collectors.toSet()));

		Assert.assertEquals(
				Util.set(
						Tuple.of(Util.string("aaa"), Util.string("b"))
						),
				a.whole().apply(Util.string("aaab")).map(ParserTest.toList()).collect(Collectors.toSet()));



		Assert.assertEquals(
				Util.set(
						Tuple.of(Util.string("aaa"), Util.string(""))
						),
				a.many().just().apply(Util.string("aaa")).map(ParserTest.toList()).collect(Collectors.toSet()));

		Assert.assertEquals(Util.set(
				Tuple.of(Util.string("a"), Util.string("")),
				Tuple.of(Util.string(""), Util.string("a"))
				),
				a.option().apply(Util.string("a")).map(ParserTest.toList()).collect(Collectors.toSet()));

		Assert.assertEquals(Util.set(
				Tuple.of(Util.string("a"), Util.string(""))
				),
				a.consume().apply(Util.string("a")).map(ParserTest.toList()).collect(Collectors.toSet()));

		Assert.assertEquals(
				Util.set(
						Tuple.of(Util.string("ababab"), Util.string(""))
						),
				Parser.sequence(a,b,a,b,a,b).apply(Util.string("ababab")).map(ParserTest.toList()).collect(Collectors.toSet()));

		Assert.assertEquals(
				Util.set(
						Tuple.of('a', Util.string(""))
						),
				Parser.satisfy('(').right(Parser.satisfy(Character.class)).left(Parser.satisfy(')')).apply(Util.string("(a)")).collect(Collectors.toSet()));

		Assert.assertEquals(Util.set(
				Tuple.of('a', Util.string(""))
				), a.enclose('(', ')').apply(Util.string("(a)")).collect(Collectors.toSet()));

		final Parser<Character,Stream<Character>> list = a.list(',').greedy();

		Assert.assertEquals(Util.set(
				Tuple.of(Util.string(""), Util.string(""))
				), list.apply(Util.string("")).map(ParserTest.toList()).collect(Collectors.toSet()));

		Assert.assertEquals(Util.set(
				Tuple.of(Util.string("a"), Util.string(""))
				), list.apply(Util.string("a")).map(ParserTest.toList()).collect(Collectors.toSet()));

		Assert.assertEquals(Util.set(
				Tuple.of(Util.string("aa"), Util.string(""))
				), list.apply(Util.string("a,a")).map(ParserTest.toList()).collect(Collectors.toSet()));


		try {
			Parser.<Character,Character>fail("fail").apply(Util.string(""));
			Assert.fail("no exception");
		} catch (final RuntimeException e) {
			// TODO: handle exception
		}

	}

	Parser<Character,Character> a = null;
	Parser<Character,Character> b = null;

	@Test
	public void testReference() {



		final Parser<Character,Tuple<Character, Character>> ab = Parser.reference(()->this.a).concat(Parser.reference(()->this.b));

		try {
			Assert.assertEquals(Util.set(
					Tuple.of(Tuple.of('a', 'b'), Util.string(""))
					), ab.apply(Util.string("ab")).collect(Collectors.toSet()));
			Assert.fail("NullPointerException should have been thrown");
		} catch (final NullPointerException e) {
			// TODO: handle exception
		}

		this.a = Parser.satisfy('a');
		this.b = Parser.satisfy('b');

		Assert.assertEquals(Util.set(
				Tuple.of(Tuple.of('a', 'b'), Util.string(""))
				), ab.apply(Util.string("ab")).collect(Collectors.toSet()));

	}

	//@Test
	public void testErrorHandling() {

		final Parser<Character,Either<Character,String>> a = Parser.satisfy(i -> i == 'a', "A");
		final Parser<Character,Either<Character,String>> b = Parser.satisfy(i -> i == 'b', "B");
		final Parser<Character,Either<Character,String>> c = Parser.satisfy(i -> i == 'c', "C");

		final @NonNull Parser<Character, Stream<Either<Character, String>>> p = Parser.sequence(a,b,c);

		Assert.assertEquals(
				Util.set(
						Tuple.of(Util.list(Either.first('a'),Either.first('b'),Either.first('c')), Util.string(""))
						),
				p.apply(Util.string("abc")).map(ParserTest.toList()).collect(Collectors.toSet()));


		Assert.assertEquals(
				Util.set(
						Tuple.of(Util.list(Either.first('a'),Either.second("B"),Either.first('b'),Either.first('c')), Util.string(""))
						),
				p.apply(Util.string("azbc")).map(ParserTest.toList()).collect(Collectors.toSet()));

		Assert.assertEquals(
				Util.set(
						Tuple.of(Util.list(Either.first('a'),Either.second("B"),Either.first('c')), Util.string(""))
						),
				p.apply(Util.string("azc")).map(ParserTest.toList()).collect(Collectors.toSet()));

		Assert.assertEquals(
				Util.set(
						Tuple.of(Util.list(Either.first('a'),Either.second("B"),Either.first('c')), Util.string(""))
						),
				p.apply(Util.string("ac")).map(ParserTest.toList()).collect(Collectors.toSet()));


		/*
		 * 1 2 3 -> OKAY -> Call current rule on current input
		 * 1 3 -> MISSING -> Call next rule on current input
		 * 1 1 3 -> WRONG -> Call next rule on next input
		 * 1 A 2 3 -> ADDITIONAL -> Call current rule on next input
		 */

	}

	@Test
	public void testBNF() {

		final Parser<Character,Character> open_id = Parser.satisfy('<');
		final Parser<Character,Character> close_id = Parser.satisfy('>');
		final Parser<Character,Character> definition_op = Parser.satisfy(':').left(Parser.satisfy(':')).left(Parser.satisfy('='));

		final Parser<Character,String> id = open_id.right(Parser.<Character>satisfy(c -> c != '>').all()).left(close_id).map(s -> s.map(Object::toString).collect(Collectors.joining()));

		final @NonNull Parser<Character, String> BNF = id.left(definition_op);

		final @NonNull Parser<Character, Stream<Character>> ws = Parser.<Character>satisfy(Character::isWhitespace).all();
		final @NonNull Parser<Character, Stream<Character>> terminal = Parser.satisfy('"').right(Parser.<Character>satisfy(c -> c != '"').whole()).left(Parser.satisfy('"'));

		final Parser<Character, BNFExp> exp = Parser.<Character,BNFExp>recursive(p -> Parser.choice(
				Parser.satisfy('(').right(p).left(Parser.satisfy(')')),
				p.left(Parser.satisfy('|')).concat(p, BNFExp::choice)
				));

		InputPreperator.instance.andThen(BNF).apply("<Ziffer ausser Null> ::= \"1\"|2|3|4|5|6|7|8|9;");

	}

	static class BNFExp {

		static BNFExp choice(final BNFExp left, final BNFExp right) {
			return null;
		}

		class Terminal extends BNFExp {

		}
	}

	@Test
	public void testTokens() {

		final TokenType<Character> number = new TokenType<>(Character::isDigit);
		final TokenType<Character> ws = new TokenType<>(Character::isWhitespace);
		final TokenType<Character> operator = new TokenType<>(c -> c == '+' || c == '-');


		final Parser<Character,Stream<Token<Character>>> tokenizer = Parser.choice(
				number.parser(), ws.parser(), operator.parser()
				).all();

		Assert.assertEquals(Util.set(
				Tuple.of(Util.list(
						new Token<>(number, Util.string("123")),
						new Token<>(ws, Util.string(" ")),
						new Token<>(operator, Util.string("+")),
						new Token<>(ws, Util.string(" ")),
						new Token<>(number, Util.string("456")),
						new Token<>(ws, Util.string(" ")),
						new Token<>(operator, Util.string("-")),
						new Token<>(ws, Util.string(" ")),
						new Token<>(number, Util.string("789"))
						), Util.string(""))
				), tokenizer.apply(Util.string("123 + 456 - 789")).map(ParserTest.toList()).collect(Collectors.toSet()));

		//final Parser<Token<Character>, Node> number_node = Parser.satisfy(number).apply(Node::new);
		//;
		//Parser<Token, Node> expr =

	}


}
