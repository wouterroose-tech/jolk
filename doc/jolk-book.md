# JOLK

**Identity Congruence, Unified Messaging, and Metaobject Protocol for the Java Type System**

The Jolk specification formalises the projection of a message-passing paradigm onto the Java type system. *Identity Congruence* mandates that every entity acts as a first-class, message-receptive identity. The language expresses a message-dispatch execution model through a C-family structural syntax, which the Tolk Engine runs on top of the Java Virtual Machine. Message dispatch governs execution semantics within the runtime overlay.

The execution model adheres to the message-passing paradigm described by Alan Kay.[1] Every operation—including object instantiation, arithmetic evaluation, and control flow—proceeds through message dispatch mechanisms.

**Acknowledgments**

Bedankt Wilfried Verachtert, voor de vele sessies waarin we sinds ons eerste kandidaatsjaar aan de VUB de software-industrie nog maar eens hebben heruitgevonden; Michel Tilman, mentor en collega, voor al je advies door de jaren heen; en Theo D'Hondt, om mij te leren meta-nadenken. Special thanks to Gilad Bracha for his invaluable advice at the very start of this project.

---

# Table of contents

* [Introduction](#introduction)
    * [Design philosophy](#design-philosophy)
    * [The unified messaging engine](#the-unified-messaging-engine)
* [Part One: Syntactic and Semantic Foundations](#part-one)
    * [Grammar](#grammar)
    * [Semantics](#semantics)
    * [Design synopsis](#design-synopsis)
* [Part Two: Type System](#part-two)
    * [Type categories](#type-categories)
    * [Messaging](#messaging)
    * [Metaobject protocol](#metaobject-protocol)
    * [Core type system](#core-type-system)
    * [Architectural highlights](#architectural-highlights)
    * [Heritage & foundation](#heritage--foundation)
* [Part Three: System Synthesis and Patterns](#part-three)
    * [Example](#example)
    * [Fragments](#fragments)
    * [Design patterns](#design-patterns)
    * [The Meta-level synthesis](#the-meta-level-synthesis)
	* [JolkUnit, the Jolk unit test framework](#jolkunit-the-jolk-unit-test-framework)
    * [Developer experience](#developer-experience)
* [Part Four: Tolk - The Engine Mechanics](#part-four)
    * [Implementation](#implementation)
    * [Tolk parser](#tolk-parser)
    * [Tolk Engine](#tolk-engine)
    * [A future-proof JVM synthesis](#a-future-proof-jvm-synthesis)
* [References](#references)
* [Glossary of terms](#glossary-of-terms)
* [Copyright](#copyright)

---

# Introduction

**Jolk: Fluidity & Integrity**

---

Jolk /dʒɔːk/ (the name is a blend of 'Java' and the Dutch word 'tolk', denoting an interpreter and referencing the 'talk' in Smalltalk-80[2]) is the formal specification of a language that reifies message-passing, behavioural semantics on top of the underlying Java type system.

Jolk extends the research initiated in ProtoTyping[3]. Leveraging on Strongtalk[4] protocol principles, Jolk complements the Java type system. The Tolk Engine enforces static type safety through compile-time validation of message signatures and leverages the Truffle framework to reify unified messaging within a self-optimising abstract syntax tree (AST). Through dynamic specialisation and GraalVM partial evaluation, the engine executes semantic flattening, collapsing high-level protocols into machine code to target runtime parity with native operations.

The manuscript structure reflects the system architecture: Parts One and Two establish the syntax and language specifications; Part Three demonstrates system synthesis through design patterns and frameworks; and Part Four provides an analysis of the Tolk Engine mechanics and implementation.

## Design philosophy

Jolk is a convergent architecture synthesizing C-family syntax with messaging semantics, engineered for the GraalVM ecosystem.

### Core principles

**Unified messaging:** Every interaction—including object instantiation, arithmetic evaluation, control flow, and error handling—is a formal message send.

**Unified type system:** Every entity—including closures, booleans, the Nothing singleton, and classes via their metaclasses—is an identity receptive to messages.

**Data confinement:** State isolation is enforced through exclusive field binding during construction and message-mediated mutation.

**Strong typing:** A static protocol layer leverages type rules to guarantee type and behavioral safety prior to execution.

**Syntactic alignment:** A C-derived syntax with a restricted keyword set and standard lexical tokens.

### Architectural outcomes

Structural complexity is managed through the following design constraints:

**Keyword minimisation:** The model restricts its lexical footprint by excluding procedural keywords and relying on structural scaffolding for type definition.

**Operational uniformity:** Computation operates within the unified messaging protocol. This structural integration restricts the runtime engine to a singular execution mechanic for both object evaluation and branch execution.

**Semantic flattening:** Leveraging a Truffle-based implementation, the architecture targets execution parity with native JVM operations by collapsing high-level messaging protocols into optimized machine code through GraalVM’s partial evaluation of the generated abstract syntax tree (AST).

**Interoperability:** By targeting the GraalVM ecosystem, the platform achieves integration with host Java types, ensuring stable interaction within the JVM.

**Ecosystem alignment:** The specification is designed for forward-compatibility with evolving JVM features, specifically Project Valhalla (Value Objects) and Project Amber (Pattern Matching).

This unified messaging model ensures that every state transition and control branch is a negotiable handshake, allowing the Tolk Engine to specialize dynamic logic into high-performance machine code.

Empirical software engineering literature demonstrates that lexical token density correlates with defect frequency [5] and object-oriented complexity metrics correlate with defect density [6]. Comparative research indicates that higher programming language levels reduce statement volume, yielding lower defect rates per functional unit [7]. The unified messaging model eliminates explicit operator syntax and structural overhead, aligning language mechanics with these defect reduction parameters.

## The unified messaging engine

*Jolk defines the object not as a container, but as an identity manifested through message-driven interactions.*

*The Receiver constitutes the terminus for invocations. The metaobject protocol (MOP) [11] provides the intrinsic reflection necessary to map high-level abstractions to the JVM. The Identity asserts the instance as a first-class, non-nullable entity. Adherent to Kay’s vision, these components are mediated by a meta-level boundary that enforces local retention, thereby effecting the 'disappearance of data.'* 

*The unified messaging protocol subsumes keyword-driven control flow with an exchange of messages, formalising branching as a first-class participant within a unified messaging.*

---

# Part One

**Syntactic and Semantic Foundations**

---

## Grammar

The Jolk grammar defines the language's syntax, integrating class-based structural layout with a unified message-dispatch paradigm.

    (* Jolk Grammar *)
    (* ============ *)
	
	unit                  = [ package ] { expansion } { projection } { annotation } ( type_declaration | extension_declaration)
	package               = ("package" | "~") namespace  ";"
	expansion             = ("using" | "+") inclusion
	projection            = ("using meta" | "&") inclusion
	inclusion             = [ alias ] namespace  [ ".*" ] ";"
	alias                 = meta_id "="
	namespace             = identifier { "." identifier }
       
	type_declaration      = [ visibility ] [ finality ] type_category type_bound "{" { type_member } "}"
	visibility            = "public" | "package" | "protected" | "private"
	finality              = "abstract" | "final"
	type_category         = "class" | "value" | "record" | "enum" | "protocol"
	type_bound            = type [ type_contracts ]
	type                  = "Self" | [ namespace ] meta_id [ type_arguments ]
	type_arguments        = "[" type_bound { "," type_bound } "]"
	type_contracts        = [ "extends" type ] [ "implements" type { "&" type } ]
	type_member           = { annotation } ( member | enum )
	member                = ( [ "meta" ] state ";" | [ visibility ] [ finality ] [ "meta" ] method )	
	state                 = constant | field
	constant              = "constant" type identifier assignment
	assignment            = "=" message
	field                 = [ "stable" | "lazy" ] type identifier [ assignment ]
	enum                  = meta_id [ arguments ] ";"
	method                = [ "lazy" ] [ type_arguments ] [ type ] method_id "(" [ typed_params ] ")" ( block | ";" )
	method_id             = identifier | operator
	typed_params          = annotated_type ( instance_id { "," annotated_type instance_id } [ "," annotated_type vararg_id ] |  vararg_id )
	annotated_type        = { annotation } type
	annotation            = "@" identifier [ "(" ... ")" ]
	vararg_id             = "..." instance_id
	extension_declaration = "extension" meta_id "on" type "{" { extension_member } "}"
	extension_member      = { annotation } [ visibility ] [finality] method

	block                 = "{" statements "}"
	statements            = { statement ";" } [ statement [ ";" ] ]
	statement             = state | binding | [ "^" ] message
	binding               = identifier assignment
	message               = ( primary | ( "!" | "-" ) message ) { (selector | operator) [ payload ] }
	primary               = reserved | identifier | literal | list_literal | "(" message ")" | closure | method_ref
	closure               = "{" [ stat_params "->" ] statements "}"
	method_ref            = [ identifier | reserved ] "##" identifier
	payload               = arguments | closure | message
	arguments             = "(" [ message { "," message } ] ")"
	stat_params           = typed_params | inferred_params
	inferred_params       = instance_id { "," instance_id }

	reserved              = "true" | "false" | "null" | "super" | "self" | "Self"
	selector              = "#" identifier
	identifier            = meta_id | instance_id 
	meta_id               = upper_alpha { alpha | digit }
	instance_id           = lower_alpha { alpha | digit }
	literal               = number_literal | string_literal | char_literal
	list_literal          = array_literal | set_literal | map_literal
	array_literal         = "#[" [ literal_list ] "]"
	set_literal           = "#{" [ literal_list ] "}"
	map_literal           = "#(" [ map_list ] ")"
	literal_list          = message { "," message }
	map_list              = map_entry { "," map_entry }
	map_entry             = message "->" message
	number_literal        = digit { digit } [ "." digit { digit } ]
	string_literal        = "\"" { char } "\""
	char_literal          = "'" char "'"

	modifier              = "#" (visibility_ops)? (finality_ops)?
	visibility_ops        = "<" | "~" | ":" | ">"
	finality_ops          = "?" | "!"

The grammar synthesises class-based structural layout with message dispatch, positioning all entities as types. It enforces syntactic signatures by decoupling lexical primitives from structural layout rules and isolating atomic tokens from high-level abstractions like selectors (`#`), block markers (`{ }`) and generics (`[ ]`). All execution interactions are normalised as message sends, with mathematical and logical precedence operating as semantic rules within the execution engine to maintain algebraic expectations in a flat syntactic field.

Symbolic anchors define access boundaries, and the grammar provides keyword aliases for visibility modification, aligning with object-oriented topologies (e.g., `#<` maps to `private`). Modifiers enforce compile-time invariants, while the synthesis of class-based declarations with message-passing semantics enables syntactic validation and static optimisation. A restricted vocabulary of structural anchors and reserved identifiers, such as `class` for type definitions and `self` for the active message receiver, unifies interaction.

### Keywords

*Reserved Object Identifiers* define object identity and reflective semantics, while *Structural Scaffolding* keywords establish the architectural metadata required for platform integration.

**Reserved Object Identifiers:** These keywords represent the immutable mechanics of the object model, serving as pre-defined identifiers for first-class identities.

* `self`: Represents the current instance.  
* `super`: Represents the parent context/identity.  
* `Self`: Represents the reflective identity of the type.  
* `null`: Represents the absence of an object value.  
* `true` / `false`: The boolean object literals.

**Structural scaffolding:** These markers establish the structural boundaries for static organization without participating in the runtime message-passing flow.

* *Directives / Projections*: `using`, `using meta`
* *Structure / Metadata*: `package`, `class`, `value`, `record`, `enum`, `protocol`, `extension`
* *Relations / Hierarchy*: `on`, `extends`, `implements` are hierarchy markers  
* *Access / Visibility & Finality*: `public`, `protected`, `private`, `package`, `abstract`, `final`
* *State & Behavior Modifiers*: `meta`, `stable`, `constant`, `lazy`
* *Annotations*: anchored by the at-symbol (`@`).

### Lexical anchors

**Operators:** Expressed as mathematical or logical symbols (e.g., `+`, `-`, `==`, `!=`, `~~`, `!~`, `?`).

**Message selectors:** Designated by the hashtag prefix (`#`) followed by an identifier to execute a message send (e.g., `this #name #uppercase #print`).

**Method references:** Designated by the double hashtag (`##`) extending the selector anchor to reify a message dispatch into a first-class `Closure` identity (e.g., `String ##toUpperCase`). Jolk utilises the double hashtag to maintain lexical consistency and symmetry with message the selector syntax.

**Return:** The caret `^` denotes the explicit return symbol. To ensure lexical uniqueness, the symbol `|!` is designated for the bitwise XOR operation; this uses the pipe (`|`) for OR and the exclamation mark (`!`) for NOT to visually denote "OR but NOT both", aligning with the mathematical definition of XOR. 

The **Semantic casing** is a lexical rule where the first-letter casing of an identifier determines its semantic category and role.

* Meta-Objects: Types, constants and class selectors start with an Uppercase letter (e.g., `String`, `PI`, `#PI`)  
* Variables, parameters, properties & instance selectors: start with a lowercase letter (e.g., `name`, `#name`).

### Structural anchors

Syntactic elements act as structural anchors for the parser.

**& operator** instead of a comma for protocol implementation emphasizes that a type is a logical conjunction of behavioral contracts, shifting the focus from a procedural list to a composition of protocols while reinforcing the separation between a singular implementation lineage (inheritance) and a multi-faceted subtyping lattice (protocols).

**Generic type brackets** The syntax adopts angle brackets (`[ ]`) for generics.

**Syntactic geometry:** Notational exclusivity ensures that the semantic interpretation of every structural boundary remains absolute and invariant. The grammar defines a unique token geometry to each structural construct: `[ ]` for generics, `{ }` for structural bounds and closures, and `( )` for parameters.

	List[Result] process(List[Signal] signals) {
		^ signals #map { s -> Result #new(s #id) }
	}

**Collection literals:** A collection literal (Array `#[ ]`, Set `#{ }` or Map `#( )`) is a shorthand for the underlying message-based variadic creation of a primary object. 

**Assignment:** Syntactically, the assignment symbol (`=`) acts as a structural anchor by occupying the lowest possible precedence. This ensures that the entire expression chain to the right is evaluated before the result is bound to an identifier. Assignments are viewed as a meta-level change from functions. In this sense, the (`=`) symbol acts as a 'fence' that guards the crossing of a boundary from pure functional evaluation to a state-changing operation.

**Semicolon:** The semicolon (`;`) is mandatory for structural metadata, such as package and import declarations, as well as instance state declarations like fields and enums. Within method bodies, standard statements including variables, assignments, and expressions are firmly anchored by the semicolon to ensure clarity and structural integrity, but it is optional for the final statement of a block.

## Semantics

Priority is given to source code clarity over the brevity of general type inference. By mandating explicit type signatures and the use of the caret (`^`) operator for query methods, the model enforces *protocol transparency*. This ensures that the 'contract of the message' remains visible at every step of the execution flow, allowing the developer to track the state of the message chain without the need to resolve implicit types.

Reserved identifiers are tokens that occupy a middle ground between the grammar and the object model. While they are not rigid structural keywords like `class`, they are names with pre-defined semantic meaning that the Tolk toolchain protects.

### Unified message-passing

The unified message-passing, where every interaction follows the *receiver \#message* pattern enforces encapsulation. Whether instantiating an object (`User #new`), accessing a property (`user #name`), executing an arithmetic operation (`1 + 2`), or evaluating control flow (`switch #case`), the underlying mechanism remains an identical message dispatch.

The assignment operator (`=`) performs local variable binding and field initialisation at declaration. Object state modifications post-declaration proceed through self-directed message dispatch (e.g., `#x(value)`). This ensures that all state modifications are mediated through the message protocol defined by the type, preserving structural isolation and encapsulation. If a local variable is bound without prior declaration, the engine applies implicit type inference based on the result of the assigned expression.

### Keyword-less control flow

Implemented as an intrinsic messaging protocol, control flow shifts from a syntactic language constraint to an emergent property of object interaction. Operational logic functions as a library feature, ensuring that fundamental branching conforms to the identical rules of encapsulation governing user-defined code.

**Branching:** Traditional if-else blocks are replaced by the ternary pattern `condition ? expression : expression`; with `?` and `:` as grammatical delimiters that are semantically resolved as messages sent to Booleans.  
**Looping:** Native while and for statements are superseded by messages sent to Closures or Integers, such as `[cond] #while [body]` or `10 #times [n -> ...]`.   
**Error handling:** Exception logic is unified with the messaging model, with `#catch` and `#finally` messages sent to closures.

### Mathematical and equality operators

Identity (`==` and the negation `!=`) is distinguished from equivalence (`~~` and the negation `!~`), which execute a reference parity check and a structural state comparison respectively. All mathematical symbols are treated as overloaded selectors, allowing custom types to interact like native primitives. The engine evaluates these message chains according to a priority list—ranging from exponential powers to logical disjunctions—ensuring that algebraic and logical expectations are preserved within the message dispatching.

### Parameter immutability

All identifiers defined within a parameter\_list are implicitly immutable. Any attempt to use these identifiers as the target of an assignment will result in a compile-time error. This ensures that the initial contract of the method remains transparent and prevents side effects common in languages that allow parameter reassignment.

### Type system

**Types**: The type system defines the `class`, `enum`, `record`, `value`, and `protocol` user-defined type categories. Generic type bounds enforce static behavioural contracts across class hierarchies, protocol conjunctions, message receivers, and parameter payloads.

**Protocol conjunctions** utilize the ampersand operator (`&`) to create 'branded' types that represent an intersection of contracts. Aligning with the concept of Traits[8], this facilitates the composition of behaviour without the state conflicts inherent in multiple inheritance. This provides a structural guarantee ensuring the identity of the participant and the contract of the message remain transparent and secure, preventing semantically incompatible objects from matching based on syntax alone. Finally, the syntax supports extensions, permitting type expansion via new message protocols.

**Identity congruence** is the foundational architectural mandate that ensures a singular logical representation for all entities—including complex objects, primitives, and the absence of value—regardless of their physical storage format. It establishes an alignment between the abstract identity defined by the programmer and the physical representation in the runtime engine.

**Intrinsic primitives** like `Boolean`, `Long` and `String`- are first-class identities that participate in the messaging protocol.

**Absence reification:** The literal `null` represents the absence of a value as a singleton instance of the `Nothing` class. By implementing `null` as a first-class object identity, all values act as valid message receivers, ensuring that operations on absent values resolve through defined protocols rather than NullPointerExceptions.

The **modifier protocol** defines a specification for member management:

`meta`: Designates non-instance members, defining their association with type-level metadata and enforcing member segregation between the instance and meta-level.  
`constant`: Establishes a field as non-assignable (shallow immutability)  
`stable`:  a field or local remains unchanged after its initial binding  
`lazy`	: Designates deferred member initialisation, facilitating the creation of an identity only upon the reception of its primary message.  

### The Self type

`Self` is a reserved type alias that, within the type definition, denotes the receiver's own type — in a subtype, `Self` resolves to that subtype, not to the definition in which it appears. It thus states self-referential constraints directly: where a system without a `Self` type must encode "any subtype of me is comparable to me" as an F-bounded generic bound[9], as in `T extends Comparable<T>`, Jolk expresses it through `Self`.

Building on Bruce's work on `MyType` and matching[15], the `Self` type alias guarantees that a return value has the type of the receiver, so subclass invocations yield the specialised subtype identity — this makes self-referencing returns, type-safe binary operations, and fluent interfaces safe across inheritance hierarchies without manual signature overrides in subtypes. The distinction between `Self` (the type) and `self` (the instance) follows the semantic casing rule. Because covariant parameter types are unsound in general, `Self` in parameter position is restricted to final methods and final types, preserving static type safety within the subtype lattice.

### Closure

A closure is a first-class block of code that captures the lexical environment in which it is created. Its return value is the evaluation of its last expression, unless an explicit return (`^`) is encountered. Closures are brace-delimited `{ }` and can act as receivers for control-flow messages. Parameters are separated from the body by an arrow `->` — for example, `10 #times {n -> n + 1}`. When a closure is the only argument of a message, the parentheses around the argument list may be omitted; the send still resolves to the standard message form.

### Meta-directives

Meta-directives establish the context that governs the relationship between the source and the platform. *Expansion* via the `+` / `using` anchor incorporates external types into the local vocabulary by mapping a terminal identity to a fully qualified path. This is augmented by *Projection* through `&` / `using meta`, which projects platform facts—whether static constants or functional methods—and as local symbols. *Visibility* (e.g., `private`/ `#>`) and *Finality* (e.g., `final` / `#!`) are structural properties which mandate the state of access and identity.

### Architectural integrity

The execution model enforces a semantic framework where the message boundary—the line between internal state and the communication protocol—remains structurally inviolable. State isolation and finality operate as deterministic execution properties.

This architecture prioritises absolute boundary enforcement. External agents cannot bypass defined object protocols, preserving internal state integrity. The paradigm prioritises message-passing interaction over direct state manipulation, targeting the messaging interface as the primary meta-level boundary where communication resides. By shifting emphasis from concrete object instances to message protocol precision, the architecture achieves structural composition without exposing implementation details to external callers.

## Design synopsis

The specification defines a pure object-oriented evolution for the JVM that fuses the *message-oriented* paradigm of Smalltalk-80 with the structural rigour of the C-family. By treating every interaction—from basic arithmetic to complex control flow—as a unified message send while enforcing standard mathematical precedence as a *semantic rule*, Jolk establishes a single semantic paradigm for all operations without discarding existing industrial conventions. The orthogonal design decouples lexical primitives from underlying semantic protocols like message dispatches or array literals and achieves a compact footprint through a lean keyword palette and a semantic casing rule. The model utilizes 'lexical fences'—such as the hashtag selector (`#`) and the assignment operator (`=`)—to achieve zero token ambiguity. This enables deterministic, linear-time parsing without symbol table lookups. 

Encapsulation is enforced via a message boundary that separates an object’s internal state from the external environment. Within this framework, the absence of a value is handled not as a system-collapsing null, but as a valid singleton instance (`null`) that behaves as a first-class object. Control flow is implemented as a library feature; branching and looping are reified through messages sent to Booleans, Integers, or Closures. Furthermore, protocol conjunctions (`&`) represent the composition of behavioural contracts. This ensures that while the developer experiences the receiver-centric flexibility of late-bound messaging, the Tolk Engine performs semantic flattening to target high-performance execution.

---

# Part Two

**Type System** 

---

Jolk is a semantic overlay for the Java type system: it prioritises messaging over internal state. Every value — whether it originates in Java or in Jolk — is governed by a single messaging protocol, so the message boundary is the only means of interaction.

That boundary enforces absolute field privacy. An object's internal state is never directly accessible: all state interaction passes through synthesised selectors. Reflection is consequently not merely discouraged but unavailable — an external agent cannot bypass the protocol, because no other path to the state exists. Every interaction, from arithmetic to object instantiation, takes the form `receiver #message`. Instantiation itself is no exception: an object is created by sending the canonical `#new` message to its metaclass.

The JVM manages the physical layout and integrity of objects; the Jolk type system governs their behavior as observable through messages. Following Strongtalk, Jolk separates implementation lineage (subclassing) from behavioral protocols (interface conformance). This separation allows the Tolk Engine to place Java and Jolk entities in one unified type lattice, without translation shims.

## Types

Classes, records, enums, and value types share a single, unified syntax, but are semantically governed by lattice separation: an object's role in the system is defined by its behavioral protocol, not by its lineage. A set of core capabilities — equivalence among them — is standardised across every type, and fields are encapsulated implicitly. 

Jolk supports parameterised types through generics, with bounds constraining their type parameters. Protocols combine with the `&` operator, expressing behavioral conjunction within the subtype lattice. Extension protocols attach behavior to existing final types without modifying their source code.


### Type categories

The nature of an identity is defined by its type category: `class`, `record`, `enum`, or `value`. Each category serves a different architectural role, from mutable to immutable identities. Every type maps to a specific construct, chosen to avoid abstraction penalties, yet all are harmonised under a single messaging protocol. Regardless of its internal representation, every entity presents the same encapsulated interface — the distinction between a complex object and a primitive value is an implementation detail, not a semantic one.

**class**

A Class is a first-class identity that acts as both a blueprint for state and a receiver for messages. A class is a singleton instance of a metaclass. Since the class itself is an object, "constructors" are replaced by standard creation messages like `#new`.

	class Person {
	
	    String name;
	
	    // creation method  
	    meta Person new(String name) {  
	        ^ super #new #name(name)
	    }
	
	    ...  
	}

**record**

a Record is a specialised, immutable type optimised for data transfer. It functions as a class where immutability is enforced by design. While field declarations follow the same syntax as regular classes—requiring a terminating semicolon and adhering to semantic casing—the compiler implicitly treats them as final. This ensures that once the state is anchored via the automatic, canonical `#new` creation method, it remains constant.

Encapsulation is maintained by prohibiting direct field access. Instead, the system synthesises automatic message selectors for every slot, ensuring all state retrieval flows through the message protocol (e.g., `p #x`).

	/// definition  
	record Point {  
	    Int x;   
	    Int y;
	
	    // The synthesised canonical creation method  
	    // #new(Int x, Int y)  
	    //  
	    // Point p = Point #new(10, 20);
	
	    // The synthesised public getter methods  
	    // #x, #y
	    //  
	    // Int currentX = point #x;  
	}
	
	/// usage
	Point p = Point #new(10, 20);
	Int x = point #x;  


**enum**

an Enum is a specialised type representing a fixed set of shared singletons reified as uppercase lexically anchored identities. To maintain semantic casing, these constants function as Meta-Objects within the unified messaging protocol. Members are typically accessed by sending a meta selector to the type (e.g., `Day #MO`), but importing a specific member opens a lens on that identity. This allows the member name to be used directly, eliminating the need for the type prefix.

Structurally, enum constants use a shorthand notation for public meta constant declarations separated by semicolons. To ensure Identity Congruence, the system synthesises an immutable, canonical creation method (`#new`). Synthesises message selectors and resolvers—such as `#valueOf` ensure enums are first-class participants in the message flow.

	enum Day {
	
	    // Semicolon separates constants  
	    // shorthand notation for the synthesised canonical creation method  
	    // public meta constant Day MO = Day #new("Monday", 1);  
	    MO("Monday", 1);  
	    TU("Tuesday", 2);
	
	    // Fields are private by default  
	    stable Int index;  
	    stable String label;
	
	    // message send  
	    // today = Day #MO;  
	    //  
	    // lens projection  
	    // today = #MO;
	
	    // synthesised resolver
	    // meta level valueOf(String name) { } 

        // default accessor for enum fields
		// Int index() [ ^index ]
        // String label() [ ^label ]
	
	}

**value**

Value types represent immutable data structures. They are inspired by Java's Project Valhalla and are intended to provide the memory efficiency of primitives while behaving as objects in the language's unified messaging model. 

Syntactically, value types follow the same unified messaging and declaration rules as other types. They support operator overloading, allowing complex types to feel like native primitives.

**protocol**

A Jolk protocol acts as a contract. It defines a set of messages (methods) that a type must support, mirroring the behavior of a Java interface. The grammar allows these protocols to contain method signatures terminated by a semicolon, which signify a contract without implementation. If the method includes a block, it provides a functional implementation directly within the protocol, behaving like a Java default method.

### State

To maintain rigorous encapsulation, fields are absolutely private. Every state change is implemented as a message send (e.g., `#x(5)`). This ensures that the message boundary is never bypassed, as all state transitions remain formal message dispatches governed by the object's protocol.

To facilitate a fluid conversation with self, jolk supports the implicit self-receiver. When a message begins directly with a selector hashtag (e.g., `#name`), the receiver is implicitly resolved to `self`. This provides a high-density idiomatic shorthand for internal state management: retrieval remains a unary message (e.g., `^ #name`), and mutation remains a keyword-style message (e.g., `#name(newValue)`). By retaining the `#` anchor, the language preserves the lexical fence—visually distinguishing state interaction from local stack variables—while eliminating redundant repetition.

External state interaction is managed through implicit field encapsulation, a public protocol synthesised by the compiler that provides an automatic fluent API. All synthesised setters inherently return a covariant `Self` type, ensuring that fluid message chains maintain the type of the receiver throughout execution. While these accessors are defaulted, they may be explicitly redefined by the developer to implement validation logic, lazy instantiation, or restricted visibility without compromising structural consistency. However, when fields represent stable values within the identity, the generation of a setter is suppressed. By automating this fence, jolk ensures that state integrity is maintained through a contract of messages, effectively preventing encapsulation leaks.

	class Point { 
		// no modifiers on fields 
	    stable Int x;  
	    stable Int y;
	
	    // Synthesised default accessors
		// getter returns x
	    // public Int x();
		// getter sets x, returns self
	    // public x(Int value);
	
	    // overridden accessors  
	    protected Int x();  
	    private x(Int value);

		move(Int dx, Int dy) {  
			// Implicit self-receiver
			#x(#x + dx);  
			#y(#y + dy);

			// Explicit receiver
			self #x(self #x + dx);
		}

	    // ...  
	}

The binding protocol establishes a deterministic hierarchy for state management, anchored by a clear distinction between declaration and mutation. By requiring an explicit type for state anchoring, the language ensures that memory slots are architecturally defined, while mandatory initialisation provides a streamlined path toward referential stability.

	method() {  
	    constant Int x = 10; // constant - immutability

	    stable Int y;        // stable - immutability once bound
		#y(10);              // stable binding  

	    Int z = 10;          // field - mutability  
	    #z(20);              // binding  - value update
	
	    // ...  
	}

This distinction allows the binding production to function exclusively as a state-update mechanism for existing identifiers. Because a binding lacks a leading type or modifier, the *Tolk Engine* immediately categorises the interaction as a mutation. This allows the engine to verify the lifecycle of an identity.

### Core protocol

The Jolk core protocol establishes the object foundation, ensuring every instance is operationally complete and predictable from the moment of instantiation. This foundation is anchored by intrinsic selectors that govern state and flow: _object instantiation_ (`#new`), _equivalence_ (`~~` / `!~`), _identification_ (`#hash`), _pattern matching_ (`#isInstance` / `#instanceOf`), _representation_ (`#toString`), _identity state_ (`#isPresent` / `#isEmpty`), and _flow control_ (`#ifPresent` / `#ifEmpty`).

### Modifiers

The Lexical Anchors designate the membership scope of an identifier, establishing the lexical fence that regulates message reception and the valid reach of the identity. These symbols function as structural coordinates, maintaining semantic parity with their Java counterparts; for convenience Jolk permits the use of keywords as aliases for the Lexical Anchors.

Visibility :`#<` (`public`), `#~` (`package`), `#:` (`protected`), `#>` (`private`)  
Structural finality: `#?` (`abstract`) , `#!` (`final`)  
Or combinations like: `#~?` (`package abstract`) and `#<!` (`public final`)

By defaulting to public for types and methods, the language encourages open message passing, requiring explicit intent only when a boundary must be enforced. This convention increases structural density as the majority of members require no prefix. The package alias ensures lexical resemblance for developers accustomed to Java, providing an explicit token for modular restriction. 

To maintain encapsulation, fields default to `private` visibility, requiring a `public meta` declaration to enable lens projection. The constant modifier establishes a unified contract of immutability. At the Meta level, it enables shared access; at the Instance level, it ensures fields remain reassign-proof post-primary initialisation. Locally, it projects this stability into the execution scope as a fixed stack constant.

### Generics

Jolk incorporates the Strongtalk heritage with a static type system that distinguishes the protocol from the implementation lattice, so that an object's messaging protocol is verified independently of its implementation lineage. Jolk's protocol specification treats type arguments as first-class, reified components of an identity's contract, with square bracket notation `[ ]` serving as the lexical anchor for these generic protocols.

### Extension

Extensions add message implementations to existing JVM types, giving host identities a larger messaging protocol without modifying their underlying structure. By defining an extension on `java.lang.Object`, Jolk admits every JVM entity—from third-party classes to arrays—as a first-class message recipient. This replaces rigid hierarchies with dynamic mappings, letting the Tolk Engine project Jolk logic onto the JVM. More specific extensions on classes such as `String`, `Collection`, and `Exception` provide domain-specific message handling while preserving identity across the environment.

	@Intrinsic  
	extension StringExtension on String {
	
	    /// Iterate over each character.  
	    String forEach(Closure action) [
	        self #toCharArray #forEach [ c -> action #apply(c) ]
		]

	}

Extensions are a valid grammatical element, but their use is restricted to the `jolk.lang` core package as an intrinsic type construct: application code gains richer messaging on Java types, but cannot inject non-deterministic behaviour.

### Deferred computation

Jolk's `lazy` keyword defers initialization and computation for both fields and methods, memoizing the result. Expensive operations run only when first accessed or invoked — if never accessed, they are never computed; once computed, the value is frozen and subsequent accesses return it without re-evaluation. The mechanism is thread-safe and guards against circular dependencies during initialization. 

*   **Lazy Fields:** a compiler intrinsic for "Thunk Projection," `lazy` fields provide on-demand instance initialization, reducing startup overhead by creating dependencies only when first required. Used for values the object has — a dependency or resource — whose initialization can't happen at construction time.

*   **Lazy Methods:** a method marked lazy executes its body once and memoizes the result within the instance — suited to expensive properties that depend on object state but need not be recalculated on every access, effectively turning the method into a computed property. Used for values the object derives — expensive to compute, safe to freeze.

Deferred Computation is not a replacement for caching. A cache optimizes repeated lookups: keys, hits, misses, eviction, invalidation. lazy makes a different promise — the value is computed at most once, on first demand, and frozen thereafter — and not computed at all if never demanded. When a value must stay fresh, or memory must stay bounded, use a cache, not a lazy construct.

### Meta-level

Jolk's meta-level is built on a metaclass architecture that defines each class's superclass, an instantiation protocol, and class constants. The `MetaClass` intrinsic realises the metaobject protocol that maps Jolk abstractions onto the JVM. Because classes are reified as first-class objects, meta-level operations go through the same message dispatch as instance-level logic.

**Self**

The type alias `Self` is a reference to the current type definition: it resolves to the specific class or protocol being implemented, so method returns and parameter requirements adapt to inheritance without manual overrides. By referencing its own identity in a name-independent manner, `Self` enables expressive factory methods and protocols that remain type-safe throughout the inheritance hierarchy. `Self` resolves to the class when a `meta` method returns the class receiver or to the instance when it returns an allocated instance.

**Super class**

At the meta-level, `super` is a reference within the `MetaClass`, enabling class-level inheritance through a model where the metaclass hierarchy runs parallel to the class hierarchy. Because all interactions are message-driven, `super` invoked at the class level is resolved by the Tolk Engine within the metaclass hierarchy of the parent class.

**Object instantiation**

Object instantiation operates as a uniform message dispatch via the `MetaClass` hierarchy following the `Receiver #message` pattern. The metaclass architecture treats every class as an instance of the singular `MetaClass` intrinsic, exposing the root allocation method `#new`. Decoupling memory allocation (`MetaClass #new`) from object initialisation enables creation mediated through the metaobject protocol, while compiler semantic checks guarantee structural safety during construction.

Classes function intrinsically as class factories. Developers can define custom factory selectors on the `MetaClass` to orchestrate input validation, raw allocation (`super #new`), and state binding within an atomic sequence. These custom creation methods compose directly with covariant self-return and implicit self-receiver mechanics.

Defining an explicit creation method automatically restricts the visibility of the intrinsic `#new` protocol to `private`. This prevents uninitialised object creation.

	class Person {
	
	    String name;
	
	    // creation method    
	    meta new(String name) {
			^ #new #name(name)
		}

		// ...
	}

Treating object instantiation as a formal protocol ensures that type-specific behaviours—such as cached hashing in records or constant identity optimisation in enumerations—are applied uniformly across all types.

**Collection creation**

Literal collection creation in Jolk uses the `#` anchor as a shorthand for message-based instantiation. This notation enables concise collection initialization, such as:

	// literal anchor shortcut  
	Array[String] colors = #["red", "green", "blue"];

This serves as a minimalist alternative to the variadic `new` with varargs:

	// variadic meta creation method 
	@Intrinsic
	meta new(T... elements) { }

	// variadic creation   
	Array[String] colors = Array #new("red", "green", "blue");

By implementing these as literal anchors, Jolk remains "bracket-light" while upholding the Unified Messaging principle. Since these literals are reified as underlying messages, the resulting collection can immediately participate in message chains	.

**Constants**

Class-level constants are meta objects within the `MetaClass`, defined by `meta constant` distinguished by semantic casing (e.g., PascalCase or UPPERCASE) at the lexer level. This convention acts as a delimiter—or "fence"-, allowing the Tolk Engine to resolve constants like `Math #PI` within the metaclass layer. Thus, constants are not just static data—they are first-class participants in the metaobject protocol, enabling full integration with Jolk’s dynamic messaging architecture.

	class Math {
	
	    // symbolic directive for
		// public meta constant Double PI = 3.141592653589793;
	    #< meta constant Double PI = 3.141592653589793;
	
	    // ...  
	}

**Meta Projection (&)**

The `&` operator (or `using meta` directive) introduces a meta field projection, which acts as a "lens" to create a local reference that maps an identifier (e.g., `PI`) to a constant within a remote identity (e.g., `Math`). This maintains semantic integrity by preserving the link to the parent object while eliminating the need for explicit message selectors (e.g., `Math #PI`).

While the projected identifier appears as a bare variable in the local scope, the Tolk Engine recognizes the underlying lens and applies semantic flattening, resolving the reference at compile time (often via inlining). This mechanism enables more fluid and concise syntax when accessing meta-level constants or methods.

For example, instead of repeatedly writing `Math #PI`, `PI` is projected into the local scope:

	// Symbolic directive for projecting a meta constant
	// using meta java.lang.Math.PI;
	& java.lang.Math.PI;

	// Standard message send
	x = 2 * r * Math #PI;

	// Lens approach (after projection)
	x = 2 * r * PI;

**Assignment**

The assignment symbol (`=`) demarcates the boundary between internal evaluation and identity definition. While the language syntactically allows field assignment, semantically, the meta-level restricts assignment to local variables and field initialization at declaration. By mandating that all other state changes must be mediated through messages, Jolk enforces local retention and encapsulation, ensuring that an identity’s internal state remains isolated.

## Messaging

Jolk achieves syntactic uniformity through a pure message-oriented model in which operators, control flow, and error handling are implemented as library-level protocols. Mathematical and logical symbols such as `+` and `==` are defined as unified message selectors — ordinary messages that any type can implement — so custom types participate in expressions with the same syntax as built-in types.

Messaging is further refined by permitting the omission of the explicit `self`receiver and, in command methods, of the `Self` return type; see [*Return & Implicit Self-Return*](#return--implicit-self-return). When a message selector appears without an explicit receiver, the compiler binds the invocation to the implicit self-receiver. This allows compact internal message chains such as `#x(#x + 1)` while preserving the message boundary. Local variable bindings shadow implicit self dispatches, keeping scope resolution unambiguous.

This choice produces a compact lexical footprint: traditional keywords are replaced by polymorphic dispatch. Instead of `if`, `else`, and `while`, the selectors `?`, `:`, and `#while` are sent directly to Booleans or Closures. Error handling likewise dispenses with `try-catch-finally` in favour of `#try`, `#catch`, and `#finally` messages sent to closure objects.

Transforming fundamental operations into extensible library features opens the language up: following Kay's vision that computation is a flow of messages between autonomous objects [1], Jolk shifts the focus from objects to the messages between them. Because even basic logic and arithmetic resolve as message sends, the core design stays fixed while the engine implementation expands. New protocols are added as ordinary messages, without changes to the core language — the property Kay calls a "growable" system [1].

### Creation method

A creation method is a *Class-Level Messaging* interface: a meta-method declared on the type itself. It performs an atomic, linear transition from the type definition to a new identity — the lifecycle of an identity is therefore defined in a single, visible sequence. This removes hidden execution order: the state of an object is predictable and easy to trace. (See the `Person #new` example under [*Type categories*](#type-categories).)

For class-level state, Jolk favours expression-based initialisation. Complex setup is handled by direct assignment or self-contained closures, so that static data is established as a discrete, side-effect-free computation.

The creation method is the only block in which fields may be initialised. Because authority over a variable is never fragmented, the compiler can guarantee definite assignment: once the creation method returns, the object's state is fully established.

### Operators

**Unary operators**

Unary operators such as `!` (logical NOT) and `-` (arithmetic negation) are integrated into the unified messaging protocol as overloaded selectors. The syntax is conventional, but it functions as a deterministic layer within the expression hierarchy that maintains Jolk's message-passing integrity.

Unary operators bind more tightly than arithmetic terms (addition) and factors (multiplication), and less tightly than primary messages. The grammar supports right-associativity, allowing nested expressions such as `!!true`. Although `!!` is logically redundant for booleans, the recursive structure keeps every operator a discrete message send. Jolk distinguishes these unary operators from unary selectors: the latter are argument-less messages such as `user #name`, used for property access.

**Mathematical and logical rules for operator order**

Jolk adopts standard mathematical and logical precedence rules to comply with algebraic convention while preserving a pure object-oriented model. This aligns with the concept of unified messaging: Jolk defines operators as selectors, so every mathematical or logical operation is a message send, regardless of precedence. An expression such as `a + b * c` therefore follows standard precedence, yet resolves to message sends: b receives the `*` message, and a then receives the `+` message with the result as its argument. Embedding these rules in a deterministic grammar keeps the keyword-lean syntax without changing the semantics: operators remain message sends.

**Numerical operation evaluation**

Jolk governs numeric transitions through *Guided Coercion*: the mechanism preserves semantic integrity while guaranteeing execution performance. The numerical type ranking is `Int` \< `Long` \< `Float` \< `Double` \< `Decimal`; ascent is automatic, descent is explicit, and operations between types of the same rank remain at that rank.

Passive Coercion (widening): Jolk performs lossless promotions when mixing types, such as adding an `Int` to a `Double`. The narrower type is lifted to the wider one, preserving precision without explicit casts.

    // Passive Coercion in action  
    Int count = 10;  
    Double factor = 2.5;

    // The Int is automatically promoted to Double to match 'factor'  
    // The result is a Double (25.0), ensuring precision is maintained.  
    Double result = count * factor;

    // Subsequent operations continue in the wider space  
    Double finalValue = result + 0.75;

    // Result: 25.75 

Active Coercion (narrowing): a high-precision value cannot be assigned to a lower-precision type without an explicit conversion. When a transition is lossy, the compiler rejects it unless an *Explicit Coercion* — a conversion message such as `#asInteger` — confirms that the data loss is intentional.

    // Active Coercion in action  
    Double price = 19.99;

    // Attempting to assign a Double to an Int variable:  
    // COMPILATION ERROR: Narrowing conversion requires explicit guidance.  
    Int rounded = price;

    // The Explicit Coercion:  
    Int finalPrice = price #asInteger; 

    // Result: 19 (The .99 is truncated)

**Null-Coalescing operator**

The Null-Coalescing Operator (`??`) is a specialized binary operator providing a deterministic fallback: when the left-hand operand evaluates to null, it yields the right-hand operand. It removes the repetition of manual identity checks and allows safe defaults to be chained concisely.

	// Traditional ternary vs. Null-coalescing
	result = data != null ? data : default;
	result = data ?? default;

**Operator overloading**

Operator overloading treats symbols as unified message selectors: including operators within the selector grammar makes every operation semantically a message send. Custom types such as `ComplexNumber` can therefore respond to standard symbols such as `+`, `-`, or `*`, with the same syntax as built-in primitives. Jolk retains standard mathematical precedence.

### Keyword selectors

Jolk's unified messaging protocol distinguishes between data arguments in parentheses and logic arguments in blocks. Each selector carries a mandatory hashtag prefix and acts as a lexical fence, allowing the Tolk Engine to distinguish behaviour from data.

Property access, such as `#name`, requires no arguments and therefore omits parentheses. Data messages, such as list `#add(item)`, require parentheses to carry data arguments. Logic-driven messages, such as `list #forEach { i -> ... }`, use a bracket-light syntax in which parentheses are omitted for the closure.

Jolk enforces a separation between data and logic arguments: a message may accept parenthesised arguments or a closure block, but not both. Passing a closure as a standard argument is syntactically valid, but Jolk encourages more concise forms: a lambda — a parenthesised parameter list followed by -> and a single expression — a [method reference](#method-reference), or splitting the message across selectors.

	// closure
	list #reduce(0, { sum, each -> ... })

	// lambda
	list #reduce(0, (sum, each) -> sum + each)

	// method reference
	list #reduce(0, ##accumulator)

	// split across selectors
	list #inject(0) #into { sum, each -> sum + each }

### Control flow 

Control flow is executed by sending selectors to Boolean, Closure, and other identities, so that branching, looping, and error handling follow the same unified messaging rules as every other interaction. The `?` and `:` delimiters resolve semantically to messages sent to Booleans; looping is expressed through messages such as `#while` sent to a Closure or `#times` sent to an Integer; error handling unifies with messaging through `#catch` and `#finally` messages sent to closures. Because messages are the only means of interaction with an object — the lexical fence admits no bypass of the object's protocol — control flow is subject to the identical rules of encapsulation that govern user-defined code, 

**if-elseIf-else Chaining**

Branching is formalised as a recursive message expression sent to Booleans. Under the grammar rule `expression = logic_or [ ("?" | "?!") expression [ ":" expression ] ]`, the `?` ("if true") and : ("else") symbols act as structural selectors that guide message flow. Because the `:` selector can be followed by a nested expression (starting a new ternary check) or by a terminal closure, the parser rejects an invalid chain during parsing. Branching is therefore keyword-less control flow: a sequence of message sends to Boolean identities.

    // Multi-branch logic using ? and :

    (score >= 90) ? { Grade #A }   
        : (score >= 80) ? { Grade #B }  
        : { Grade #F };

Although the grammar admits recursive branching with both the positive (`?`) and negative (`?!`) operators, chains that use `?` consistently are easier to read. In a `?!` chain the `:` branch executes when the preceding condition holds, which inverts the reading established by `?` chains.

**Control loops** 

Control loops are polymorphic dispatch messages sent to objects. Looping is a protocol between receivers rather than a procedural keyword: a fixed count is expressed by an Integer receiving the `#times` message `(10 #times { ... })`, conditional iteration uses `#repeatUntil`, and `#forEach` is a polymorphic message send to a collection object.

    // A while  loop  
    counter = 0;  
    { counter < 5 } #while { counter = counter + 1 }

**Pattern matching and safe casting** 

Pattern matching emerges from the composition of safe-cast and logical messages. It uses monadic chaining — a pipeline in which each message receives the result of the previous one — to pass an identity through a sequence of steps until it is narrowed to a specific type. A match is not a branch: the identity passes through a sequence of messages that either accept it or ignore it.

Safe casting uses the `#as(Type)` and `#instanceOf(Type)` messages. `#instanceOf` performs type narrowing at runtime: instead of returning a raw pointer or throwing a cast exception, it returns a `Match[T]` container holding the value, or `Nothing` if the identity does not conform to the proposed protocol.

    other #instanceOf(Person) #ifPresent { p -> ... }

Pattern matching builds on these safe-cast messages. `Match[T]` enables monadic chaining through `#filter`, `#map`, and `#ifPresent`, turning imperative branching into a declarative data flow.

    ^ x #as(String)                            // Returns Match[String] with the value or Nothing
        #filter { s -> !(s #isEmpty) }         // If false the content of Selection is dropped  
        #map { s -> System #out #println(s) }  // It was a non-empty String so it gets printed

### Return & Implicit self-return

Jolk combines explicit returns with an implicit self-return rule. The caret symbol (`^`) marks an explicit return, providing type safety and permitting early exit. For self-returning methods, the covariant return contract is a structural guarantee: because `Self` is a context-aware reference to the current type definition, the mapping ensures that methods are subclass-safe. The semantic casing rule aligns the two: the type identity (`Self`) governs the return of the instance (`self`).

Closures and logic blocks use implicit expression evaluation: the last expression in a block is automatically returned as its result, so control-flow structures yield values. Block returns also support non-local returns, in which a closure terminates its defining method immediately. Because all results are message-send results, the return is a continuation of the message chain.

The method boundary sets the lexical limit for this behaviour. The `Self` return type is optional: when a method omits a return type, or explicitly specifies `Self` (or the name of the defining class), the Tolk Engine classifies it as a command and enforces an implicit return of self, sustaining a fluent API. Methods with other explicit return types require the return operator (`^`); if execution reaches the end of such a method without a return, the semantic analyzer rejects the code at compile time with a type mismatch error.

The method boundary also anchors non-local returns. A caret (`^`) used inside a nested closure targets the enclosing method, allowing the closure to terminate the defining method. This applies only within Jolk code: when a closure crosses the guest-host boundary and is used as an opaque Java functional interface (such as a `java.util.function.Predicate`), non-local returns are prohibited.

These rules—explicit carets, implicit self-returns, the `Self` alias, and block-level evaluation—give Jolk a keyword-lean flow that remains semantically clear within its unified messaging protocol.

### Method reference

A method reference reifies a message dispatch on a target identity into a first-class Closure identity. The double hashtag (`##`) extends the selector anchor so that a message selector can be passed as a value without writing a closure block.

    // Block syntax
    names #map {n -> n #toUpperCase}

    // Method Reference syntax
    names #map(String ##toUpperCase)

A reference is either instance-bound (`self ##method`, `instance ##method`) or meta-level (`Type ##method`). In a meta-level reference such as `String ##toUpperCase`, the receiver of each message is supplied by the argument of the enclosing message — for example, each element of names. The Tolk Engine projects these constructs to `MethodHandle` or `LambdaMetafactory` call sites.

### Exceptions

Jolk uses the standard Java exception hierarchy without checked exceptions: exceptions are never required to be caught and propagate through the call hierarchy. Exception handling is implemented through the unified messaging protocol: error handling is a library feature whose logic is executed by sending messages to closures. Closure is a intrinsic kernel class that provides the `#try`, `#catch` and `#finally`  selectors.

**Exception Class**

	@Intrinsic
	extension ExceptionExtension on java.lang.Throwable {
		
	    throw() { }  
	}

**Throwing Errors (\#throw)**

Since there is no throw keyword, exceptions are thrown by sending the `#throw` message to an object or a class.
Exceptions are raised by sending the `#throw` message to an exception identity.

    // classic dual call  
    Exception exception = Exception #new;
    exception #throw("Description");

    // shortcut implementation   
    Exception #throw("Description");

**Protected Evaluation (\#catch)**

To handle a potential exception, the code that may raise it is wrapped in a closure and sent the `#catch` message. Its argument is a closure whose type-hinted parameter specifies which exception to catch.

    { file #open(fileName) }  
        #catch { IOException e -> System #out #println ("File error") }  
        #catch { FileNotFoundException e -> System #out #println("File not found : " + fileName) }

**Cleanup Logic (\#finally)**

Code that must execute regardless of whether an error occurred is expressed with the `#finally` message, sent to the closure chain.

    { file #open(fileName) }  
        #catch { IOException e -> ... }  
        #finally { file #close }

## Metaobject protocol

The metaobject protocol, declared on the `@Intrinsic` `MetaClass` and defined in the Jolk kernel (`jolk.lang`), establishes the rules of communication between identities and their meta-level representations. By treating classes as first-class identities, Jolk replaces traditional reflection with a recursive protocol: a `MetaClass` is itself a first-class identity, so a class can receive any message, including those of the metaobject protocol.

Messages such as `#new` are sent to a class identity. Because the class operates as an instance of `MetaClass`, it executes its own allocation logic through ordinary message dispatch — there is no separate dispatch path for class-level messages. Every object therefore exists as a concrete instance in the runtime.

	@Intrinsic
	class MetaClass[T extends Object] { 

		/// The metaobject protocol

		/// Transforms a nominal path into a Meta-Class identity.
		meta MetaClass class(String path) { }
		/// Reifies a name into a Selector.
		meta Selector #message(String name);

		/// Identifies the available handshake surface for instances.
		List[Selector] instanceProtocol() { }
		/// Identifies factory and configuration messages for the Class.
		List[Selector] metaProtocol() { }
		
		/// Handshake for Materialisation and Orchestration
		T project(Selector s, Object... args) { }
		/// Retrieves the factual state for serialisation/mapping.
		Map stateProjection() { }
	}

Protocol projection connects the string in the source code to the selector in the engine. It is deterministic and bound by the lexical fence: the `#project` message cannot access internal state; state is exposed only through protocol extensions.

A mature metaobject protocol enables non-intrusive tooling — IDE extensions, static analysers and automated test frameworks — and boilerplate-free domain engineering. A generative metaobject protocol must respect Jolk's architectural constraints:

* **Meta-level boundary**: dynamic meta-structures cannot violate state privacy or bypass visibility boundaries (`private`, `protected`, `package`).
* **Type lattice conformance**: Strongtalk-inspired static subtyping and protocol bounds are preserved, with compile-time verification intact.
* **Non-intrusive tooling**: tool builders get a deterministic, predictable foundation, without opaque runtime mechanisms.

## Core type system

The core type system governs the identity and behaviour of every entity through intrinsic types and extensions, and provides the unified messaging protocol for both Jolk and host identities. The system treats foundational types such as `MetaClass`, `Boolean`, and `Nothing` as first-class identities, so that basic data structures meet the same safety guarantees as any other type.

### Intrinsic types

An Intrinsic Type is a foundational kernel object — such as `Long`, `Boolean` or `Closure` — marked with the `@Intrinsic` annotation. Intrinsic types appear in Jolk source as first-class identities with a discoverable messaging protocol; at runtime, they compile to unboxed JVM values. The Tolk Engine uses these identities to map message sends onto the underlying JVM representation, and the class representations remain the anchor point for tooling such as IDEs and documentation. `Boolean` and the numerical types are non-nullable.

**First-class Identities** 

`true`, `false`, and `null` are atomic first-class identities.

*Boolean \- true | false*

Jolk reifies `true` and `false` as singletons that encapsulate branching behaviour. Branching is a message send: a message such as `?` is sent to an expression, and the `Boolean` receiver decides which branch of the expression executes, so Booleans participate in message chaining.

*Nothing \- null*

The `Nothing` class represents the absence of a value, identified by the reserved literal `null`. Implemented as a singleton at the bottom of the type lattice, `null` is a first-class object that implements the structural Null Object Pattern [13]. Because `null` conforms to the universal receiver contract, message dispatches targeting it execute methods defined on `Nothing` rather than raising a null-pointer error.

Message dispatch to `null` relies on identity-level polymorphism. When `Nothing` receives a message, its implementation returns `null`, so chained dispatches (`object #first #second`) evaluate without conditional branching or wrapper types. This short-circuiting behaviour preserves message evaluation across missing values and produces a deterministic null result at the end of the expression.

*MessageNotUnderstood*

`MessageNotUnderstood` is reified as a first-class atomic identity: a failed dispatch becomes a Jolk-level event that can be recovered through the `#catch` protocol, yielding a stable, queryable identity instead of a runtime fault.

Within statically verified Jolk code, `MessageNotUnderstood` is unreachable: the compiler validates every selector against the receiver's declared protocol, so a send admitted by the type lattice cannot fail at dispatch time. The identity is therefore a boundary protocol, covering dispatches whose receiver protocol is known only at runtime. Three such situations exist:
* A message send to a coalesced host identity resolves at runtime against the loaded class; if that class differs from the one observed at compile time, the dispatch fails dynamically, and `MessageNotUnderstood` reifies the failure as a first-class event recoverable through `#catch`, rather than surfacing as a raw linkage error.
* The metaobject protocol admits dispatches constructed from reified selectors — `#project(Selector, Object...)` and `#message(String)` build sends from data, which static verification cannot check.
* Identities synthesised through the metaobject protocol, such as mocks and serialisers, enter the lattice after compile time.

`null` receivers are excluded: Nothing answers every message and returns `null`, so the scope of `MessageNotUnderstood` is precisely a non-null identity whose runtime protocol lacks the selector.

**Primitive Identities**

The Numeric identities wrap the JVM scalar types as `@Intrinsic` kernel classes; they are non-nullable and immutable, and every operation message returns a new identity. The `String` identity reifies `java.lang.String` as an immutable kernel class for literal data; it is an intrinsic identity but not a scalar. `Long` is the principal example:

	@Intrinsic  
	class Long {
	
	    +(Long other) { }  
	    -(Long other) { }  
	    *(Long other) { }  
	    /(Long other) { }
	
	    Boolean >(Long other) { }  
	    Boolean <(Long other) { }
	
	    repeat(Closure action) { }
	
	}

    // Usage  
    5 #repeat { System #log("This is a human-readable loop.") };

**Closure**

In Jolk, a closure is a reified identity: a first-class block of deferred logic that maintains a link to its defining environment. Its interaction with the surrounding scope is governed by the Closure contract, which depends on the selector that receives it. The contract determines whether the closure's boundary is transparent or opaque, and therefore whether the closure may use the return terminal (`^`) to exit the enclosing method.

* **Intrinsic selectors:** When a closure is passed to a core selector (such as `#while` or `?`), the compiler flattens the interaction into native JVM constructs. The boundary is fully transparent: the closure operates directly on the caller's stack, can mutate local variables without overhead, and can use `^` for a non-local return, exiting the parent method immediately.
* **Transparent selectors:** For library methods marked with `@Inline` (such as `#withLock`), the compiler inlines the method body at the call site, treating the closure as a structural extension of the method. The boundary is transparent, with the same capabilities as intrinsic selectors. This allows developers to create custom control structures.
* **Opaque selectors:** When a closure is passed to a standard functional selector (such as `#map`), it operates as an encapsulated unit of logic, projected as a Java lambda. It can capture and mutate state from its defining environment, but it may not use `^` to exit the parent method; the compiler rejects such returns to prevent invalid stack manipulation.

Syntactically, a closure is brace-delimited: `{ params -> statements }`. Parameters are separated from the body by an arrow `->`, which is omitted when the closure takes no parameters. A closure is typically not embedded in a parenthesised argument list. Instead, the language encourages selector refining: the closure is the sole payload of a dedicated message (e.g., `#with(param) #do { ... }`), so each message receives either data or logic.

	@Intrinsic  
	class Closure[T] {
	
	    // Catch specific types by passing the Meta-ID and a handler  
	    catch(Type errorType, Closure handler) { }
	
	    // Finally always runs and returns the result or self  
	    finally(Closure finalAction) { }  
	}

To enable custom control structures, the `Closure` type provides `@Intrinsic` selectors that pull logic into the caller's scope:

*   `#call`: Executes the closure, acting as the "hole" for user logic.
*	`#try`: Handles the opening of a resource and passes it to the subsequent closure.
*   `#catch(error, handler)`: Intercepts specific error identities.
*   `#finally(cleanup)`: Guarantees execution during stack unwinding.

This architecture allows developers to define templates like `#withLock` that behave like native language keywords.

	@Inline
	T #withLock(Closure[T] logic) {
		#lock;
		{ ^ logic #call } #finally { #unlock }
	}

In Jolk, the distinction between intrinsic, transparent, and opaque selectors defines the boundary between structural control and functional data. Transparent selectors allow developers to extend the language with custom control structures.

**MetaClass**

Jolk implements the ObjVlisp metaclass model [14]: the `MetaClass` is an intrinsic, and every class is an instance of its own metaclass. Each metaclass is in turn an instance of the single `MetaClass` intrinsic, so the metaclass layer has one type but many objects: metaclass instances form an inheritance hierarchy that mirrors the class hierarchy. `MetaClass` is an instance of itself, so the metaclass hierarchy terminates.

This makes types first-class Meta-Objects: the type protocol (`#new`, `#name`) is defined on the metaclass, and a class-side invocation is a message send to the metaclass. Class-side methods therefore dispatch polymorphically through the metaclass hierarchy — virtual static dispatch — while method lookup traverses the class hierarchy.

Object instantiation uses the `#new` message defined on `MetaClass` (see [*Meta-level - Object instantiation*](#meta-level)). The default `#new` is an intrinsic: the compiler maps it to standard allocation. A class specialises instantiation by overriding `#new` with a meta method, so allocation logic participates in the unified messaging protocol.

	@Intrinsic
	class MetaClass[T extends Object] {

		/// The metaObject protocol
		...
	
	    /// The default creation method  
	    T new() {  
	        // java pseudocode  
	        // return new T();  
	    }
	
	    /// Returns the name this Type  
	    String name() {  
	        // java pseudocode  
	        // return T.getName();  
	    }
	
	    /// Returns the Type Identity that this one extends  
	    [S extends MetaClass[T]] S superclass() {  
	        // java pseudocode  
	        // return T.getSuperclass();  
	    }  
	}

**Object**

The *Universal Root Identity* constitutes the common denominator for all non-null entities. The Intrinsic Object protocol defines how any identity—Native or Coalesced—participates in the messaging exchange.

The *Universal Root Identity* is the root of the class lattice: the protocol every non-null identity inherits, and the point at which the metaclass hierarchy meets the class hierarchy. It is realised as an extension on `java.lang.Object`, so identities native to Jolk and identities imported from Java and enhanced through Type Coalescence (see the next section) participate in the unified messaging protocol on equal terms. `Nothing`, the reified absence referred to by `null`, is deliberately outside this lattice: absence is not an object, and the Object protocol does not apply to it.

The intrinsic `Object` protocol defines the messages every receiver therefore understands — equivalence, presence, type reference, and safe casting:

	// symbolic directive for
	// package jolk.lang
	~ jolk.lang;
	
	// Root identity for the Unified Messaging Model
	
	@Intrinsic  
	extension ObjectExtension on java.lang.Object {
	
	    // structural equivalence  
	    Boolean ~~(Object other) { /* this.equals(other) */ }
	
	    // Flow control presence & emptiness  
	    ifPresent(Closure action) { action #apply(self) }  
	    ifEmpty(Closure action) { }
	
		// Safe Casting: Narrow the identity into a specific protocol.
		Match[T] instanceOf(Type[T] type) { }
		Match[T] as(Type[T] type) { }

		// Predicate: Verify if the identity adheres to a specific protocol.
		Boolean isInstance(Type[T] type) { }
	
	    // Context-aware type reference  
	    Metaclass[Self] getClass() { /* this.getClass() */ }
	
	}

### Type Coalescence

*Type Coalescence* is the strategy that governs Extensions. A Coalesced Extension—an extension projected onto a native Java type, such as `Array` onto `java.util.List`—must remain a valid Java identity for ecosystem interoperability, so it cannot be erased. The Tolk Engine adopts native Java structures such as `java.lang.Class`, `java.lang.Object` and `java.lang.Exception`, and enhances them with Jolk-specific semantics: it injects high-level selectors, enforces null-safe intrinsic mappings for core operations such as equality, and shadows legacy Java methods that contradict Jolk's safety model. This native synthesis ensures that every object in the ecosystem, whether defined in Jolk or imported from a Java library, conforms to a consistent interface.

Jolk achieves shim-less integration by adopting native Java types for its most critical structures: `java.util.List` is augmented with the `Array` protocol, and `java.lang.Throwable` with the `Exception` protocol. The Tolk Engine maps Jolk messages to the corresponding Java methods: calling `#name` on a Java bean resolves to a `name()` invocation on the host object. Because Jolk objects are themselves Java objects, they can be passed to existing Java APIs without conversion wrappers, preserving compatibility with Java collections and libraries while maintaining the unified messaging protocol.

**Collection Types**

Jolk provides three fundamental collection protocols, each anchored by a lexical shorthand derived from its mathematical notation. Because Jolk adopts the Java Collections Framework natively, these protocols are projected onto host types through implicit extensions.

The `Array` is an ordered sequence, the primary structure for sequential logic. Its literal form, `#[ ]`, is anchored by the square bracket, the standard notation for the matrix and the vector, which serves a single purpose: the variadic creation of an ordered sequence. Every element is indexed by position. It responds to positional messages (`#at`) and the stream-based protocol (`#map`).

	@Intrinsic
	extension ArrayExtension[T] on java.util.List[T] {
	
	    meta Array[T] new(T... elements) { }

	    T at(Int index) { }

	    Array[T] put(Int index, T element) { }

	    [R] Array[R] map(Closure[R] mapper) { }
	}

By standardising on these types and their supporting iterators, Tolk applies protocol standardisation. This maintains a single, dense model through a 1:1 mapping between mathematical concept and syntactic form.

The `Set` is a collection of unique identities: it removes duplicates and has no defined order. Its literal, `#{ }`, uses the brace, the canonical symbol of set theory. Membership is defined by identity, not position. It responds to membership queries (`#includes`) and mathematical unions.

The `Map` is an associative type that reifies the relationship between a domain and a codomain. Its literal, `#( )`, distinguishes an association from the containment of a set or the body of a block. The entry operator (`->`) links keys to values. It responds to key-based retrieval (`#at`) and domain inspection.

The `Iterator` is the traversal protocol, specified as a set of messages. To support the integration of Java's iteration patterns into Jolk's message flow, the kernel provides an extension for the native Java Iterator.

	@Intrinsic
	extension IteratorExtension[T] on java.util.Iterator[T] {
		
		/// Yields the next identity in the sequence.
		T next() { }

		/// Signals if the sequence contains further identities.
		Boolean hasNext() { }

		/// Consumes the remaining flow via a closure.
		Self forEach(Closure[T] action) { 
			{ self #hasNext } #while { action #apply(self #next) }
		}
	}

By standardising on these types and their iterators, Tolk maintains a single model with a one-to-one mapping between mathematical concept and syntactic form.

## Architectural highlights

*Deferred initialisation:* The on-demand (`lazy`) creation of the system's configuration through traceable, manual dependency injection.

*Extension protocols:* Extension protocols attach new behavioural contracts to existing types without modifying their source.

*Guided coercion:* A mechanism governing numeric transitions between the intrinsic numeric types: widening promotions are applied automatically; narrowing requires an explicit message such as `#asInteger`.

*Identity restitution:* A boundary protocol that lifts raw JVM `null` pointers into the `Nothing` singleton and lowers them back for Java interoperability.

*Implicit self-return*: The mechanism where a method automatically returns `self` if no explicit return expression (`^`) is evaluated before reaching the end of the method body.

*Local retention*: An encapsulation principle that restricts the assignment operator to local identifiers within an object's internal context.

*Message boundary*: The enforcement of receiver-centric interaction, prioritising communication protocols over internal properties to ensure state isolation.

*Modifiers:* The model distinguishes structural constraints (`final`) from value stability (`constant`) and instance-level stability (`stable`). Types and methods default to `public` and fields to `private`; meta constants permit direct access through import lenses.

*Non-local returns*: The ability of a closure to command its defining method to finish immediately, even across different stack frames.

*Null object pattern:* The reification of absence as an atomic identity (`Nothing`), so that uninitialised states can safely receive messages.

*Object instantiation*: Decoupling object creation via intrinsic #new from object initialisation achieves flexible construction through the metaobject protocol. Compiler semantic checks guarantee structural safety throughout the instantiation lifecycle.

*Predicate assertion:* A message form (`?` / `?!`) that combines a state query with a logical expectation, so the compiler branches on the message itself without a negation operator.

*Reified closures:* Closures are identities that maintain a structural link to their lexical environment; their boundary is projected in two ways:
- **Inline projection:** for control-flow structures, the closure boundary is erased, enabling in-place execution where the logic is merged directly into the calling method.
- **Unbounded projection:** when passed as a parameter or crossing method boundaries, the closure is projected as an opaque functional interface, supporting Java interoperability while maintaining identity congruence.

*Self type:* First-class `Self` type integration within the receiver type lattice preserves concrete return types across polymorphic message sends without recursive generic bounds.

*Semantic casing*: A lexical rule where the first character's casing determines an identifier's role: Uppercase for Meta-Objects (Types) and lowercase for instances (Variables).

*Strong typing*: A static protocol leveraging the Java type system that verifies behavioural protocols via conjunctions (`&`), ensuring that objects are matched based on both syntactic signature and explicit semantic identity.

*Syntactic fluidity:* A bracket-light syntax that eliminates redundant delimiters to keep expressions dense.

*Unified message-passing:* Every interaction—from arithmetic and object instantiation to control flow—is a message between a receiver and a selector.

*Receiver retention:* The Tolk Engine intercepts host Java `void` method invocations and restores the receiver (`self`) as the evaluation result, preventing procedural host calls from breaking Jolk message chains.

## Heritage & foundation

Jolk is a convergent architecture where the static safety of Java acts as the gatekeeper for a Smalltalk-inspired runtime. The Java influence provides the structure—nominal typing, curly-brace scoping, and visibility modifiers—utilising factory patterns as a core construct to govern object lifecycles. The Smalltalk influence provides the execution via the messaging kernel. Beyond these primary anchors, Jolk’s design is further inspired by the pragmatic ergonomics of Kotlin, the symbolic density of C#, and the pioneering meta-object research of Self and Lisp.

*Smalltalk-80*: Jolk adopts the core philosophy that "everything is an object" and computation is a "dynamic exchange of messages"[2]. It utilizes keyword selectors (using a `#` hashtag anchor) and closures (`{ }`) as first-class identities to manage control flow. Methods implicitly return `self` upon completion unless an explicit return (`^`) is specified. Non-local returns allow a closure to command its defining method to terminate immediately. Unlike Smalltalk, which operates in a closed image, Jolk must respect the JVM stack. Non-local returns are permitted within the guest environment but are forbidden when a closure is projected as an opaque Java Functional Interface.

*Strongtalk*: Jolk applies principles from Strongtalk to achieve structural type constraints within the host environment[4]. The runtime explicitly separates behavioral protocols from the class hierarchy through the use of `protocol`, a concept known as *lattice separation*. Jolk employs Strongtalk-inspired type-checking to statically validate messages, ensuring a receiver can understand a message before execution. Jolk leverages Strongtalk to focus on behavioral integrity. This is reinforced by the ampersand (`&`) operator, which reifies *conjunction types* for behavioral composition, and the `Self` type alias, which ensures that methods returning `Self` remain type-safe across the inheritance tree.

*The Self language*: The Tolk Engine’s strategy of semantic flattening is the spiritual successor to the optimization techniques developed for the Self language[10].

*Java and JVM*: The syntax for structural scaffolding—including package, import, and class—is intentionally aligned with Java. Jolk integrates with the Java Collections Framework and supports both annotations and Java Generics. 

*Kotlin*: Jolk synthesizes Kotlin's pragmatic ergonomics with Smalltalk's foundational principles. Features like null safety, exceptions, and predicates are re-imagined through a dual heritage. Jolk's `Nothing` identity, rooted in Smalltalk's reified absence, aligns with Kotlin's emphasis on preventing `NullPointerException`s. Similarly, the elimination of checked exceptions mirrors Kotlin's approach, while Jolk's message-passing for control flow (using closures as predicates) draws from both Smalltalk's blocks and Kotlin's functional patterns.

*Scala*: The *monadic chaining* of the `Match[T]` container is a direct evolution of the functional patterns popularized by Scala's `Option` and `Try` types. Jolk adopts the semantic rigor of monadic data-flow—chaining logic through containers—while utilizing the Tolk Engine to elide the associated allocation overhead.  While Jolk’s `lazy` mechanism resembles Scala's `lazy val` in its memoized behaviour, it functions as a primitive for resource management and memoization.

*C#*: The `??` operator for null-coalescing, providing a concise, expression-based mechanism for handling null values that aligns perfectly with Jolk's fluid messaging. The `using` directive for vocabulary expansion, aliasing and constant projection.

*Extension*: While the syntax borrows from the ergonomics of Dart, the semantics are more Lisp-centric.

*Encapsulation*: Jolk synthesises message passing with encapsulation. The symbolic notation derives from the visibility and variability (finality) facet of the ProtoTyping research study on typed object-oriented languages[3].

*Guided coercion* is based on the generality principles of the past (Java & Smalltalk-80) but adds a semantic layer of protection to ensure that data transitions are as mathematically sound as they are performant.

---

# Part Three

**System Synthesis and Patterns**

---

*-- under construction --*

The practical utility of Jolk’s architecture is demonstrated through the Form Validation Framework. While the language adheres to a minimal syntax, the Tolk Engine ensures that these high-level choreographies achieve ecosystem congruence with the Java Runtime Environment.

## Example

The form validation framework illustrates several language aspects: types, creation methods, unified messaging, mathematical & logical expressions, control flow & exceptions.

### Framework

The framework provides a set of abstract & final classes that are the basis for a form validation system.

**class \- Node, ChildValidation, Validation, Suite, Constraint, Interrupt**

Demonstrated language concepts: generics, Self type alias, message chaining for control and exception flow, null object pattern

	//
	package abstract class Node[T] {
		package abstract Self accept(T subject, ExecutionContext context);
	}

	//
	package final class ChildValidation[T, R] extends Node[T] {

		Function[T, R] supplier;  
		Validation[R] validation;

		meta ChildValidation new(Function[T, R] supplier, Validation[R] validation) {
			^ self #new
				#supplier(supplier)
				#validation(validation)
		}

		package Self accept(T subject, ExecutionContext context) {  
			supplier #value(subject) #ifPresent { child -> validation #accept(child, context) }  
		}  
	}

	package abstract class Validation[T] extends Node[T] {

		package final Self accept(T subject, ExecutionContext context) {  
			(self #satisfiesPreCondition(subject, context)) ? { self #doAccept(subject, context) }  
		}

		protected Boolean satisfiesPreCondition(T subject, ExecutionContext context) { ^ true }

		protected Interrupt interrupt() { ^ null }

		package abstract Self doAccept(T subject, ExecutionContext context);  
	}

	//
	abstract class Constraint[T] extends Validation[T] {

		package final Self doAccept(T subject, ExecutionContext context) {  
			self #isValid(subject) ? { ^ self };  
			context #add(subject, self #getIssue(subject, context));  
			self #interrupt #ifPresent { e -> e #throw }  
		}

		package abstract Boolean isValid(T subject);

		package abstract Issue getIssue(T subject,ExecutionContext context);  
	}

	//
	abstract class ValidationSuite[T] extends Validation[T] {

		constant Array[Node[T]] nodes = Array #new;

		final Self add(Constraint[T] constraint) {
        	nodes #add(constraint)
    	}

		final [R] Self add(ChildValidation[T, R] suite) {
        	nodes #add(suite)
    	}

		final Self validate(T subject, ExecutionContext executionContext) {
			{ self #accept(subject, executionContext) }
				#catch { Interrupt e -> /* ignore */ }
		}

		package final Self doAccept(T subject, ExecutionContext executionContext) {
			{ nodes #forEach {node -> node #accept(subject, executionContext)} }
				#catch {
					// the further validation of this ruleset is ignored on an interrupt
					Interrupt e ->  (e != self #interrupt) ? e #throw
					// no action required, the containing ruleset will resume the validation
				}
		}
	}

	//
	final class Interrupt extends Exception { ... }

	//  
	record Issue {  
		Object subject;  
		String message;  
		Level level;  
	}

	//  
	enum Level { ERROR; WARNING; INFO }

### Domain

The domain types are a set of data objects and validation classes implementing a specific form validation.

	//  
	class ContactForm {  
		Person person;  
		String description  
	}

	//  
	class Person {  
		Int ssn;  
		String firstName;  
		String lastName;

		Boolean ~~(Object other) {
			(self == other) ? { ^ true };
			other #instanceOf(Person) #ifPresent { p ->
				^ (#ssn == p #ssn)
					&& (#firstName ~~ p #firstName)
					&& (#lastName ~~ p #lastName)
			};
			^ false
		}

	}

### Validation

Demonstrated language concepts: creation methods, message chaining, closure, DI, meta projection, expression evaluation

	//  
	class ContactFormValidation extends ValidationSuite[ContactForm] {

		#< meta constant Interrupt INTERRUPT = Interrupt #new;

		// singleton in DI configuration  
		meta lazy ContactFormValidation new() {  
			^ super #new  
				#add(ZipConstraint #new)
				#add(InssConstraint #new, ContactForm ##person)
		}

		Interrupt interrupt() { ^ INTERRUPT }  
	}

	//
	& demo.validation.rules.ContactFormValidation.INTERRUPT;

	#! class InssConstraint extends Constraint[Person] {

		// singleton in DI configuration
		meta lazy InssConstraint new() {  
			^ super #new;  
		}

		Boolean satisfiesPreCondition(Person person, ExecutionContext context) {  
			^ person #ssn #isPresent
		}

		#: Boolean isValid(Person person) {  
			^ self #isValid(person #ssn)  
		}

		#> Boolean isValid(Long ssn) {  
			^ (inss / 97) != (ssn % 97)  
		}

		#: Issue getIssue(Person person,  ExecutionContext context) {  
			^ Issue #new(person, "INSS_INVALID", Level #ERROR)  
		}

		#: Interrupt interrupt() { ^ INTERRUPT }  
	}

## Fragments

### Closure 
Closures & The Selector Contract
In the Jolk architecture, the behavior of a closure is dictated by the selector that receives it. The compiler categorizes these interactions into three distinct contracts to determine the appropriate projection.

*Intrinsic Selectors* (Structural Primitives) These are hard-coded building blocks known to the compiler (e.g., `?`, `#catch`). They undergo Flattening, projecting directly to native Java constructs like if statements or try-catch blocks. They support non-local returns (`^`) and require no imports.

	user #isActive ? {
		Log #info("Access Granted");
		^ true // Non-local return: exits the parent method
	} #catch { Error e -> 
		log #error(e #message);
		^ false
	}

*Transparent Selectors* (Custom Control Templates) Library methods marked with `@Inline`. The transpiler performs Lexical Inlining, injecting the method body directly into the call site. This allows developers to create custom control structures (like #`withLock`) that support non-local returns, extending the language without modifying the compiler.

	@Inline
	T #withLock(Closure[T] logic) {
		self #lock;
		{ ^ logic #call } #finally { self #unlock }
	}

	// Usage
	#withLock {
		item #isExpired ? ^ false; // Exits the parent method via inlining
		item #process
	}

*Opaque Selectors* (Functional Messages) Standard methods (e.g., `#map`) where the closure is treated as a self-contained unit of work. The transpiler applies Boxing, converting the closure into a Java Lambda. To ensure thread safety and logical consistency, the Semantic Guard forbids non-local returns in this context.

	Thread #async { 
		Log #info("Running in background");
		^ false; // ERROR: Cannot escape a thread boundary.
	}

Placing a closure within a parenthetical argument list—such as #`do(param, { logic } )`—is a signal of procedural bias. This pattern forces a collision between static data and deferred logic. To maintain fluidity, Jolk adopts the Selector Refining Protocol: logic must never be a secondary passenger; it must be the primary payload of a Refining Selector. Instead of saturating a single message, the choreography is split into Contextualisation and Application.

	// Anti-Pattern: anonym parameter
	db #query(sql, { row -> ... } );

	// Allowed Pattern: identified parameter
	rowHandler = { row -> ... };
	db #query(sql, rowHandler);

	// Recommended Pattern: Selector Refining
	db #query(sql) #each { row -> ... };

## Design Patterns

### Fluent State-Chaining Technique

Jolk supports method chaining and domain-specific language (DSL) construction using a state-chaining technique. By returning target identities or updated receiver states from message sends, execution flows naturally through sequential dispatches without requiring specialised control-flow syntax or macro mechanisms. It resolves overloading anti-patterns—where a single selector accepts multiple disparate argument types—by reifying context shifts into transitional identities.

This technique allows an identity to delegate its messaging protocol to a contextual proxy. The code remains linear and declarative, eliminating nested closures or multi-argument calls. A State-Chaining Fluent API comprises four participants:

* *The Root Aggregate:* The primary entry point maintaining the parent protocol (e.g., `ValidationSuite`).
* *The Context Transition Selector:* A message dispatch initiating the context shift (e.g., `#subject`).
* *The Transitional Scope:* An intermediate interface representing the pending sub-operation (e.g., `ChildRequirement`).
* *The Context Bridge:* An internal implementation capturing context and returning execution focus to the Root Aggregate.

The defining characteristic of this state-chaining idiom is automatic scope reversion. Unlike standard fluent builders that retain sub-context state, terminal messages within a transitional scope explicitly return the parent aggregate identity.

	// Linear State-Chaining Fluent API
	ValidationSuite #new
		#add(PresenceConstraint #new)               // Parent aggregate state
		#subject { f -> f #email } #add(Email #new) // Transition to sub-context -> fulfilment -> scope reversion
		#add(NextConstraint #new)                   // Continuation on parent aggregate

This state-chaining design ensures single-responsibility boundaries for each message send:

* *Context Transition:* The `#subject` selector projects the target.
* *Context Fulfilment:* The `#add` message on the transitional scope completes the sub-operation.

By decoupling these concerns, the parent aggregate remains encapsulated. It avoids direct interaction with extraction logic, receiving instead the resulting identity node provided by the bridge upon completion. To implement this technique, the aggregate delegates to a context bridge encapsulating the parent reference and extraction logic.

	// Method implementation inside Sovereign[T]
	#! [R] Requirement[T, R] subject(Closure[T, R] supplier) {
		^ RequirementBridge #new(this, supplier)
	}

	// Method implementation inside RequirementBridge[T, R]
	#! [T] Sovereign[T] add(Constraint[R] node) {
		// Construct internal mapping and update the parent aggregate
		master #nodes #add(MappingNode #new(supplier, node))
		^ master // Terminal scope reversion to parent aggregate
	}

This state-chaining technique prevents protocol expansion. As system architectures scale, it absorbs structural complexity directly into the object graph avoiding proliferating overloaded method signatures.

## The Meta-level synthesis

The Tolk Engine establishes a unified messaging model where type-level interactions are as rigorously managed as instance-level communications. This architectural symmetry simplifies the orchestration of dependency injection, configuration, and state extraction into a consistent, negotiable handshake. For tool builders, this design provides a clear and predictable protocol for interacting with Jolk's meta-level:

*   **Self-Projection:** A MetaClass can act as its own recipient for the `#project` message. This allows tools to configure meta-level state or trigger factory orchestration directly on the type.
* **State Discovery and Extraction** (`#instanceProtocol`, `#metaProtocol`, `#stateProjection`): Tools can discover the message boundary of any Jolk MetaClass without resorting to reflection.
*   **Orchestration:** Dependency containers and configuration frameworks can leverage the `#project` message for both instance and meta-object configuration. 
*   **Verification:** Mocking frameworks and other verification tools can rely on Jolk's robust identity and equivalence protocols.

Crucially, Jolk's architecture explicitly prevents intrusive reflection. There are no guest-level primitives capable of bypassing the defined protocol to interrogate the object's structure. Every interaction, including object instantiation, is managed by the Tolk Engine.

### Dependency injection

Dependency Injection is established as JIT-DI (Just-In-Time Dependency Injection), a model that replaces reflection-heavy containers with Structural Synthesis. By treating component assembly as a fundamental application of native object-oriented principles, JIT-DI ensures that the dependency graph remains strictly traceable and only materialises when needed. This approach reduces DI to a transparent message-passing pattern where the developer retains full authority over the instantiation, wiring, and lifecycle of every component through explicit, statically traceable code.

At the core of this model lies the `lazy` feature, acting as a compiler intrinsic for Thunk Projection to provide thread-safe, memoised instances that are only hydrated upon the reception of their primary message. While independently conceived to support Jolk's messaging architecture, this mechanism provides a native solution for resource management without intrusive frameworks. Because Jolk enforces immutable constructor parameters, this contract provides a level of structural integrity that ensures every object is "Correct by Construction," physically preventing the bypass of the dependency contract once an object is created.

### The federated model and local injection

Jolk rejects centralised registries and global ApplicationContexts in favour of a Federated Model to prevent context pollution. To avoid "parameter push-down" in deep hierarchies, the system employs Local Injection via Polymorphic Meta-Parameters. Components receive abstract meta ConfigurationProviders as resource bundles and use a `meta constant` lens to retrieve specific dependencies. The Tolk Engine subsequently flattens these access paths, treating resource access as a static dispatch after initial resolution.

This native architecture elegantly replaces the "magic" of traditional DI features with language-level equivalents. Singleton scopes are inherently managed by the lazy modifier, while prototype scopes are implemented as factory methods. Dependency wiring is accomplished through standard method calls and polymorphism. This results in significant performance gains, as Jolk applications achieve fast startup times through the absence of classpath scanning or proxy generation.

Multi-environment and test configurations are managed through Specialization and inheritance. Environment variants allow the engine to configure and create the appropriate version during the build or at the system entry point. This ensures deterministic mocking, as developers can simply pass a different map of dependencies to the `#new` message for isolated execution.

	// The Configuration factory  
	class ConfigurationProvider {

		meta lazy ConfigurationProvider CONFIG = Self #new;

		lazy Database database() { 
			// configure database  			^ Database #new;  
		}

		// The Explicit Shutdown Message  
		Self shutdown() {  
			self #database #close  
		}  
	}

	// Starting Up  
	main {  
		/// Injection via Canonical creation method
		Service service = Service #new(ConfigurationProvider #CONFIG #database);

		// Register the module's shutdown  
		Runtime #onShutdown { config #shutdown }
		
		// ...  
	}

	// Unit Testing  
	class ServiceTest {

		// Test Configuration  
		Service service = Service #new(Mock #for(Database));

		// ... execute tests  
	}

### MetaObject Protocol

Within the Jolk engine, the Metaobject protocol serves as the bridge between static structural definitions and dynamic runtime orchestration, resolving the tension between pre-compiled safety and runtime fluidity by treating the `MetaClass` instance as the unit of communication. This synthesis is operationalized through source generation, which provides a deterministic, type-safe API for formal vocabularies—such as REST APIs or database schemas—by defining the architectural "slots" utilized by `#project` during hydration, and identity projection, which facilitates total architectural fluidity for orchestration tools by enabling the runtime discovery of identities through `#instanceProtocol` and the reification of strings into active selectors via the `meta #selector` handshake.

The MetaObjet protocol respects the message boundary fence. If a generated DAO marks a field as private, the `#project` message will result in an exception. Furthermore only verified participants can initiate a meta-level handshake through a reified selector. Because a `selector` in Jolk is a constant, the engine treats the `#project(identity, value)` message as a stable interaction. 

During steady-state execution, the Graal JIT recognizes these patterns as candidates for *Speculative Pruning*. By treating the dynamic send as a candidate for *Polymorphic Inline Caching*, the dynamic dispatch is collapsed. Whether utilizing a generated DAO, a dynamic service container, or a mocking framework, the Tolk Engine ensures the system operates within the highest possible performance density.

Projection in Jolk offers architectural clarity that surpasses the obscurity of source-generated boilerplate. It establishes a deterministic identity, where the logic for value assignment is always a visible handshake, `Receiver #project(Identity, Value)`, eliminating the need to search through extensive generated code common in Java. *Furthermore, it ensures structural clarity by making the state of an object consistently retrievable via `#stateProjection`, thereby removing the "black box" nature of generated Plain Old Java Objects (POJOs) and rendering the data flow across the communicative field explicit and auditable.*

## JolkUnit, the Jolk unit test framework

The discovery and execution model of the Jolk unit test framework relies on the Jolk metaobject protocol and is integrated into the JUnit Platform via a custom test engine implementation. This incubator implementation serves as the reference architecture for message-driven test discovery, registration, execution, and lifecycle management within JVM test runners.

Inspired by Alan Kay’s “The big idea is messaging” and Kent Beck’s `SUnit` [16], the architecture of JolkUnit unifies structural exploration with message-oriented execution. The test engine dispatches messages to a `MetaClass` to resolve test definitions. Test configurations, parameter models, and lifecycles are implemented trough a metaobject protocol, establishing the framework as a participant within the messaging model.

The unit testing framework is synthesised as a direct manifestation of the language's message-dispatch mechanics and embodies this principle by leveraging Jolk's meta-level capabilities for test discovery and configuration. When a test method evaluates, it naturally updates an execution context. This framework handles verification constraints entirely through messaging and metaobject protocols. By utilizing a stateless protocol with default method implementations, JolkUnit serves as a flexible trait interface, bypassing single-inheritance limitations without resorting to external metadata markers.

### Architecture

Protocols with implementations offer self-receiver ergonomics while respecting encapsulation boundaries. Jolk rejects compiler transformations, maintaining fidelity to the message-passing philosophy. The Trade-off: To get clean validation prose without boilerplate, the test class must announce its intent to the type system. By implementing the `protocol TestCase` mitigates the single-inheritance limitation, but the developer must still specify the intent via `implements` on the class definition boundary.

Because the builder pattern operates at execution runtime, datasets can be dynamically computed via logic, database connections, or external configuration files during the building phase, offering a major architectural advantage over static annotations. This approach solves the metadata problem natively within a message-passing layer completes a clear architectural loop. It directly evolves the original design choices of SUnit to solve the challenges of modern enterprise software.

The Jolk unit test framework is designed as a native extension of the unified messaging protocol. It employs a unified protocol-based approach where test case definitions, lifecycle hooks (`#before`, `#after`) , assertions and verifications, are all expressed as message dispatches. This adheres to Jolk's unified messaging protocol, ensuring all testing constructs are first-class citizens within the language's object model. The framework's design prioritizes linguistic homogeneity and architectural purity, allowing for seamless integration with the GraalVM/Truffle runtime's optimization capabilities, such as inline caching and partial evaluation, to achieve high-performance test execution.

### Implementation blueprint

The framework architecture decouples operational definitions from state management through three structural layers: a stateless configuration protocol, an isolated execution context, and an execution engine.

The stateless protocol (`jolk.test.api.TestCase`) establishes the contract for validation routines and test configurations. It provides default method implementations for lifecycle phases and fluent builder selectors that delegate operational data mutations to an active execution context, preserving encapsulation by remaining devoid of mutable instance variables.

```jolk
protocol TestCase {
    
    before() { } 

    after() { }

    fail() {
        #throw("Test Failure: Invariant did not hold")
    }

    assertThat(Boolean condition) {
        condition ?! { #throw("Assertion Mismatch: Expected True") }
    }

	// ...
}
```

The execution engine coordinates execution pipelines via the meta-protocol, driving the verification cycle:
* **Discovery:** The engine loads the foundational `jolk.test.api` protocol definitions, identifies and evaluates Jolk test classes from the classpath within a dedicated Truffle context, loading the target definitions into the runtime environment.
* **Building:** The engine interrogates the loaded `MetaClass` object via the metaobject protocol to extract method selectors. Orchestrator slots containing configuration modifiers evaluate their underlying method blocks to populate the execution context with runtime parameter matrices and lifecycle conditions.
	```
	@Inline
	Extension MetaClassTestExtension on jolk.lang.MetaClass[T extends TestCase] {

		List[Selector] #testSelectors { }

	}
	```
* **Execution:** The engine processes the returned selector payload and the populated execution context, executing the validation logic and logging the test results.

### Integration into the JUnit platform

The Jolk unit test framework achieves compatibility with continuous integration ecosystems through a host-boundary bridge designated as the `JolkTestEngine`. This engine implements the `org.junit.platform.engine.TestEngine` SPI and registers via the standard Java `ServiceLoader` mechanism, allowing external build tools to discover and delegate execution natively.

The integration lifecycle manages guest-to-host boundaries through a continuous execution pipeline. During discovery, the engine instantiates a `JolkTestEngineExecutionContext` to encapsulate the GraalVM polyglot container and to initialize the runtime type space. Transiting to the building phase, the engine maps the resolved guest-space execution configurations into a host-side JUnit `TestDescriptor` tree. Finally, during execution, the engine traverses this tree to initiate guest-language validations, intercepting guest-space exceptions or assertion failures to translate them into corresponding host-side success, skipped, or failure nodes on the JVM platform.

Through this host-bridge design, the framework retains its message-oriented paradigm internally while exposing a standard execution contract to the Java ecosystem.

### Mock

In its current Proof of Concept (PoC) state, the Jolk MOP does not yet encompass the complete set of meta-level capabilities needed for dynamic, on-the-fly class and protocol synthesis. 

While the full architectural formalization of the dynamic MOP is reserved for future ecosystem evolution, the PoC demonstrates that high-density unit testing does not need to wait for runtime class synthesis. Instead, Jolk delivers an expressive, lightweight mock demonstrator by synthesizing classic object-oriented design patterns with native language capabilities: protocols, closures and method references.
   
```
@Test
testGivenThenReturn() {
	repo = #mock(UserRepository);
	User alice = User #new(42, "Alice");
	#given {repo #findById(42)} #then(alice);

	// test & verify
	User user = UserManager #new(repo) #find(42);
	#assertIsPresent(user);
	#assertEquals("Alice", user #name);
	#assertTrue(repo #verify("findById", 42));
}

@Test
testGivenThenThrow() {
	repo = #mock(UserRepository);
	#given [repo #findById(42)] #throw(RuntimeException);

	// test & verify
	Closure find = { UserManager #new(repo) #find(42)};
	#assertThrows(find, RuntimeException );
	#assertTrue(repo #verify("findById", 42));
}
```

## Developer experience

*Jolk adopts Kay’s vision by viewing computation as an emergent protocol where every interaction, from object instantiation to control flow, is a message send. This model allows developers to build 'growable' systems by shifting the focus from internal structure and composition to messaging interaction. By protecting metaboundaries with lexical fences, Jolk preserves a 'Security of Meaning'. Developers can build powerful, resilient JVM components by mastering the protocols of interaction.*

*Jolk achieves a compact grammar, reducing redundant structure and making the message dispatch transparent. Through lexical and semantic anchoring, the language utilizes the hashtag selector (`#`) and semantic casing to transform the source code into a deterministic map of intent. By replacing rigid control-flow structures with polymorphic message sends, Jolk allows the developer to read the system as a series of conversations between identities. Developers gain reliability as the Nothing singleton replaces failure-prone nulls, transforming system crashes into message dispatches. This results in code that is not only shorter but inherently more secure, as the syntax itself forbids the violation of the message boundary.*

---

![Tolk  ](images/tolk.jpg "The Art of Tolk")

# Part Four

**Tolk - The Engine Mechanics**

---

*-- under construction --*

While initial explorations considered a static bytecode compiler targeting `.class` files, the inherent tension between Jolk’s late-bound messaging ergonomics and the rigid constraints of traditional JVM compilation necessitated an architectural evolution. The transition to the Truffle framework ensures that the language’s fluid semantics are preserved while leveraging
specialization to achieve execution parity with native operations.

The Tolk Project is the engineering framework for implementing Jolk on the JVM. It is composed of three primary pillars: the Jolk specification, which defines the formal grammar for fluid method chaining and type-safety; `jolk.lang`, the kernel library containing the core identities and intrinsic primitives necessary for branching, iteration, error recovery, and structural concurrency, while serving as the bridge to the Java ecosystem; and the Tolk Engine. The selection of the Truffle framework[17] is a strategic architectural decision to reconcile Jolk’s message-passing semantics with industrial-grade performance. This enables the engine to perform semantic flattening, collapsing high-level abstractions into optimized machine code through node specialization. While Jolk provides the high-level message-passing syntax, Tolk ensures that complex logic, concurrent execution, and *shim-less* Java interoperability are specialised for peak performance within the GraalVM runtime.

The implementation of the *Jolk Messaging Protocol* is operationalised through the *Truffle DSL*, where the type system is realised as a specialised abstract syntax tree (AST). Within this architecture, every identity exists as a node in a dynamic, self-optimising graph. The *Universal Root Identity* is reified as the base `JolkNode`, providing the foundational infrastructure for the *Intrinsic Object protocol*. This base node enforces a unified dispatch interface across the entire graph, utilizing `@Specialization` annotations to map Jolk message selectors to executable logic. By rooting all entities in this single base class, the execution engine maintains *Identity Congruence*, treating native structures and coalesced external objects as uniform participants within the messaging exchange.

The *Semantic Overlay* is manifested through the use of CallTarget objects, which represent the projection of Jolk protocols onto the substrate. When a message is dispatched, the AST performs a lookup to resolve the appropriate logic, facilitating the structural absorption of external code via method handles. As the execution graph stabilises, the Graal compiler inlines these call targets to erase the overhead of the object boundary. This transition turns the messaging continuum into a high-density execution trace, ensuring that the *Sparse Type System* remains a high-performance machine state.

## Implementation

*TBC*

## Tolk parser

The Tolk Project implements the Jolk grammar by harmonising its human-centric design with the rigorous requirements of high-performance parsing. The toolchain is engineered to resolve token overlaps through a priority hierarchy, ensuring that documentation markers and meta-annotations are correctly intercepted before standard operators. A key syntactic requirement involves the management of functional Whitespace; the lexer distinguishes between ignorable separation and mandatory "Jolk-space," a first-class requirement used to disambiguate object-oriented selectors from standard identifiers.

To manage the shared prefixes inherent in the specification—where annotations and modifiers may initiate both types and members—the project utilizes an LL(k) strategy with a lookahead of $k <> 2$. This approach facilitates the resolution of overlapping structural paths without compromising the original design, while enhancing the precision of syntax diagnostics by deferring decisions until sufficient context is established. Within the construction of the abstract syntax tree (AST), the implementation mandates right-associativity for the power operator and the flattening of deeply nested expression hierarchies into N-ary nodes to optimise memory efficiency and traversal velocity.

The grammar is LL(2): every decision needs at most two tokens of lookahead. One decision — a parenthesized bare identifier — is resolved on the token following the close parenthesis. Implementations may instead split the lambda alternatives and reject malformed parameter lists during parsing; both strategies accept the same language.

While Jolk philosophy posits that "everything is a message," the implementation in `jolk.g4`[18] utilizes a stratified grammar. This architecture—manifested in the tiered expression productions—structurally enforces standard operator precedence and associativity without the need for disambiguation directives.

The `JolkVisitor` is responsible for bridging this technical necessity with the semantic core. It visits these hierarchical tiers and reifies them into `JolkMessageSendNode` instances. This process, referred to as *semantic flattening*, ensures that while the parser understands the "layers" of an expression, the Tolk Engine executes them as a singular, fluid communicative flow.


### Semantic analysis

The semantic phase validates the semantic rules. Tolk ensures that Jolk’s functional intent, concurrent execution models, and interoperability bridges are mapped to the JVM’s runtime. This process is orchestrated via the `JolkVisitor`, which reconciles the parse tree with the target `JolkNode` identities.

* the Covariant Self-Type Return Rule
* ...

### Binary AST packaging

(not yet supported)

The Tolk Parser facilitates the transition from source code to a structured Truffle AST. To minimize the recurring cost of lexical analysis, the engine supports *Binary AST Packaging*. This protocol serializes the unspecialized node hierarchy into a high-density binary format (typically `.jolc`). By persisting the AST in a pre-reified state, the engine achieves near-instantaneous loading, deferring all computational effort to the specialization and JIT phases of the GraalVM.

## Tolk Engine

The engine is designed to leverage emerging JVM features, specifically Project Valhalla for Value Objects, Project Loom for Structured Concurrency, and Project Amber for Pattern Matching.

	Partial Escape Analysis (PEA)
	Loop Unrolling
	Loop Fusion
	Loop Canonicalisation
	Iteration Space Fusion
	Full Unrolling & Folding
	Semantic Guard: Type, Shape or Value
	Identity Promotion
	Reference Wrapping
	Reified Identity
	Scope Permeability
	Node Rewriting
	Type Specialisation
	Monomorphic Fast Path
	Instructional Projection

### Kernel types

Kernel Types are the foundational identities from `jolk.lang` that enable Jolk's pure object-oriented model. By treating every entity—from numbers to truth values—as an object capable of receiving messages, the engine eliminates the syntactic distinction between primitives and heap objects. 

To the developer, these are high-level objects like `Int`, `Boolean`, or `Nothing`. To the Tolk Engine, they are *Pseudo-Identifiers* that undergo semantic flattening. This process "intrinsifies" messages into substrate-native instructions, bypassing standard method dispatch. Through *Identity Erasure*, messages like `1 + 2` are flattened to raw hardware arithmetic, and the `null` identity is projected directly as `aconst_null`. This ensures that Jolk’s expressive syntax executes with the speed of native Java while the compiler handles the "Guided Unboxing" required for shim-less interoperability.

These kernel types constitute the language's logic engine:
*   **Nothing:** The reified absence identity that safely absorbs messages without triggering a `NullPointerException`.
*   **MessageNotUnderstood:** The reified identity representing a dynamic dispatch failure.
*   **Boolean:** Replaces `if/else` keywords with singletons that respond to `?` and `:` messages.
*   **Int / Long:** Provides message-passing for numeric operations and iteration (`#times`).
*   **Closure:** Represents deferred logic, handling control flow via messages like `#while` and `#catch`.
*   **Array:** A message-passing facade for the Java Collections Framework.

The `jolk.lang` class definitions for these types act as a formal bridge to the JVM. They provide method signatures that serve as anchors for documentation and static analysis, even though the Tolk Engine replaces the calls with direct machine code, preserving the object model without sacrificing performance.

**Nothing (`null`):** Reified as the `JolkNothing` singleton, this identity represents the fact of absence. Jolk achieves safe navigation through identity-level polymorphism; the implementation in `JolkNothing.java` exports the `InteropLibrary` and overrides `invokeMember` to return the receiver for unknown selectors. This ensures that message chains collapse gracefully. The Tolk Engine optimizes this via a *Monomorphic Fast Path* in `JolkDispatchNode.doNothing`, allowing safe navigation to occur at the speed of a single reference comparison.

**MessageNotUnderstood:** The Tolk Engine operationalizes this identity within the `JolkDispatchNode` by intercepting heterogeneous substrate failures—ranging from JVM linkage errors to polyglot `UnknownIdentifierException` signals—and projecting them onto a singular `JolkMessageNotUnderstoodException`. This semantic harmonization ensures that dispatch failures are no longer terminal system events but first-class communication signals within the guest environment. By mapping these failures to the `MessageNotUnderstood.jolk` kernel class, the engine enables the guest-level `#catch` protocol to interrogate the `receiver` and `selector` identities, transforming a low-level crash into a manageable and retryable messaging transaction.

**Boolean (`true` / `false`):** Booleans are treated as atomic identities that encapsulate branching behavior. While the guest language interacts with them as first-class objects, the implementation applies *Identity Erasure* to project these identities onto substrate-native scalars. In `JolkDispatchNode.doBooleanCached`, the engine utilizes specialization to map messages like `?` or `?!` directly to specialized call nodes. By caching the `CallTarget` of branches, the engine enables the Graal JIT to resolve high-level logical gates into raw hardware branch instructions. This transformation ensures that Jolk’s keyword-less control flow achieves execution parity with procedural hardware-level branches.

**Primitive Identities:** The engine applies *Identity Erasure* to prevent boxing overhead for primitive types (`Long`, `Int`, ...). This optimization is architecturally anchored in the Truffle `@TypeSystem`, which provides the structural foundation for specialization and generates the optimized check-and-cast logic. Primitive Identities are integrated into the AST through Type Specialisation*, a process that enables the framework to bypass traditional object boxing and execute logic at hardware speeds. The numeric identity is implemented via specialised nodes (e.g., `JolkLongExtension`) that operate directly on Java primitives such as `long`. 
 
Through *Node Rewriting* (specifically realized in `tolk.nodes.JolkDispatchNode.doLong` and `doBoolean`), the engine replaces generic dispatch nodes with these specialised variants when type stability is detected, effectively collapsing the messaging exchange into substrate-native scalar operations. During this process, the engine strips away the object headers and identity metadata to emit raw 64-bit hardware instructions.

**String Identity:** The String Identity is reified through Truffle type specialisation as an irreducible leaf node within the AST. By leveraging `TruffleString`, the implementation ensures the immutable state of literal data and facilitates memory-efficient deduplication. This transformation is mechanically operationalised within the `tolk.nodes.JolkDispatchNode` through two distinct specialisations:
* `doTruffleString`: Serves as the high-performance fast path for guest operations, performing instructional projection for messages such as `#length` and `#isEmpty` to bypass substrate overhead while prioritizing Jolk-native extensions.
* `doJavaString`: Manages the guest-host language boundary by intercepting raw `java.lang.String` instances and applying Identity Restitution to lift them into the optimized `TruffleString` identity.
 
This architecture allows the engine to treat text as a specialized primitive, maintaining full compatibility with the host JVM through automated lifting and lowering.


### Types

**class**

**record**

**enum**

### State

**constant**

**stable**

### Semantic flattening
This is the mechanical process of collapsing high-level messaging protocols into optimized machine code. The foundations of this process trace back to the *Self Language (1989)*[10], which pioneered "Maps" (the conceptual predecessor to Truffle Shapes) to flatten object dispatch. Tolk evolves this by utilizing the *Truffle framework's* implementation of *Partial Evaluation*—the mathematical realization of the Futamura Projections. By taking the generic Jolk interpreter and the specific execution path of a Jolk source file, the engine "collapses" the high-level AST into specialized instructions through the following mechanisms:

**Registry Initialisation:** In the `JolkMetaClass` implementation, the engine avoids the overhead of recursive hierarchy walks during message dispatch. Through the `ensureHydrated()` protocol, complex inheritance trees are collapsed into a consolidated, flattened registry. This transforms what would traditionally be a costly search into a deterministic lookup. By deferring this initialisation until the first message is sent, the engine resolves forward references and dynamic extensions without sacrificing runtime density.

**Field Access Specialization** Because Jolk enforces a "Lexical Fence" where fields are never accessed directly, the `doShapeRead` specialization in `JolkDispatchNode` caches the Truffle `Shape` and the specific `Property` offset for a given selector. During partial evaluation, this dynamic lookup is elided, collapsing a message (e.g., `user #name`) into a raw machine-code memory offset load or store.
 
**Logical Gate Flattening:** This optimization is orchestrated within the `doControlFlow` specialization of `JolkDispatchNode`. By utilizing a `@Shared("callNode")`, the engine provides the Graal JIT with the context to inline multiple execution branches (such as `? :` ternary chains). This collapses the message sends into optimized hardware branch instructions or JVM `tableswitch` opcodes.

**Functional Flow Flattening:** The engine utilizes loop fusion to collapse iterative patterns into single-pass loops. The `JolkDispatchNode` leverages `@Cached IndirectCallNode` to inline closure bodies, allowing the compiler to elide intermediate collection allocations.
 
**Monadic Flow Flattening:** The Tolk Engine identifies monadic chaining patterns (such as the `Match[T]` container) to elide physical object allocation during partial evaluation. By recognizing these logical structures, the JIT collapses high-level pipelines into raw hardware branches, reducing logic execution to zero-cost machine instructions.

Through these specializations, the Tolk Engine resolves dynamic protocols into static hardware instructions, ensuring performance parity with procedural JVM languages while maintaining a pure message-passing model.

### Creation methods

### The implicit return
*   Eliminates explicit return instructions, allowing the compiler to optimize CPU register allocation, safely inline execution paths, and flatten sequential message chains.

### The architecture of Closure projection

Closures in Jolk are reified identities defined as a reified block. A Jolk closure acts as a portable block of execution that maintains a link to its origin. This distinction enables *Transparent Capture*, where closures bypass the "effectively final" constraint to maintain mutable access to their defining environment, and *Return Authority*, where a return statement (`^`) targets the lexical home of the actual defining method.

The Tolk Engine performs *Contextual Projection* by evaluating the selector contract to determine the most efficient substrate strategy:

*   **Transparent (inline) projection:** For structural selectors like `?` or `#while`, the engine applies semantic flattening. The closure boundary is erased, and the logic is merged directly into the caller's stack frame. This enables *Scope Permeability*, allowing lon-local returns (`^`)—implemented via `JolkReturnException`—to reach their lexical home.
*   **Opaque projection:** When a closure is passed as functional data (e.g., `#map`), the engine applies *Boxing*, wrapping the block in a `JolkClosure`. To resolve the friction between Jolk’s natural mutation and the substrate's requirement for stable references, the engine implements *Identity Promotion* through *Reference Wrapping*—projecting mutable identifiers as single-element final arrays.

This transformation follows a rigorous flow in `JolkVisitor` that manages substrate scopes and parameter extraction. During this process, the engine acts as a *Semantic Guard*; if it detects a non-local return (`^`) within a boxed context, it halts the process and issues a `JolkSemanticException`. Optimization is handled in `JolkDispatchNode.doControlFlowDirect`, where specialized call nodes initiate monadic flow flattening to elide container allocations from the compiled execution.

### Interoperability and the reification of Nothing
*   **Identity Restitution:** `doShapeRead` ensures that if a substrate value is `null`, it is automatically "lifted" into the `Nothing` identity before returning to the guest language.

### Exception handling

### Ternary expression projection

Ternary Expression Projection reifies conditional branching as a formal message exchange between a `Boolean` receiver and two competing logical continuations. By eliding procedural keywords, Jolk treats branching as a **Polymorphic Message Dispatch** mediated through the `?` and `:` selectors. This architecture ensures that decision logic adheres to the receiver-centric execution model, where the boolean state determines the execution path. The Tolk Engine utilizes the Truffle DSL to collapse this abstraction, this is achieved through two primary specialization paths within `tolk.nodes.JolkDispatchNode`:

**Closure-Direct Projection:** For branches expressed as closures, the engine employs specialized call nodes (`doTernaryDirect`) to facilitate aggressive inlining. By caching the `CallTarget` of the closure arguments, the engine enables the Graal JIT to perform **Partial Evaluation**, collapsing the closure boundary and allowing **Partial Escape Analysis (PEA)** to elide the allocation of logic blocks during steady-state execution.

**Value-Scalar Projection:** For literal branches (e.g., `receiver ? 0 : 1`), the engine applies **Identity Erasure** via the `doTernaryValues` specialization. By operating directly on substrate primitives, the engine bypasses the `JolkClosure` protocol entirely. This transparency enables the JIT to treat the message send as a primitive hardware branch, facilitating **Loop-Invariant Code Motion (LICM)** to hoist invariant results out of high-frequency iterative workloads.

By transforming branching into a reified projection, Jolk ensures that control flow remains an extensible property of the object model while achieving the execution density of native JVM control structures.

### Null-Coalescing

The Null-Coalescing Operator (`??`) is reified at the engine level by the `JolkDispatchNode.doNullCoalescing` specialization. This optimization leverages the core principle of *Identity Restitution* by ensuring that if the left-hand operand (`receiver`) evaluates to the `Nothing` identity, execution is immediately redirected to the right-hand operand (`fallback`). If the `receiver` is not `Nothing`, the engine applies *Monadic Flow Flattening* to elide the `fallback` evaluation entirely, collapsing the operation into a single, high-performance identity check and direct return. This ensures that Jolk's safe navigation, targeting execution parity with native JVM null-checks.

### Deferred Computation

Jolk's `lazy` keyword, enabling deferred and memoized initialization, is operationalized at the engine level through the `JolkLazyValue` identity. This mechanism ensures that resource-heavy computations or object instantiations are performed only once, upon their first access, and their results are then cached for subsequent retrievals.

The core of this implementation resides in the `JolkLazyValue` class, which encapsulates the `initializer` (a `JolkNode` representing the deferred computation) and manages its lifecycle. It employs an `AtomicReference` to store the computed value, utilizing a sentinel pattern for efficient, thread-safe state management. Upon the first access to a `lazy` field or method, the `JolkDispatchNode.doShapeRead` specialization intercepts the read operation. If the field's value is a `JolkLazyValue` instance, it triggers the `get(receiver)` method.

Within this `get` method, the Tolk Engine orchestrates the on-demand execution of the initializer. To maintain integrity, a `ThreadLocal` guard is utilized to detect and prevent circular dependencies during initialization, throwing a `JolkSemanticException` if a cycle is identified. Once computed, the result is atomically stored, ensuring that all subsequent accesses retrieve the cached value without re-evaluation. Although the initial evaluation path is marked with `@TruffleBoundary` as a slow path, the subsequent direct access to the memoized result benefits from specialization, effectively collapsing the lookup into a direct memory read after the first resolution. This approach provides a native, high-performance solution for resource management and dependency initialisation, aligning with Jolk's pure message-passing architecture.
 
### Meta-level

To keep meta-level dispatch efficient, the Tolk Engine applies the `@Intrinsic` directive: the compiler maps protocol constants during the build phase, flattening dynamic message sends so that the Graal JIT [12] can apply speculative pruning and polymorphic inline caching. Dynamic projections then execute at the same cost as static dispatch.

### Concurrency

### Extension

### Lifecycle and integrity

## Experimental evaluation

## A future-proof JVM synthesis

---

# References

[1]: Kay, Alan (1996). The early history of Smalltalk | History of programming languages Pages 511 - 598. ACM. ([ISBN:0201895021](https://dl.acm.org/doi/epdf/10.1145/234286.1057828))

[2]: Goldberg, A.; Robson, D. (1983). Smalltalk-80: The Language and its Implementation. Addison Wesley. ISBN:978-0-201-11371-6.

[3]: Roose, W.; Verachtert, W. (1988). Een andere kijk op object georiënteerdheid ([biblio.vub.ac.be/opac/3/200467](http://biblio.vub.ac.be/opac/3/200467)) \[Proefschrift, VUB\]. VUB Universiteitsbibliotheek

[4]: Bracha, G.; Griswold, D. (1993)  Strongtalk: Typechecking Smalltalk in a Production Environment. OOPSLA ‘93.

[5]: Halstead, M. H. (1977). Elements of Software Science. Elsevier North-Holland. ISBN: 978-0-444-00205-1.

[6]: Chidamber, S. R.; Kemerer, C. F. (1994). A Metrics Suite for Object Oriented Design. IEEE Transactions on Software Engineering, 20(6), pp. 476–493. doi:10.1109/32.295895.

[7]: Jones, C. (2010). Scoring and Evaluating Software Methods, Practices, and Results (Version 3.1). Namcook Analytics LLC.

[8]: Schärli, N.; Ducasse, S.; Nierstrasz, O.; Black, A. P. (2003). Traits: Composable Units of Behaviour. ECOOP 2003.

[9]: Canning, P. S.; Cook, W. R.; Hill, W. L.; Mitchell, J. C.; Olthoff, W. (1989). F-bounded polymorphism for object-oriented programming. In Conference on Functional Programming Languages and Computer Architecture.

[10]: Chambers, C.; Ungar, D. (1989). Customization: Optimizing Compiler Technology for Self, a Dynamically-Typed Object-Oriented Programming Language. In PLDI '89 (pp. 146–160).

[11]: Kiczales, G.; des Rivieres J.; Bobrow D. (1991). The Art of the Metaobject Protocol. The MIT Press. ISBN 978-0262610742. 

[12]: Würthinger, T., et al. (2013). One VM to Rule Them All. ([Proceedings of the 2013 ACM International Symposium on New Ideas, New Paradigms, and Reflections on Programming & Software (Onward!)](https://lafo.ssw.uni-linz.ac.at/pub/papers/2013_Onward_OneVMToRuleThemAll.pdf))

[13]: Woolf, B. (1998). Null Object. Pattern Languages of Program Design 3. Addison-Wesley.

[14]: Cointe, P (1987). "Metaclasses are first class: The ObjVlisp Model". ACM SIGPLAN Notices. 22 (12): 156–162. doi:10.1145/38807.38822. ISSN 0362-1340.

[15]: Bruce, K. B. (2002). Foundations of Object-Oriented Languages: Types and Semantics. MIT Press.

[16]: Beck, K. (1998). Kent Beck's Guide to Better Smalltalk chapter 30 "Simple Smalltalk Testing". Cambridge University Press. ISBN 978-0-521-64437-2

[17]: Oracle. Truffle Language Implementation Framework. GraalVM Documentation. ([https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework](https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework))

[18]: Roose, W. (2026). The Jolk ANTLR4 Grammar (`jolk.g4`). (https://github.com/wouterroose-tech/jolk/blob/main/poc/grammar/src/main/antlr4/tolk/grammar/jolk.g4)

---

# Glossary of terms
The terminology and recontextualized concepts of the Jolk language:

**Atomic identity:** A terminal, first-class identity (such as `true`, `false`, `Nothing`or Value Objects) that participates in the messaging protocol as a recipient.   

**Guided coercion:** The active, type-aware alignment of differing numerical identities to a common protocol, requiring explicit guidance (e.g., `#asInteger`) for any lossy transition.

**Identity congruence:** The singular logical representation for all entities—including complex objects, primitives, and absence—by aligning the abstract identity defined in the source code with the representation of the Tolk Engine.

**Identity erasure:** A performance strategy where the engine physically strips away object structures and headers at the machine level.

**Identity Restitution:** A host-guest language boundary protocol that "lifts" raw JVM `null` pointers into the `Nothing` singleton to ensure they can safely receive messages.

**Implicit self-receiver**: The syntactic resolution rule where a bare hashtag selector (e.g., `#name`) automatically binds to self as its target receive.

**Implicit self-return:** Method-level behavior where omitting an explicit return type defaults the return value to self.

**Lexical fence:** A structural boundary that enforces message-only interaction.

**Message boundary:** The structural line separating an object's internal state from the external message-passing environment. It enforces *Local Retention* and *Encapsulation*

**MetaClass:** The intrinsic `MetaClass[T]` that reifies the type; every class is an instance of its metaclass, all metaclasses are instances of this single intrinsic, and the hierarchy terminates there.

**Nothing:** A reified, first-class Atomic Identity representing the fact of absence and referred to by the reserved object identifier `null`.

**Semantic casing:** A lexical rule where the first-letter casing of an identifier determines its semantic role: Meta-Objects are Uppercase, while instances and selectors are lowercase.

**Semantic flattening:** The process where the Tolk Engine utilizes dynamic node specialization to collapse high-level message-passing abstractions and patterns into optimized machine code, effectively eliminating dispatch overhead during GraalVM partial evaluation.

**Substrate:** Substrate VM is an Oracle internal project name for the technology behind GraalVM Native Image.

---

# Copyright 

Copyright © 2026 by Wouter Roose

This book is published under a Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International License (CC BY-NC-SA 4.0).

The right of Wouter Roose to be identified as the author of this work has been asserted by them in accordance with the Copyright, Designs and Patents Act 1988\.

You are free to share, copy, redistribute, adapt, remix, transform, and build upon the material in any medium or format. Under the following terms:

- Attribution: You must give appropriate credit to the author, provide a link to the license, and indicate if changes were made.

- NonCommercial: You may not use the material for commercial purposes.

- ShareAlike: If you remix, transform, or build upon the material, you must distribute your contributions under the same license as the original.

Jolk is a community effort to implement an experimental message-oriented language specification designed for the JVM and to closely integrate with the Java ecosystem and is not endorsed by, affiliated with, or supported by Oracle. Java and the JVM are registered trademarks of Oracle and/or its affiliates. Other names may be trademarks of their respective owners. The use of these trademarks does not imply any affiliation with or endorsement by the trademark holders.

**Disclaimer**

The code examples and software architecture described in this book are provided for educational and design purposes. While every precaution has been taken in the preparation of this book, the author assumes no responsibility for errors or omissions, or for damages resulting from the use of the information contained herein. The software is provided "as is," and the author disclaims all warranties, express or implied.

The artwork and illustrations were conceptualised by the author and rendered using Generative AI tools. Portions of the manuscript were drafted with the assistance of LLM's to accelerate the writing process. However, all technical specifications, code examples, and architectural definitions were reviewed, verified, and refined by the author.

---