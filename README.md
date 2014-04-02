validation
==========

This is a Groovy impl of Validation.

There are several projects which try to represent a nice taxonomy of commonly used functional constructs in Groovy.

Here we limit ourselves to Validation and avoid the abstract generalisations for several reasons:
- to provide an quicker and easier demonstration of value for an audience with varying levels of experience with functional programming
- we don't have the time to do things properly
- to have something useful without depending on additional libraries
- to focus on an extremely useful construct, the lack of which causes much frustration
- to focus on a contstruct which is immediately useful in refactoring a major Groovy project we have to hand

An interesting facet is that in Groovy we can implement the builder for apply() without resorting to a class or method call for each arity, as is seen in Scala (without Shapeless, that is). The price we pay is loss of type safety, which presumably we aren't that concerned about in the first place since we are already using Groovy.
