package uk.co.bhyland.validation

//Validation<String,String> s = Validation.<String,String>success() // illegal
//Validation<String,String> s = Validation.<String,String>success(null) // illegal
//Validation<String,String> s = Validation.<String,String>success("a", "b") // illegal
//Validation<String,String> f = Validation.<String,String>failure() // illegal
//Validation<String,String> f = Validation.<String,String>failure(null) // illegal
//Validation<String,String> f = Validation.<String,String>failure("a",null,"c") // illegal

Validation<String,String> s1 = Validation.<String,String>success("hello")
Validation<String,String> s2 = Validation.<String,String>success("world")
Validation<String,Integer> s3 = Validation.<String,String>success(42)

Validation<String,String> f1 = Validation.<String,String>failure("a", "b", "c")

Validation len = Validation.success( {s -> s.length() } )
Validation rev = Validation.success( {s -> s.reverse() } )

println "Demo of Validation in groovy"
println ""

println "fold examples"
println "folding on " + s1 + " gives: " + (s1.fold( { errors -> "error count: " + errors.toList().size() }, { value -> "success: " + value } ))
println "folding on " + f1 + " gives: " + (f1.fold( { errors -> "error count: " + errors.toList().size() }, { value -> "success: " + value } ))
println ""

println "orElse examples"
println s1.toString() + " orElse " + s2 + " gives: " + s1.orElse(s2)
println f1.toString() + " orElse " + s2 + " gives: " + f1.orElse(s2)
println ""

println "getOrElse examples"
println s1.toString() + " getOrElse 'default' gives: " + s1.getOrElse {"default"}
println f1.toString() + " getOrElse 'default' gives: " + f1.getOrElse {"default"}
println ""

println "map examples"
println s1.toString() + " map { it.length() } gives: " + (s1.map { it.length() })
println f1.toString() + " map { it.length() } gives: " + (f1.map { it.length() })
println ""

println "ap examples"
println s1.toString() + " ap Success({ it.length() }) gives: " + s1.ap( len )
println s1.toString() + " ap Success({ it.reverse() }) gives: " + s1.ap( rev )
println f1.toString() + " ap Success({ it.reverse() }) gives: " + f1.ap( rev )
println ""

println "examples of accumulation of errors"
println "ap does accumulate: " + f1 + " ap " + f1 + " gives : " + f1.ap(f1)
println "flatMap doesn't accumulate, and isn't as useful (for Validation): " + f1 + " flatMap { it -> " + f1 + " } gives: " + f1.flatMap({f1})
println ""

println "applicative builder examples"
//TODO: is there a better name for 'with'? NOT '|@|'
println s1.toString() + " with " + s2 + " with " + s3 + ", then applying gives: " + s1.with(s2).with(s3).applyAll { a,b,ans -> a + " " + b + ": " + ans }
println f1.toString() + " with " + s1 + " with " + f1 + ", then applying gives: " + f1.with(s1).with(f1).applyAll { a,b,ans -> a + " " + b + ": " + ans }
println s1.toString() + " with " + s2 + " with " + s3 + ", then flatmapping gives: " + s1.with(s2).with(s3).flatMapAll { a,b,ans -> a + " " + b + ": " + ans }
println f1.toString() + " with " + s1 + " with " + f1 + ", then flatmapping gives: " + f1.with(s1).with(f1).flatMapAll { a,b,ans -> a + " " + b + ": " + ans }
println ""

println ""

// further examples - lists of checks
def check1 = { it -> if(it.length() < 10) Validation.success(it.length()) else Validation.failure("too long") }
def check2 = { it -> if(it.startsWith("hello")) Validation.success("world") else Validation.failure("unfriendly") }
def check3 = { it -> if(it.contains(" ")) Validation.success(it) else Validation.failure("I have no words") }
final def input1 = "hello :)"
final def input2 = "helloworld!"
def checks = [check1, check2, check3]
println ApplicativeBuilder.allWithInput(checks, input1).applyAll {a,b,c -> b + c.substring(5, a) }
println ApplicativeBuilder.allWithInput(checks, input2).applyAll {a,b,c -> b + c.substring(5, a) }