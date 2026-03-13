
package rs.ac.bg.etf.pp1;
import java_cup.runtime.Symbol;

%%

%{

	//ukljucivanje informacije o poziciji tokena
	private Symbol new_symbol(int type) {
		return new Symbol(type, yyline+1, yycolumn);
	}
	
	//ukljucivanje informacije o poziciji tokena
	private Symbol new_symbol(int type, Object value) {
		return new Symbol(type, yyline+1, yycolumn, value);
	}

%}

%cup
%line
%column
%xstate COMMENT

%eofval{
	return new_symbol(sym.EOF);
%eofval}

%%

" " | "\t" | "\r\n" | "\n" | "\r" | "\f" { }
//[ \t\r\n\f]+   { }


"program"							{ return new_symbol(sym.PROG, yytext()); }
"break"								{ return new_symbol(sym.BREAK, yytext()); }
"enum"								{ return new_symbol(sym.ENUM, yytext()); }
"else"								{ return new_symbol(sym.ELSE, yytext()); }
"const"								{ return new_symbol(sym.CONST, yytext()); }
"if"								{ return new_symbol(sym.IF, yytext()); }
"new"								{ return new_symbol(sym.NEW, yytext()); }
"print"								{ return new_symbol(sym.PRINT, yytext()); }
"read"								{ return new_symbol(sym.READ, yytext()); }
"return"							{ return new_symbol(sym.RETURN, yytext()); }
"void"								{ return new_symbol(sym.VOID, yytext()); }
"continue"							{ return new_symbol(sym.CONTINUE, yytext()); }
"for"								{ return new_symbol(sym.FOR, yytext()); }
"length"							{ return new_symbol(sym.LENGTH, yytext()); }
"switch"							{ return new_symbol(sym.SWITCH, yytext()); }
"case"								{ return new_symbol(sym.CASE, yytext()); }
//"default"							{ return new_symbol(sym.DEFAULT, yytext()); }
//"while"							{ return new_symbol(sym.WHILE, yytext()); }
//"goto"							{ return new_symbol(sym.GOTO, yytext()); }



//"^"    							{ return new_symbol(sym.MAX, yytext()); }
//"@"    							{ return new_symbol(sym.MONKEY, yytext()); }
"+"									{ return new_symbol(sym.PLUS, yytext()); }
"-"									{ return new_symbol(sym.MINUS, yytext()); }
"*"									{ return new_symbol(sym.MUL, yytext()); }
"/"									{ return new_symbol(sym.DIV, yytext()); }
"%"									{ return new_symbol(sym.MOD, yytext()); }
"==" 								{ return new_symbol(sym.ISEQL, yytext()); }
"!=" 								{ return new_symbol(sym.NOTEQL, yytext()); }
">" 								{ return new_symbol(sym.GRT, yytext()); }
">=" 								{ return new_symbol(sym.GRTEQL, yytext()); }
"<" 								{ return new_symbol(sym.LWR, yytext()); }
"<=" 								{ return new_symbol(sym.LWREQL, yytext()); }
"&&"								{ return new_symbol(sym.AND, yytext()); }
"||"								{ return new_symbol(sym.OR, yytext()); }
"="									{ return new_symbol(sym.ASSIGN, yytext()); }
"++"								{ return new_symbol(sym.INC, yytext()); }
"--"								{ return new_symbol(sym.DEC, yytext()); }
","									{ return new_symbol(sym.COMMA, yytext()); }
"."									{ return new_symbol(sym.DOT, yytext()); }
"("									{ return new_symbol(sym.LPAREN, yytext()); }
")"									{ return new_symbol(sym.RPAREN, yytext()); }
"["									{ return new_symbol(sym.LBRACK, yytext()); }
"]"									{ return new_symbol(sym.RBRACK, yytext()); }
"{"									{ return new_symbol(sym.LBRACE, yytext()); }
"}"									{ return new_symbol(sym.RBRACE, yytext()); }
"?"    								{ return new_symbol(sym.TERNARY, yytext()); }
":"									{ return new_symbol(sym.COLON, yytext()); }
";"									{ return new_symbol(sym.SEMI, yytext()); }


"//" 								{ yybegin(COMMENT); }
<COMMENT> .							{ yybegin(COMMENT); }
<COMMENT> "\r\n"|"\n"				{ yybegin(YYINITIAL); }


[0-9]+								{ return new_symbol(sym.NUMBER, new Integer(yytext())); }
"'"."'"								{ return new_symbol(sym.CHARACTER, new Character(yytext().charAt(1))); }
("true"|"false")					{ return new_symbol(sym.BOOL, yytext().equals("true")? 1 : 0); }
([a-z]|[A-Z])[a-z|A-Z|0-9|_]*		{ return new_symbol(sym.IDENT, yytext()); }

. 									{ System.err.println("Leksicka greska (" + yytext() + ") na liniji " + (yyline+1) + " u koloni " + (yycolumn+1) + "\n" ); }


