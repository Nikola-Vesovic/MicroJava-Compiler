**MicroJava Compiler**

A compiler for the MicroJava programming language, implemented as a university project for the Programming Translators 1 course at the School of Electrical Engineering, University of Belgrade.

**About**

MicroJava is a simplified, Java-like language. This compiler translates syntactically and semantically valid MicroJava source code into MicroJava bytecode, which runs on the MicroJava Virtual Machine (MJVM).

**Features**

- Lexical analysis — tokenizes MicroJava source using a JFlex-generated scanner
- Syntax analysis — LALR(1) parser built with AST-CUP, producing an abstract syntax tree; includes error recovery  
- Semantic analysis — AST visitor that validates context conditions and populates the symbol table  
- Code generation — AST visitor that emits valid MJVM bytecode into an executable .obj file

**Tech Stack**

Java, JFlex, CUP (AST-CUP extension)


**How to Build**

Open the project in Eclipse and run the provided build.xml Ant script, or build manually using the JFlex and CUP tools from the course site.


**How to Run**

java rs.ac.bg.etf.pp1.Compiler <input.mj> <output.obj>
