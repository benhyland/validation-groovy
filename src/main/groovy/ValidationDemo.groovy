public final class Demo {

	private Demo() {}

	public static final class NEL<T> {
		private final T head
		private final List<T> tail
		private NEL(T head, List<T> tail) {
			this.head = head
			this.tail = tail
		}

		public NEL<T> append(T item) {
			def list = tail()
			list.add(item)
			return new NEL<T>(head, list)
		}
		public NEL<T> prepend(T item) { return new NEL<T>(item, toList()) }
		public T head() { return head }
		public List<T> tail() {
			def list = []
			list.addAll(tail)
			return list
		}
		public List<T> toList() {
			def list = [head]
			list.addAll(tail)
			return list
		}
	}

	// Closure.curry(arg) is actually only for partial application :(
	public static final class CurriedForReal extends Closure {
		private final Closure f;
		
		public CurriedForReal(Closure f) {
			super(f.owner, f.delegate);
			this.f = f;
		}

		public def call() { this.f.call() }

		public def call(arg) {
			if(this.f.getMaximumNumberOfParameters() < 2) {
				return this.f.call(arg)
			}
			else {
				return new CurriedForReal(this.f.curry(arg))
			}
		}
	}

	public static abstract class Validation<E,A> {
		//private Validation() {} prevented by GROOVY-5728

		// defining catamorphism for ADT
		public abstract <T> T fold(Closure<T> ifFailure, Closure<T> ifSuccess)

		// could implement with nested fold but meh, let's add to subclasses
		public abstract <B> Validation<E,B> ap(Validation<E,Closure<B>> f)

		public void ifFailure(Closure ifFailure) {
			fold(ifFailure, {});
		}
		
		public void ifSuccess(Closure ifSuccess) {
			fold({}, ifSuccess);
		}

		public Validation<E,A> orElse(Validation<E,A> other) {
			return fold( { other }, { this } );
		}

		public A getOrElse(Closure<A> other) {
			return fold( other, { value } );
		}

		public <B> Validation<E,B> map(Closure<B> f) {
			return fold( { this }, { success(f(value)) } );
		}

        // bind doesn't really make much sense with a semigroup as the error type
        // but we'll keep it in and just not accumulate errors from multiple validations
        // We don't guarantee that only a single error results from any failure encountered,
        // but we do guarantee to stop processing at the first failure.
		public <B> Validation<E,B> bind(Closure<Validation<E,B>> f) {
			return fold( { this }, { f(value) } );
		}

		public ApBuilderArityN with(Validation b) {
			return new ApBuilderArityN(new NEL<Validation>(this, [])).with(b);
		}

		// builder for arbitrary arity gathering of validation values
		// unsafe to avoid requirement for separate classes for each arity, and anyway, this is groovy
		public static class ApBuilderArityN {
			private final NEL<Validation> validations;

			public ApBuilderArityN(NEL<Validation> validations) {
				this.validations = validations;
			}
			
			public ApBuilderArityN with(Validation v) {
				return new ApBuilderArityN(validations.append(v));
			}

			public Validation applyAll(Closure f) {
				def result = validations.head().ap( success({ value -> new CurriedForReal(f).call(value) }) );
				
				validations.tail().each { validation ->
					result = validation.ap( result );
				}
				
				return result;
			}

            public Validation bindAll(Closure f) {
                def result = success(new CurriedForReal(f))
                validations.toList().each { validation ->
                    result = result.bind { curriedF -> validation.ap(success(curriedF)) }
                }
                return result;
            }

            public static ApBuilderArityN allWithInput(Collection<Closure<Validation>> fs, input) {
                return fs.collect { f -> f(input) }.inject { a,b -> a.with(b) }
            }
		}

		public static class Success<E,A> extends Validation<E,A> {
			private final A value
			private Success(A success) {
				if(success == null) { throw new IllegalArgumentException("successes must not be null") }
				this.value = success
			}

			public String toString() { return "Success("+value+")" }
			public <T> T fold(Closure<T> ifFailure, Closure<T> ifSuccess) { return ifSuccess(value) }
			public <B> Validation<E,B> ap(Validation<E,Closure<B>> fval) {
				return fval.fold(
					{ fval },
					{ f -> success(f(value)) }
				);
			}
		}
		
		public static class Failure<E,A> extends Validation<E,A> {
			private final NEL<E> errors
			private Failure(NEL<E> errors) {
				this.errors = errors
			}
		
			public String toString() { return "Failure("+errors.toList()+")" }
			public <T> T fold(Closure<T> ifFailure, Closure<T> ifSuccess) { return ifFailure(errors) }
			public <B> Validation<E,B> ap(Validation<E,Closure<B>> fval) {
				return fval.fold(
					{ fvalErrors ->
						def list = fvalErrors.tail()
						list.addAll(errors.toList())
						failure(fvalErrors.head(), *list)
					},
					{ this }
				);
			}
		}

		public static <E,A> Validation<E,A> success(A value) {
			new Success<E,A>(value)
		}

		public static <E,A> Validation<E,A> failure(E failure, E... moreFailures) {
			if(failure == null || moreFailures.contains(null)) { throw new IllegalArgumentException("failures must not be null") }
			new Failure<E,A>(new NEL<E>(failure, moreFailures.toList()))
		}
	}
}

