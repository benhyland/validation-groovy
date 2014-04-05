package uk.co.bhyland.validation

import static Validation.success
import static Validation.failure

public class ApplicativeBuilderTest extends ValidationTestCase {

    void testApplyAllOnValidationsThatAreAllSuccesses() {
        def s1 = success("a")
        def s2 = success("b")
        def s3 = success("c")
        isSuccessOf("abc", s1.with(s2).with(s3).applyAll( {a,b,c -> a+b+c} ))
    }

    void testApplyAllOnValidationsThatAreAMixOfSuccessesAndFailures() {
        def s1 = success("a")
        def f1 = failure("foo", "bar")
        def s2 = success("b")
        def f2 = failure("baz", "quux")
        def s3 = success("c")
        def f3 = failure("wibble", "↑↑↓↓←→←→ba")
        isFailureOf(["foo", "bar", "baz", "quux", "wibble", "↑↑↓↓←→←→ba"],
                    s1.with(f1).with(s2).with(f2).with(s3).with(f3).applyAll( {a,b,c,d,e,f -> "OK!"} ))
        isFailureOf(["foo", "bar", "baz", "quux", "wibble", "↑↑↓↓←→←→ba"],
                    f1.with(s2).with(f2).with(s3).with(f3).applyAll( {a,b,c,d,e,f -> "OK!"} ))
    }

    void testFlatMapAllOnValidationsThatAreAllSuccesses() {
        def s1 = success("a")
        def s2 = success("b")
        def s3 = success("c")
        isSuccessOf("abc", s1.with(s2).with(s3).flatMapAll( {a,b,c -> a+b+c} ))
    }

    void testFlatMapAllOnValidationsThatAreAMixOfSuccessesAndFailures() {
        def s1 = success("a")
        def f1 = failure("foo", "bar")
        def s2 = success("b")
        def f2 = failure("baz", "quux")
        def s3 = success("c")
        def f3 = failure("wibble", "↑↑↓↓←→←→ba")
        isFailureOf(["foo", "bar"],
                s1.with(f1).with(s2).with(f2).with(s3).with(f3).flatMapAll( {a,b,c,d,e,f -> "OK!"} ))
        isFailureOf(["foo", "bar"],
                f1.with(s2).with(f2).with(s3).with(f3).flatMapAll( {a,b,c,d,e,f -> "OK!"} ))
    }
}
