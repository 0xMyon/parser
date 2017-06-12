package com.github.myon.parser;

import java.util.function.Function;

/**
 * Recursive function interface
 * 
 * @author 0xMyon
 *
 * @param <F> original function
 */
public interface RecursiveFunction<F extends Function<?, ?>> extends Function<RecursiveFunction<F>, F> {

}
