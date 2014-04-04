package uk.co.bhyland.validation

/**
 * An immutable list which does not conform to the java.util.List interface and which is guaranteed to be non-empty.
 */
public final class NonEmptyList<T> {

    private final T head
    private final List<T> tail

    public NonEmptyList(final T head, final List<T> tail) {
        if(head == null) { throw new IllegalArgumentException("NonEmptyList cannot have null head") }
        if(tail.contains(null)) { throw new IllegalArgumentException("NonEmptyList cannot contain null") }
        this.head = head
        final def tailCopy = []
        tailCopy.addAll(tail)
        this.tail = tailCopy
    }

    public NonEmptyList<T> append(final T item) {
        final def newTail = tail()
        newTail.add(item)
        return new NonEmptyList<T>(head, newTail)
    }

    public NonEmptyList<T> prepend(final T item) {
        return new NonEmptyList<T>(item, toList())
    }

    public T head() { return head }

    public List<T> tail() {
        final def tailCopy = []
        tailCopy.addAll(tail)
        return tailCopy
    }

    public List<T> toList() {
        final def list = [head]
        list.addAll(tail)
        return list
    }
}