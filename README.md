# piraha-peg
Piraha is a [Parsing Expression Grammar](https://en.wikipedia.org/wiki/Parsing_expression_grammar) library written in Java

The name comes from the spoken language called [Pirahã](https://en.wikipedia.org/wiki/Pirah%C3%A3_language), which by some measure is
considered the simplest language in the world.

The syntax is loosely based on regular expressions as they are found in Perl or Java.
The Reference link below provides a detailed description of the pattern elements. The
Quick Start describes how to use the API. See the Calculator and CSscript examples to
get a more detailed idea on how to write programs with Piraha.

Note, however, that since Piraha does not describe a <a href="https://en.wikipedia.org/wiki/Regular_language">
Regular Language</a> the pattern expressions used to describe it are not properly called
"regular expressions." However, because of the similarity in form between Parsing Expression
Grammars (PEGs) and regular expressions, I use the term "pegular expressions" instead.

[Reference](https://cdn.rawgit.com/stevenrbrandt/piraha-peg/master/doc/ref.html) - This
reference card provides a description of the pattern elements that can be used in a
pegular expression.

[Quick Start](https://cdn.rawgit.com/stevenrbrandt/piraha-peg/master/doc/QuickStart.html) -
This guide shows you how to call the Piraha engine from Java. It also explains the differences
between the Piraha matcher and a regular expression matcher.

[Grammar Files](https://cdn.rawgit.com/stevenrbrandt/piraha-peg/master/doc/GrammarFiles.html) -
This document explains how to construct and use Piraha expressions from inside a Grammar File,
i.e. a file containing multiple pegular expressions.

[Calculator](https://cdn.rawgit.com/stevenrbrandt/piraha-peg/master/doc/Calculator.html) -
What grammar engine is complete without a calculator example?

[CScript](https://cdn.rawgit.com/stevenrbrandt/piraha-peg/master/doc/CScript.html) -
Calculators are boring! Here's how you can write a complete language using Piraha.

[Citing Piraha](http://ieeexplore.ieee.org/document/5698011/) This is a link to the
Piraha Paper. You can also cite the digital object identifier:
[https://dx.doi.org/10.6084/m9.figshare.3837840](DOI: https://dx.doi.org/10.6084/m9.figshare.3837840)