//Demo.Validation<String,String> s = Demo.Validation.<String,String>success() // illegal
//Demo.Validation<String,String> s = Demo.Validation.<String,String>success(null) // illegal
//Demo.Validation<String,String> s = Demo.Validation.<String,String>success("a", "b") // illegal
//Demo.Validation<String,String> f = Demo.Validation.<String,String>failure() // illegal
//Demo.Validation<String,String> f = Demo.Validation.<String,String>failure(null) // illegal
//Demo.Validation<String,String> f = Demo.Validation.<String,String>failure("a",null,"c") // illegal

Demo.Validation<String,String> s1 = Demo.Validation.<String,String>success("hello")
Demo.Validation<String,String> s2 = Demo.Validation.<String,String>success("world")
Demo.Validation<String,Integer> s3 = Demo.Validation.<String,String>success(42)

Demo.Validation<String,String> f1 = Demo.Validation.<String,String>failure("a", "b", "c")

Demo.Validation len = Demo.Validation.success( {s -> s.length() } )
Demo.Validation rev = Demo.Validation.success( {s -> s.reverse() } )

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
println "bind doesn't accumulate, and isn't as useful (for Validation): " + f1 + " bind { it -> " + f1 + " } gives: " + f1.bind({f1})
println ""

println "applicative builder examples"
//TODO: is there a better name for 'with'? NOT '|@|'
println s1.toString() + " with " + s2 + " with " + s3 + ", then applying gives: " + s1.with(s2).with(s3).applyAll { a,b,ans -> a + " " + b + ": " + ans }
println f1.toString() + " with " + s1 + " with " + f1 + ", then applying gives: " + f1.with(s1).with(f1).applyAll { a,b,ans -> a + " " + b + ": " + ans }
println s1.toString() + " with " + s2 + " with " + s3 + ", then binding gives: " + s1.with(s2).with(s3).bindAll { a,b,ans -> a + " " + b + ": " + ans }
println f1.toString() + " with " + s1 + " with " + f1 + ", then binding gives: " + f1.with(s1).with(f1).bindAll { a,b,ans -> a + " " + b + ": " + ans }
println ""

println ""

// further examples - lists of checks
def check1 = { it -> if(it.length() < 10) Demo.Validation.success(it.length()) else Demo.Validation.failure("too long") }
def check2 = { it -> if(it.startsWith("hello")) Demo.Validation.success("world") else Demo.Validation.failure("unfriendly") }
def check3 = { it -> if(it.contains(" ")) Demo.Validation.success(it) else Demo.Validation.failure("I have no words") }
final def input1 = "hello :)"
final def input2 = "helloworld!"
def checks = [check1, check2, check3]
println Demo.Validation.ApBuilderArityN.allWithInput(checks, input1).applyAll {a,b,c -> b + c.substring(5, a) }
println Demo.Validation.ApBuilderArityN.allWithInput(checks, input2).applyAll {a,b,c -> b + c.substring(5, a) }