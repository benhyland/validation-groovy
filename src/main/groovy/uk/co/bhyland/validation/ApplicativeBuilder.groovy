package uk.co.bhyland.validation

import static Validation.success
import static Validation.failureFromNEL

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
     * Gathers this builder's Validations together with the given Validations into a new builder,
     * which may continue to accumulate further Validations with the same error type E.
     */
    public ApplicativeBuilder withAll(final Validation<E,?> next, final Validation<E,?>... more) {
        return withAllNEL(new NonEmptyList<Validation<E,?>>(next, more.toList()))
    }

    /**
     * Gathers this builder's Validations together with the given NonEmptyList of Validations into a new builder,
     * which may continue to accumulate further Validations with the same error type E.
     */
    public ApplicativeBuilder withAllNEL(final NonEmptyList<Validation<E,?>> more) {
        return new ApplicativeBuilder(validations.appendAll(more))
    }

    /**
     * If all of the gathered Validations are successful, apply their values in order
     * as parameters to the given Closure and return the result in a Validation.Success.
     *
     * If any of the gathered Validations are failures, accumulate their errors
     * and return them in a Validation.Failure.
     *
     * If the given Closure takes the same number of parameters as there are success values,
     * the closure will be called as normal with those values.
     * If there are multiple success values and the given Closure takes only one parameter,
     * the successes will be passed as a list in the first parameter.
     * Otherwise an IllegalArgumentException is thrown.
     */
    public <T> Validation<E,T> applyAll(final Closure<T> f) {

        def argsValidation = sequenceApplicative()

        return applyClosureToArgs(f, argsValidation)
    }

    /**
     * If all of the gathered Validations are successful, apply their values in order
     * as parameters to the given Closure and return the result in a Validation.Success.
     *
     * If any of the gathered Validations are failures, return the first failure encountered.
     * Note that this Validation.Failure may still contain multiple errors.
     *
     * If the given Closure takes the same number of parameters as there are success values,
     * the closure will be called as normal with those values.
     * If there are multiple success values and the given Closure takes only one parameter,
     * the successes will be passed as a list in the first parameter.
     * Otherwise an IllegalArgumentException is thrown.
     */
    public <T> Validation<E,T> flatMapAll(final Closure<T> f) {

        def argsValidation = sequence()

        return applyClosureToArgs(f, argsValidation)
    }

    /**
     * Convenience for handling a collection of validation checks which all take the same input value.
     * Applies the input to each check, and gathers the resulting Validations in order into a ApplicativeBuilder.
     * Throws IllegalArgumentException if the given collection is empty.
     */
    public static <E> ApplicativeBuilder<E> allWithInput(final Collection<Closure<Validation<E,?>>> fs, input) {
        if(fs.isEmpty()) { throw new IllegalArgumentException("must supply at least one validation function") }
        def withInput = fs.collect { f -> f(input) }

        def head = withInput[0]
        def tail = fs.size() == 1 ? [] : withInput[1..-1]
        return new ApplicativeBuilder<E>(new NonEmptyList<Validation<E,?>>(head, tail))
    }

    /**
     * Transform the list of Validations gathered by this builder into a Validation.Success
     * of a list of success values, if all were Validation.Success.
     * Otherwise, transform into a Validation.Failure of the accumulated errors.
     */
    public Validation<E,NonEmptyList> sequenceApplicative() {

        Validation<E,NonEmptyList> args = validations.head().map { value -> new NonEmptyList(value, []) }

        validations.tail().each { validation ->
            args = validation.fold(
                    { errors -> args.fold(
                            {accumulatedErrors -> failureFromNEL(accumulatedErrors.appendAll(errors))},
                            {validation}
                    )},
                    { value -> args.fold(
                            {args},
                            {accumulatedArgs -> success(accumulatedArgs.append(value))}
                    )})
        }

        return args
    }

    /**
     * Transform the list of Validations gathered by this builder into a Validation.Success
     * of a list of success values, if all were Validation.Success.
     * Otherwise, transform into a Validation.Failure containing the errors from the first Failure encountered.
     */
    public Validation<E,NonEmptyList> sequence() {

        def args = validations.head().map { value -> new NonEmptyList(value, []) }

        validations.tail().each { validation ->
            args = args.flatMap { accumulatedArgs ->
                validation.map { value -> accumulatedArgs.append(value) }
            }
        }

        return args
    }

    private static <T> Validation<E,T> applyClosureToArgs(Closure<T> f, Validation<E,?> argsValidation) {
        return argsValidation.fold(
                {argsValidation},
                {args ->
                    def argList = args.toList()
                    if(argList.size() == f.getMaximumNumberOfParameters()) {
                        success(f.call(*argList))
                    }
                    else if(f.getMaximumNumberOfParameters() == 1) {
                        success(f.call(argList))
                    }
                    else {
                        throw new IllegalArgumentException("A closure taking ${f.getMaximumNumberOfParameters()} parameters cannot be applied to ${argList.size()} arguments")
                    }
                }
        )
    }
}
