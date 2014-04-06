package uk.co.bhyland.validation

import static Validation.success
import static Validation.failure

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

    /** Creates a builder, guaranteeing that it contains at least one Validation */
    public ApplicativeBuilder(final NonEmptyList<Validation<E,?>> validations) {
        this.validations = validations
    }

    /**
     * Gathers this builder's Validations together with the given Validation into a new builder,
     * which may continue to accumulate further Validations with the same error type E.
     */
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

        def result = validations.head().ap( success({ value -> new CurryingClosure(f).call(value) }) )

        validations.tail().each { validation ->
            result = validation.fold(
                    { errors -> result.fold(
                            {accumulatedErrors -> accumulateErrors(accumulatedErrors, errors)},
                            {validation}
                    )},
                    { value -> result.fold(
                            {result},
                            {closure -> success(closure.call(value))}
                    )})
        }

        return result
    }

    public <T> Validation<E,T> applyAllAsList(final Closure<T> f) {

        def args = validations.head().ap( success({ value -> [] << value }) )

        validations.tail().each { validation ->
            args = validation.fold(
                    { errors -> args.fold(
                            {accumulatedErrors -> accumulateErrors(accumulatedErrors, errors)},
                            {validation}
                    )},
                    { value -> args.fold(
                            {args},
                            {accumulatedArgs -> success(accumulatedArgs << value)}
                    )})
        }
        return args.ap(success(f))
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

        def result = success(new CurryingClosure(f))

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

    private static <E> Validation<E,?> accumulateErrors(final NonEmptyList<E> accumulatedErrors,
                                                        final NonEmptyList<E> errors) {
        def newTail = accumulatedErrors.tail()
        newTail.addAll(errors.toList())
        return failure(accumulatedErrors.head(), *newTail)
    }
}
