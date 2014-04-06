package uk.co.bhyland.validation

public class ValidationTestCase extends GroovyTestCase {

    public static def isSuccessOf(expected, validation) {
        assertEquals(expected,
                validation.fold(
                    {errs -> this.fail("expected success of $expected but saw failure of ${errs.toList()}")},
                    {value -> value}
                ))
    }

    public static def isFailureOf(expected, validation) {
        assertEquals(expected,
                validation.fold(
                        {errs -> errs.toList()},
                        {value -> this.fail("expected failure of $expected but saw success of $value")}
                ))
    }
}