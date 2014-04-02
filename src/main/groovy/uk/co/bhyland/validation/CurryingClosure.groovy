package uk.co.bhyland.validation

/**
 * A closure wrapper which partially applies arguments to its wrapped closure as it
 * is successively called with single arguments.
 *
 * In this way it implements true currying - the conversion of a closure that takes multiple parameters
 * into a chain of closures, each taking a single argument and returning the next in the chain.
 * Closure.curry(arg) is actually partial application, not currying.
 */
public final class CurryingClosure extends Closure {

    private final Closure closure

    public CurryingClosure(final Closure closure) {
        super(closure.owner, closure.delegate)
        this.closure = closure
    }

    public def call() { closure.call() }

    public def call(arg) {
        if(closure.getMaximumNumberOfParameters() < 2) {
            return closure.call(arg)
        }
        else {
            return new CurryingClosure(closure.curry(arg))
        }
    }
}