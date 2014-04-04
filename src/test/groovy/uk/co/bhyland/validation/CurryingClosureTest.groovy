package uk.co.bhyland.validation

public class CurryingClosureTest extends GroovyTestCase {

    void testCurryingASingleArgumentClosureYieldsASingleArgumentClosure() {
        def closure = new CurryingClosure(singleArgClosure)
        def result = closure.call("hello")
        assertFalse(result instanceof Closure)
    }

    void testCurryingAMultiArgumentClosureYieldsAChainOfSingleArgumentClosures() {
        def closure = new CurryingClosure(multipleArgClosure)
        def result = closure.call(true)
        assertTrue(result instanceof CurryingClosure)
        result = result.call("hello")
        assertTrue(result instanceof CurryingClosure)
        result = result.call("world")
        assertFalse(result instanceof Closure)
    }

    private def singleArgClosure = { x -> x }
    private def multipleArgClosure = { a,b,c -> if(a) b else c }
}
