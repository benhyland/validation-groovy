package uk.co.bhyland.validation

public class NonEmptyListTest extends GroovyTestCase {

    void testNonEmptyListCannotContainNull() {
        shouldFail { new NonEmptyList<String>() }
        shouldFail { new NonEmptyList<String>(null, []) }
        shouldFail { new NonEmptyList<String>("hello", ["world", null]) }
    }

    void testHeadReturnsOriginalInstance() {
        def hello = new Object()
        assertEquals(hello, new NonEmptyList<Object>(hello, [new Object()]).head())
    }

    void testConstructorTakesShallowCopyOfTail() {
        def head = "hello"
        def tail = ["world"]
        def nel = new NonEmptyList<String>(head, tail)
        tail << "!"
        assertEquals(["hello", "world"], nel.toList())
    }

    void testToListReturnsShallowCopy() {
        def head = "hello"
        def tail = ["world"]
        def nel = new NonEmptyList<String>(head, tail)
        def list = nel.toList()
        tail << "!"
        assertEquals(["hello", "world"], list)
        list << "!"
        assertEquals(["hello", "world"], nel.toList())
    }

    void testTailReturnsShallowCopy() {
        def head = "hello"
        def tail = ["world"]
        def nel = new NonEmptyList<String>(head, tail)
        def newTail = nel.tail()
        tail << "!"
        assertEquals(["world"], newTail)
        newTail << "!"
        assertEquals(["world"], nel.tail())
    }

    void testAppendCreatesNewList() {
        def nel = new NonEmptyList<String>("hello", [])
        def newList = nel.append("world")
        assertEquals(["hello"], nel.toList())
        assertEquals(["hello", "world"], newList.toList())
    }

    void testPrependCreatesNewList() {
        def nel = new NonEmptyList<String>("hello", [])
        def newList = nel.prepend("world")
        assertEquals(["hello"], nel.toList())
        assertEquals(["world", "hello"], newList.toList())
    }

    void testAppendAllCreatesNewList() {
        def nel1 = new NonEmptyList<String>("foo", ["bar"])
        def nel2 = new NonEmptyList<String>("baz", ["quux"])
        def newList = nel1.appendAll(nel2)
        assertEquals(["foo", "bar"], nel1.toList())
        assertEquals(["baz", "quux"], nel2.toList())
        assertEquals(["foo", "bar", "baz", "quux"], newList.toList())
    }

    void testPrependAllCreatesNewList() {
        def nel1 = new NonEmptyList<String>("foo", ["bar"])
        def nel2 = new NonEmptyList<String>("baz", ["quux"])
        def newList = nel1.prependAll(nel2)
        assertEquals(["foo", "bar"], nel1.toList())
        assertEquals(["baz", "quux"], nel2.toList())
        assertEquals(["baz", "quux", "foo", "bar"], newList.toList())
    }
}