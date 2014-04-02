package uk.co.bhyland.validation

/**
 * An applicative builder deliberately limited to handling ValidationNELs.
 *
 * It allows handling of arbitrary arity in a single class by being type unsafe.
 *
 * This approach is probably simpler to understand and quicker to implement,
 * and certainly more idiomatic for Groovy than alternatives involving heavy use of higher kinds.
 */
public final class ApplicativeBuilder<E> {

    private final NonEmptyList<Validation<E,?>> validations

    public ApplicativeBuilder(final NonEmptyList<Validation<E,?>> validations) {
        this.validations = validations
    }

    public ApplicativeBuilder with(final Validation<E,?> next) {
        return new ApplicativeBuilder(validations.append(next))
    }

    /**
     * If all of the gathered Validations are successful, apply their values in order
     * as parameters to the given Closure and return the result in a Validation.Success.
     *
     * If any of the gathered Validations are failures, accumulate their errors
     * and return them in a Validation.Failure.
     *
     * If the given Closure takes arguments of the wrong arity or type, the result is undefined
     * but will likely be a runtime error.
     */
    public <T> Validation<E,T> applyAll(final Closure<T> f) {

        def result = validations.head().ap( Validation.success({ value -> new CurryingClosure(f).call(value) }) )

        validations.tail().each { validation ->
            result = validation.ap( result )
        }

        return result
    }


    /**
     * If all of the gathered Validations are successful, apply their values in order
     * as parameters to the given Closure and return the result in a Validation.Success.
     *
     * If any of the gathered Validations are failures, return the first failure encountered.
     * Note that this Validation.Failure may still contain multiple errors.
     *
     * If the given Closure takes arguments of the wrong arity or type, the result is undefined
     * but will likely be a runtime error.
     *
     * This is included for completeness, it's not really as useful as applyAll.
     */
    public <T> Validation<E,T> flatMapAll(final Closure<T> f) {

        def result = Validation.success(new CurryingClosure(f))

        validations.toList().each { validation ->
            result = result.flatMap { curryingClosure -> validation.ap(Validation.<E,?>success(curryingClosure)) }
        }

        return result
    }

    /**
     * Convenience for handling a collection of validation checks which all take the same input value.
     * Applys the input to each check, and gathers the resulting Validations in order into a ApplicativeBuilder.
     *
     * Note that this relies on duck typing to identify the with() method,
     * which may be on Validation or on ApplicativeBuilder. Cool or horrific? You decide!
     */
    public static <E> ApplicativeBuilder<E> allWithInput(final Collection<Closure<Validation<E,?>>> fs, input) {
        return fs.collect { f -> f(input) }.inject { a,b -> a.with(b) }
    }
}
