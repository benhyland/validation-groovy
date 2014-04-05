package uk.co.bhyland.validation

import static Validation.success
import static Validation.failure

public class ValidationTest extends GroovyTestCase {

    def ignore_GROOVY_5728_testIllegalValidationSubclassing() {
        shouldFail { new Validation<String,String>() {
            @Override
            def <T> T fold(Closure<T> ifFailure, Closure<T> ifSuccess) {
                return null
            }

            @Override
            def <B> Validation<String, B> ap(Validation<String, Closure<B>> validationOfClosure) {
                return null
            }
        }}
    }

    void testIllegalValidationConstruction() {
        shouldFail { success() }
        shouldFail { success(null) }
        shouldFail { success("a", "b") }
        shouldFail { failure() }
        shouldFail { failure(null) }
        shouldFail { failure("a", null, "c") }
    }

    void testFoldingOnSuccess() {
        def v = success("hello")
        def result = v.fold({ fail() }, { it.length() })
        assertEquals(5, result)
    }

    void testFoldingOnFailure() {
        def v = failure("hello")
        def result = v.fold({ it.toList() }, { fail() })
        assertEquals(["hello"], result)
    }

    void testOrElseOnSuccess() {
        def v = success("hello")
        def v2 = success("world")
        def result = v.orElse(v2)
        assertEquals(v, result)
    }

    void testOrElseOnFailure() {
        def v = failure("hello")
        def v2 = success("world")
        def result = v.orElse(v2)
        assertEquals(v2, result)
    }

    void testGetOrElseOnSuccess() {
        def v = success("hello")
        def result = v.getOrElse {"world"}
        assertEquals("hello", result)
    }

    void testGetOrElseOnFailure() {
        def v = failure("hello")
        def result = v.getOrElse {"world"}
        assertEquals("world", result)
    }

    void testMapOnSuccess() {
        def v = success("hello")
        def result = v.map { it.length() }
        isSuccessOf(5, result)
    }

    void testMapOnFailure() {
        def v = failure("hello")
        def result = v.map { it.length() }
        assertEquals(v, result)
    }

    void testApOnSuccessWithSuccess() {
        def v = success("hello")
        def result = v.ap(success({ it.reverse() }))
        isSuccessOf("olleh", result)
    }

    void testApOnFailureWithSuccess() {
        def v = failure("hello")
        def result = v.ap(success({ it.reverse() }))
        assertEquals(v, result)
    }

    void testApOnSuccessWithFailure() {
        def v = success("hello")
        def result = v.ap(failure("world"))
        isFailureOf(["world"], result)
    }

    void testApOnFailureWithFailure() {
        def v = failure("hello")
        def result = v.ap(failure("world"))
        isFailureOf(["hello", "world"], result)
    }

    void testApAccumulatesErrors() {
        def v = failure("hello", "world")
        def v2 = failure("foo", "bar", "baz")
        def result = v.ap(v2)
        isFailureOf(["hello", "world", "foo", "bar", "baz"], result)
    }

    void testFlatMapOnSuccessWithSuccess() {
        def v = success("hello")
        def result = v.flatMap { it -> success(it.reverse()) }
        isSuccessOf("olleh", result)
    }

    void testFlatMapOnFailureWithSuccess() {
        def v = failure("hello")
        def result = v.flatMap { it -> success(it.reverse()) }
        assertEquals(v, result)
    }

    void testFlatMapOnSuccessWithFailure() {
        def v = success("hello")
        def result = v.flatMap { it -> failure("world") }
        isFailureOf(["world"], result)
    }

    void testFlatMapOnFailureWithFailure() {
        def v = failure("hello")
        def result = v.flatMap { it -> failure("world") }
        isFailureOf(["hello"], result)
    }

    void testFlatMapDoesNotAccumulateErrors() {
        def v = failure("hello", "world")
        def f = { x -> failure("foo", "bar", "baz") }
        def result = v.flatMap(f)
        isFailureOf(["hello", "world"], result)
    }

    static def isSuccessOf(expected, validation) {
        assertEquals(expected, validation.fold({fail()}, {it}))
    }

    static def isFailureOf(expected, validation) {
        assertEquals(expected, validation.fold({it.toList()}, {fail()}))
    }
}