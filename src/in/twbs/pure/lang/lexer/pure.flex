package in.twbs.pure.lang.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import static in.twbs.pure.lang.psi.PureTokens.*;

%%

%unicode
%class _PureLexer
%implements FlexLexer
%function advance
%type IElementType
%line
%column

%eof{

return;

%eof}

%{

%}


whitespace = [ \t\f\r\n]
opChars = [\:\!#\$%&*+./<=>?@\\\^|\-~]
identStart = [:lowercase:]|"_"
identLetter = [:letter:]|[:digit:]|[_\']
properStart = [:uppercase:]
properLetter = [:letter:]|[:digit:]

decimal = [0-9]+
hexadecimal = [xX][0-9a-zA-Z]+
octal = [oO][0-7]+
stringChar = [^\"\\\0-\u001B]|{stringEscape}
stringEscape = "\\" ({escapeGap} | {escapeEmpty} | {escapeCode} )
escapeEmpty = "&"
escapeGap = {whitespace}*"\\"
escapeCode = {charEsc} | {charNum} | {charAscii} | {charControl}
charEsc = [abfnrtv\\\"\']
charNum = {decimal} | "x" [0-9a-zA-Z]+ | "o" [0-7]+
charAscii = "BS"|"HT"|"LF"|"VT"|"FF"|"CR"|"SO"|"SI"|"EM"|"FS"|"GS"|"RS"|"US"|"SP"|"NUL"|"SOH"|"STX"|"ETX"|"EOT"|"ENQ"|"ACK"|"BEL"|"DLE"|"DC1"|"DC2"|"DC3"|"DC4"|"NAK"|"SYN"|"ETB"|"CAN"|"SUB"|"ESC"|"DEL"
charControl = "^" [:uppercase:]

%x COMMENT

%{
   int comment_nesting = 0;
   int yyline = 0;
   int yycolumn = 0;
%}

%%

<COMMENT> {

"{-"                           { comment_nesting++; }
"-}"                           { comment_nesting--; if (comment_nesting == 0) { yybegin(YYINITIAL); return MLCOMMENT; } }
<<EOF>>                        { return MLCOMMENT; }
[^]                            { }

}

<YYINITIAL> {

{whitespace}                   { return WS; }

"{-"                           { yybegin(COMMENT); comment_nesting = 1; }
"--" [^\n]*                    { return SLCOMMENT; }

"data"                         { return DATA; }
"type"                         { return TYPE; }
"foreign"                      { return FOREIGN; }
"import"                       { return IMPORT; }
"infixl"                       { return INFIXL; }
"infixr"                       { return INFIXR; }
"infix"                        { return INFIX; }
"class"                        { return CLASS; }
"instance"                     { return INSTANCE; }
"module"                       { return MODULE; }
"case"                         { return CASE; }
"of"                           { return OF; }
"if"                           { return IF; }
"then"                         { return THEN; }
"else"                         { return ELSE; }
"do"                           { return DO; }
"let"                          { return LET; }
"true"                         { return TRUE; }
"false"                        { return FALSE; }
"in"                           { return IN; }
"where"                        { return WHERE; }

"=>"                           { return DARROW; }
"->"                           { return ARROW; }
"="                            { return EQ; }
"."                            { return DOT; }
"\\"                           { return DBACKSLASH; }

","                            { return COMMA; }
"("                            { return LPAREN; }
")"                            { return RPAREN; }
"["                            { return LBRACK; }
"]"                            { return RBRACK; }
"{"                            { return LCURLY; }
"}"                            { return RCURLY; }

"0"({hexadecimal}|{octal}|{decimal})|{decimal} { return NATURAL; }

"\"" {stringChar}* "\""        { return STRING; }
"\"" {stringChar}*             { return ERROR; }

{identStart}{identLetter}*     { return IDENT; }
{properStart}{properLetter}*   { return PROPER_IDENT; }
{opChars}+                     { return OPERATOR; }

.                              { return ERROR; }
}