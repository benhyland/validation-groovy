package uk.co.bhyland.validation

public class ValidationTestCase extends GroovyTestCase {

    public static def isSuccessOf(expected, validation) {
        assertEquals(expected, validation.fold({fail()}, {it}))
    }

    public static def isFailureOf(expected, validation) {
        assertEquals(expected, validation.fold({it.toList()}, {fail()}))
    }
}