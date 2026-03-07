grammar jolk;

/*
    Jolk ANTLR4 Grammar
    ===================
    This grammar is aligned with the jolk.bnf specification.
*/

// Parser rules
unit            : package_decl? import_decl* annotation* ( type_decl | extension_decl)? EOF;

package_decl    : PACKAGE namespace ';' ;
import_decl     : IMPORT namespace ('.' MUL)? ';' ;
namespace       : identifier ('.' identifier)* ;

type_decl       : visibility? variability? archetype type_bound LBRACE type_mbr* RBRACE ;
visibility      : PUBLIC | PACKAGE | PROTECTED | PRIVATE ;
variability     : ABSTRACT | FINAL ;
archetype       : CLASS | VALUE | RECORD | ENUM | PROTOCOL ;
type_bound      : type type_contracts? ;
type            : self_type | (identifier DOT)* MetaId type_args? ;
type_args       : LT type_bound (COMMA type_bound)* GT ;
type_contracts  : EXTENDS type (IMPLEMENTS type (AMP type)*)?
                | IMPLEMENTS type (AMP type)* ;

type_mbr        : annotation* (member | enum_constant) ;
member          : visibility? ( META? state | variability? META? method ) ;
state           : ( constant | field ) SEMI ;
constant        : CONSTANT type binding ;
field           : type identifier assignment? ;
binding         : identifier ASSIGN expression ;
assignment      : ASSIGN expression ;
enum_constant   : MetaId arguments? ';' ;

method          : LAZY? type_args? type selector_id LPAREN typed_params? RPAREN ( block | ';' ) ;
selector_id     : identifier | operator ;
typed_params    : annotated_type ( InstanceId (COMMA annotated_type InstanceId)* (COMMA annotated_type vararg_id)? | vararg_id ) ;
annotated_type  : annotation* type ;
vararg_id	    : SPREAD InstanceId ;

extension_decl  : EXTENSION MetaId EXTENDS type LBRACE extension_mbr* RBRACE ;
extension_mbr   : annotation* visibility? variability? method ;

annotation      : AT identifier (LPAREN annotation_args? RPAREN)? ;
annotation_args : annotation_val | annotation_arg (COMMA annotation_arg)* ;
annotation_arg  : identifier ASSIGN annotation_val ;
annotation_val  : literal | annotation | LBRACE (annotation_val (COMMA annotation_val)*)? RBRACE ;

block           : LBRACE statements? RBRACE ;
statements      : statement (';' statement)* ';'?;
statement       : constant | field | binding | returnOp? expression ;
expression      : logic_or (condOp expression (COLON expression)?)? ;
logic_or        : logic_and (OR logic_and)* ;
logic_and       : inclusive_or (AND inclusive_or)* ;
inclusive_or    : exclusive_or (BIT_OR exclusive_or)* ;
exclusive_or    : bitwise_and (BIT_XOR bitwise_and)* ;
bitwise_and     : equality (AMP equality)* ;
equality        : comparison (eqOp comparison)* ;
comparison      : term (relOp term)* ;
term            : factor (addOp factor)* ;
factor          : unary (mulOp unary)* ;
unary           : (NOT | negOp) unary | power ;
power           : message (powOp unary)? (NULL_COALESCE power)? ;
message         : primary (selector payload?)* ;
primary         : reserved | type | identifier | literal | list_literal | LPAREN expression RPAREN | closure | method_reference ;
closure         : LBRACK (stat_params LAMBDA)? statements? RBRACK ;
method_reference : ( identifier | reserved ) HASH_HASH identifier ;
payload         : arguments | closure ;
arguments       : LPAREN (expression (COMMA expression)*)? RPAREN ;
stat_params     : typed_params | inferred_params ;
inferred_params : InstanceId (COMMA InstanceId)* ;

reserved        : TRUE | FALSE | NULL | SUPER | self_type | self_instance ;
self_type       : SELF_TYPE ;
self_instance   : SELF_INSTANCE ;
selector        : HASH identifier ;
identifier      : MetaId | InstanceId ;
literal         : NumberLiteral | StringLiteral | CharLiteral ;
list_literal    : array_literal | set_literal | map_literal ;
array_literal   : HASH LBRACK literal_list? RBRACK ;
set_literal     : HASH LBRACE literal_list? RBRACE ;
map_literal     : HASH LPAREN map_list? RPAREN ;
literal_list    : expression (COMMA expression)* ;
map_list        : map_entry (COMMA map_entry)* ;
map_entry       : expression LAMBDA expression ;

returnOp        : CARET ;
condOp          : QMARK | QMARK_NOT ;
operator        : addOp | mulOp | eqOp | relOp | NOT | powOp | QMARK | QMARK_NOT | NULL_COALESCE | HASH_HASH ;

addOp           : ADD | SUB;
mulOp           : MUL | DIV | MOD;
negOp           : SUB;
notOp           : NOT;
powOp           : POW;
eqOp            : EQ | NE | EQ_TILDE | NE_TILDE;
relOp           : GT | GE | LT | LE;

// Lexer rules

// Keywords
PACKAGE: 'package';
IMPORT: 'import';
CLASS: 'class';
VALUE: 'value';
RECORD: 'record';
ENUM: 'enum';
PROTOCOL: 'protocol';
EXTENSION: 'extension';
IMPLEMENTS: 'implements';
EXTENDS: 'extends';
PUBLIC: 'public';
PROTECTED: 'protected';
PRIVATE: 'private';
ABSTRACT: 'abstract';
FINAL: 'final';
CONSTANT: 'constant';
META: 'meta';
LAZY: 'lazy';
TRUE: 'true';
FALSE: 'false';
NULL: 'null';
SUPER: 'super';
SELF_TYPE: 'Self';
SELF_INSTANCE: 'self';

// Symbols
LPAREN: '(';
RPAREN: ')';
LBRACE: '{';
RBRACE: '}';
LBRACK: '[';
RBRACK: ']';
SEMI: ';';
COMMA: ',';
DOT: '.';
ASSIGN: '=';
GT: '>';
LT: '<';
NOT: '!';
TILDE: '~';
QMARK: '?';
COLON: ':';
AMP: '&';

// Compound Symbols
EQ: '==';
LE: '<=';
GE: '>=';
NE: '!=';
EQ_TILDE: '~~';
NE_TILDE: '!~';
AND: '&&';
OR: '||';
BIT_OR: '|';
BIT_XOR: '|!';
ADD: '+';
SUB: '-';
MUL: '*';
DIV: '/';
MOD: '%';
POW: '**';
CARET: '^';
HASH: '#';
HASH_HASH: '##';
AT: '@';
NULL_COALESCE: '??';
LAMBDA: '->';
SPREAD: '...';
QMARK_NOT: '?!';

// Identifiers
MetaId: [A-Z] [a-zA-Z0-9_]*;
InstanceId: [a-z] [a-zA-Z0-9_]*;

// Literals
NumberLiteral: [0-9]+ ('.' [0-9]+)?;
StringLiteral: '"' ( ~["\\] | '\\' . )*? '"';
CharLiteral: '\'' ( ~['\\] | '\\' . )? '\'';

// Whitespace and Comments
WS: [ \t\r\n]+ -> skip;
LINE_COMMENT: '///' ~[\r\n]* -> skip; // Markdown doc comment
COMMENT: '//' ~[\r\n]* -> skip;
BLOCK_COMMENT: '/*' .*? '*/' -> skip;