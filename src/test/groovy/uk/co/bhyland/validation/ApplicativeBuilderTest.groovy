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

    void testApplyAllWithAClosureTakingASingleParameter() {
        def s1 = success("a")
        def s2 = success("b")
        def s3 = success("c")
        isSuccessOf("abc", s1.with(s2).with(s3).applyAll( {abc -> abc.join()} ))
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

    void testFlatMapAllWithAClosureTakingASingleParameter() {
        def s1 = success("a")
        def s2 = success("b")
        def s3 = success("c")
        isSuccessOf("abc", s1.with(s2).with(s3).flatMapAll( {abc -> abc.join()} ))
    }

    void testSequenceApplicativeOnValidationsThatAreAllSuccesses() {
        def s1 = success("a")
        def s2 = success("b")
        def s3 = success("c")
        isSuccessOf(["a", "b", "c"], s1.with(s2).with(s3).sequenceApplicative().map { it.toList() })
    }

    void testSequenceApplicativeOnValidationsThatAreAMixOfSuccessesAndFailures() {
        def s1 = success("a")
        def f1 = failure("foo", "bar")
        def s2 = success("b")
        def f2 = failure("baz", "quux")
        def s3 = success("c")
        def f3 = failure("wibble", "↑↑↓↓←→←→ba")
        isFailureOf(["foo", "bar", "baz", "quux", "wibble", "↑↑↓↓←→←→ba"],
                s1.with(f1).with(s2).with(f2).with(s3).with(f3).sequenceApplicative())
        isFailureOf(["foo", "bar", "baz", "quux", "wibble", "↑↑↓↓←→←→ba"],
                f1.with(s2).with(f2).with(s3).with(f3).sequenceApplicative())
    }

    void testSequenceOnValidationsThatAreAllSuccesses() {
        def s1 = success("a")
        def s2 = success("b")
        def s3 = success("c")
        isSuccessOf(["a", "b", "c"], s1.with(s2).with(s3).sequence().map { it.toList() })
    }

    void testSequenceOnValidationsThatAreAMixOfSuccessesAndFailures() {
        def s1 = success("a")
        def f1 = failure("foo", "bar")
        def s2 = success("b")
        def f2 = failure("baz", "quux")
        def s3 = success("c")
        def f3 = failure("wibble", "↑↑↓↓←→←→ba")
        isFailureOf(["foo", "bar"],
                s1.with(f1).with(s2).with(f2).with(s3).with(f3).sequence())
        isFailureOf(["foo", "bar"],
                f1.with(s2).with(f2).with(s3).with(f3).sequence())
    }

    void testSupplyingInputToListOfValidationFunctions() {
        def check1 = { it -> if(it.length() < 10) success(it.length()) else failure("too long") }
        def check2 = { it -> if(it.startsWith("hello")) success("world") else failure("too unfriendly") }
        def check3 = { it -> if(it.contains(" ")) success(it) else failure("too few words") }

        def checks = [check1, check2, check3]

        def input1 = "hello foo"
        def input2 = "hellooooooooooooo!"
        def onSuccess = { a,b,c -> "$a, $b, $c" }

        isSuccessOf("9, world, hello foo", ApplicativeBuilder.allWithInput(checks, input1).applyAll(onSuccess))
        isFailureOf(["too long", "too few words"], ApplicativeBuilder.allWithInput(checks, input2).applyAll(onSuccess))
    }

    void testWithAll() {
        def s1 = success("a")
        def s2 = success("b")
        def s3 = success("c")
        def s4 = success("d")
        isSuccessOf("abcd", s1.with(s2).withAll(s3, s4).applyAll( {abc -> abc.join()} ))
        isSuccessOf("abcd", s1.withAll(s2, s3, s4).applyAll( {abc -> abc.join()} ))
    }

    void testWithAllNEL() {
        def s1 = success("a")
        def s2 = success("b")
        def s3 = success("c")
        def s4 = success("d")
        isSuccessOf("abcd", s1.with(s2).withAllNEL(new NonEmptyList(s3, [s4])).applyAll( {abc -> abc.join()} ))
        isSuccessOf("abcd", s1.withAllNEL(new NonEmptyList(s2, [s3, s4])).applyAll( {abc -> abc.join()} ))
    }
}
