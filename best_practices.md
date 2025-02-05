# Project Best Practices

These best practises should be considered by contributors (and AI agents):

- **Kotlin Idiomatic Code**: Ensure that all code adheres to Kotlin coding conventions the [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html).
- _Public APIs_: APIs should be Kotlin idiomatic while still usable from Java.
Public APIs should be documented with examples is essential for clarity and usability.
- **Test coverage**: All code should have high test coverage, this means it should be designed to be testable.
High level of coverage should be achieved in unit tests, integration tests are only used to validate actual DB behaviour.
- **Design Principles**: code should respect the SOLID principles:
  - _Single Responsibility Principle_: A class should have only one reason to change.
  - Open/Closed Principle: Software entities should be open for extension, but closed for modification.
  - _Liskov Substitution Principle_: Objects in a program should be replaceable with instances of their subtypes without altering the correctness of that program.
  - _Interface Segregation Principle_: A client should never be forced to implement an interface that it doesn't use.
  - _Dependency Inversion Principle_: High-level modules should not depend on low-level modules. Both should depend on abstractions.
- **Additional principles**:
  - Code should depend on abstractions, not on concretions.
  - Composition over Inheritance: Favor composition over inheritance.
  - Don't Repeat Yourself: Every piece of knowledge must have a single, unambiguous, authoritative representation within a system.
  - Keep It Simple, Stupid: Systems work best if they are kept simple rather than made complex.
