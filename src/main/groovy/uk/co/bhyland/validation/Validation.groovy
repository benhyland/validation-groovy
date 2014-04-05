package uk.co.bhyland.validation

/**
 * Defines a sum type which is either a non-empty list of errors of type E, or a success of type A.
 *
 * Provides some basic operations, but with no attempt at generalisation - we don't expect to
 * interact much with types other than Validation.
 *
 * Implemented via two subclasses, Validation.Success and Validation.Failure.
 * These should never be referenced directly by client code.
 *
 * Unfortunately it would be awkward to prevent additional subclassing because of GROOVY-5728.
 * Nice things, can't have, etc.
 */
public abstract class Validation<E,A> {

    //private Validation() {} prevented by GROOVY-5728.

    /**
     * Catamorphism to safely distinguish between failures and successes.
     *
     * ifFailure takes a single parameter of type NonEmptyList<E>.
     * ifSuccess takes a single parameter of type A.
     */
    public abstract <T> T fold(final Closure<T> ifFailure, final Closure<T> ifSuccess)

    /**
     * Applies a value in the context of this Validation to a Closure in the context of the given
     * Validation if both are successes, accumulating any errors if either or both are failures.
     *
     * If validationOfClosure is a Validation.Success, its value is a Closure taking a single parameter of type A.
     *
     * Could be implemented with nested fold but this way might be easier to read.
     */
    public abstract <B> Validation<E,B> ap(final Validation<E,Closure<B>> validationOfClosure)

    /** Convenience for executing an effect if this is a Validation.Failure. */
    public void ifFailure(final Closure ifFailure) {
        fold(ifFailure, {})
    }

    /** Convenience for executing an effect if this is a Validation.Success. */
    public void ifSuccess(final Closure ifSuccess) {
        fold({}, ifSuccess)
    }

    /** Convenience for handling default successes and failures in the context of a Validation. */
    public Validation<E,A> orElse(final Validation<E,A> other) {
        return fold( { other }, { this } )
    }

    /** Convenience for handling default values. */
    public A getOrElse(final Closure<A> other) {
        return fold( other, { value } )
    }

    /**
     * Right-biased map.
     * Maps the given Closure across the success value if this is a Validation.Success.
     * Identity if this is a Validation.Failure.
     *
     * f takes a single parameter of type A.
     *
     * aka collect.
     */
    public <B> Validation<E,B> map(final Closure<B> f) {
        return fold( { this }, { success(f(value)) } )
    }

    /**
     * Right-biased flatMap.
     * Maps the given Closure across the success value if this is a Validation.Success, and unwraps
     * one level of wrapped Validations.
     * Identity if this is a Validation.Failure.
     *
     * f takes a single parameter of type A.
     *
     * aka bind. No direct Groovy equivalent, but it would be called flatCollect or similar.
     *
     * This may not be that helpful since we are enforcing the use of a semigroup (NonEmptyList) as the error type.
     * We don't guarantee that only a single error results from any failure encountered, but we do
     * guarantee to stop processing at the first failure.
     */
    public <B> Validation<E,B> flatMap(final Closure<Validation<E,B>> f) {
        return fold( { this }, { f(value) } )
    }

    /**
     * Gathers this Validation and the given Validation into a builder, which may continue to accumulate
     * further Validations with the same error type E.
     *
     * The resulting builder may be used to map across the tuple of all results
     * (if all gathered validations are Validation.Success)
     * or accumulate all errors
     * (if it contains at least one Validation.Failure).
     */
    public ApplicativeBuilder<E> with(final Validation<E,?> next) {
        final def validations = new NonEmptyList<Validation<E,?>>(this, [])
        return new ApplicativeBuilder<E>(validations).with(next)
    }

    /** Factory method for creation of Validation.Success */
    public static <E,A> Validation<E,A> success(A value) {
        if(value == null) {
            throw new IllegalArgumentException("successes must not be null")
        }
        return new Success<E,A>(value)
    }

    /** Factory method for creation of Validation.Failure */
    public static <E,A> Validation<E,A> failure(E error, E... moreErrors) {
        if(error == null || moreErrors.contains(null)) {
            throw new IllegalArgumentException("failures must not be null")
        }
        return new Failure<E,A>(new NonEmptyList<E>(error, moreErrors.toList()))
    }

    private static class Success<E,A> extends Validation<E,A> {

        private final A value

        private Success(final A success) {
            this.value = success
        }

        @Override
        public String toString() { return "Success(" + value + ")" }

        @Override
        public <T> T fold(final Closure<T> ifFailure, final Closure<T> ifSuccess) { return ifSuccess(value) }

        @Override
        public <B> Validation<E,B> ap(final Validation<E,Closure<B>> validationOfClosure) {
            return validationOfClosure.fold(
                    { validationOfClosure },
                    { closure -> success(closure(value)) }
            )
        }
    }

    private static class Failure<E,A> extends Validation<E,A> {

        private final NonEmptyList<E> errors

        private Failure(final NonEmptyList<E> errors) {
            this.errors = errors
        }

        @Override
        public String toString() { return "Failure(" + errors.toList() + ")" }

        @Override
        public <T> T fold(final Closure<T> ifFailure, final Closure<T> ifSuccess) { return ifFailure(errors) }

        @Override
        public <B> Validation<E,B> ap(final Validation<E,Closure<B>> validationOfClosure) {
            return validationOfClosure.fold(
                    { validationOfClosureErrors ->
                        final def newTail = errors.tail()
                        newTail.addAll(validationOfClosureErrors.toList())
                        failure(errors.head(), *newTail)
                    },
                    { this }
            )
        }
    }
}