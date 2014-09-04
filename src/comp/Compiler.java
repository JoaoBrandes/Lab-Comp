
package comp;

import ast.*;
import lexer.*;

import java.io.*;
import java.util.*;

public class Compiler {

	/*
	  	AssignExprLocalDec ::= Expression [ "=" Expression ] | LocalDec
		BasicType ::= "void" | "int" | "boolean" | "String"
		BasicValue ::= IntValue | BooleanValue | StringValue
		BooleanValue ::= "true" | "false"
		ClassDec ::= "class" Id [ "extends" Id ] "{" MemberList "}"
		CompStatement ::= "{" { Statement } "}"
		Digit ::= "0" | ... | "9"
		Expression ::= SimpleExpression [ Relation SimpleExpression ]
		ExpressionList ::= Expression { "," Expression }
							Factor ::= BasicValue |
							"(" Expression ")" |
							"!" Factor |
							"null" |
							ObjectCreation |
							PrimaryExpr
		FormalParamDec ::= ParamDec { "," ParamDec }
		HighOperator ::= "*" | "=" | "&&"
		Id ::= Letter { Letter | Digit | "_" }
		IdList ::= Id { "," Id }
		IfStat ::= "if" "(" Expression ")" Statement
					[ "else" Statement ]
		InstVarDec ::= Type IdList ";"
		IntValue ::= Digit { Digit }
		LeftValue ::= [ ("this" | Id ) "." ] Id
		Letter ::= "A" | ... | "Z" | "a" | ... | "z"
		LocalDec ::= Type IdList ";"
		LowOperator ::= "+" | "-" | "||"
		MemberList ::= { Qualifier Member }
		Member ::= InstVarDec | MethodDec
		MethodDec ::= Type Id "(" [ FormalParamDec ] ")"
						"f" StatementList "g"
		ObjectCreation ::= "new" Id "(" ")"
		ParamDec ::= Type Id
		Program ::= ClassDec { ClassDec }
		Qualifier ::= [ "final" ] [ "static" ] ( "private" | "public")
		ReadStat ::= "read" "(" LeftValue { "," LeftValue } ")"
		PrimaryExpr ::= "super" "." Id "(" [ ExpressionList ] ")" |
						Id |
						Id "." Id |
						Id "." Id "(" [ ExpressionList ] ")" |
						Id "." Id "." Id "(" [ ExpressionList ] ")" |
						"this" |
						"this" "." Id |
						"this" "." Id "(" [ ExpressionList ] ")" |
						"this" "." Id "." Id "(" [ ExpressionList ] ")"
		Relation ::= "==" | "<" | ">" | "<=" | ">=" | "!="
		ReturnStat ::= "return" Expression
		RightValue ::= "this" [ "." Id ] | Id [ "." Id ]
		Signal ::= "+" | "-"
		SignalFactor ::= [ Signal ] Factor
		SimpleExpression ::= Term { LowOperator Term }
		Statement ::= AssignExprLocalDec ";" | IfStat | WhileStat | ReturnStat ";" |
						ReadStat ";" | WriteStat ";" | "break" ";" | ";" | CompStatement
		StatementList ::= { Statement }
		Term ::= SignalFactor { HighOperator SignalFactor }
		Type ::= BasicType | Id
		WriteStat ::= "write" "(" ExpressionList ")"
		WhileStat ::= "while" "(" Expression ")" Statement
		
	 */
	
	// compile must receive an input with an character less than
	// p_input.lenght
	public Program compile(char[] input, PrintWriter outError) {

		error = new CompilerError(new PrintWriter(outError));
		symbolTable = new SymbolTable();
		lexer = new Lexer(input, error);
		error.setLexer(lexer);
		
		Program program = null;
		try {
			lexer.nextToken();
			if ( lexer.token == Symbol.EOF )
				error.show("Unexpected end of file");
			program = program();
			if ( lexer.token != Symbol.EOF ) {
				program = null;
				error.show("End of file expected");
			}
		}
		catch (Exception e) {
			// the below statement prints the stack of called methods.
			// of course, it should be removed if the compiler were
			// a production compiler.

			e.printStackTrace();
			program = program();
		}

		return program;
	}

	private Program program() {
		// Program ::= KraClass { KraClass }
		ArrayList<KraClass> kra = new ArrayList<KraClass>();
		kra.add(classDec());
		while (lexer.token == Symbol.CLASS)
			kra.add(classDec());
		return new Program(kra);
	}

	private KraClass classDec() {
		// Note que os métodos desta classe não correspondem exatamente às
		// regras
		// da gramática. Este método classDec, por exemplo, implementa
		// a produção KraClass (veja abaixo) e partes de outras produções.

		/*
		 * KraClass ::= ``class'' Id [ ``extends'' Id ] "{" MemberList "}"
		 * MemberList ::= { Qualifier Member } 
		 * Member ::= InstVarDec | MethodDec
		 * InstVarDec ::= Type IdList ";" 
		 * MethodDec ::= Qualifier Type Id "("[ FormalParamDec ] ")" "{" StatementList "}" 
		 * Qualifier ::= [ "static" ]  ( "private" | "public" )
		 */
		
		if ( lexer.token != Symbol.CLASS ) 
			error.show("'class' expected");
		lexer.nextToken();
		if ( lexer.token != Symbol.IDENT )
			error.show(CompilerError.ident_expected);
		String className = lexer.getStringValue();
		KraClass kra = new KraClass(className);
		if(symbolTable.getInGlobal(className) != null){
			error.show("Class "+className+" already declared");
		}
		symbolTable.putInGlobal(className, kra);
		lexer.nextToken();
		if ( lexer.token == Symbol.EXTENDS ) {
			lexer.nextToken();
			if ( lexer.token != Symbol.IDENT )
				error.show(CompilerError.ident_expected);
			String superclassName = lexer.getStringValue();
			kra.setSuperclass(symbolTable.getInGlobal(superclassName));
			
			lexer.nextToken();
		}
		if ( lexer.token != Symbol.LEFTCURBRACKET )
			error.show("{ expected", true);
		lexer.nextToken();
		
		InstanceVariable value;
		Method m = null;
		
		while (lexer.token == Symbol.PRIVATE || lexer.token == Symbol.PUBLIC) {
			
			Symbol qualifier;
			switch (lexer.token) {
			case PRIVATE:
				lexer.nextToken();
				qualifier = Symbol.PRIVATE;
				break;
			case PUBLIC:
				lexer.nextToken();
				qualifier = Symbol.PUBLIC;
				break;
			default:
				error.show("private, or public expected");
				qualifier = Symbol.PUBLIC;
			}
			Type t = type();
			
			if ( lexer.token != Symbol.IDENT )
				error.show("Identifier expected");
			String name = lexer.getStringValue();
			lexer.nextToken();
			
			if ( lexer.token == Symbol.LEFTPAR ){
				m = methodDec(t, name, qualifier);
				if(qualifier==Symbol.PUBLIC){
					kra.getPublicMethodList().addElement(m);
				}else{
					kra.getPrivateMethodList().addElement(m);
				}
			}else if ( qualifier != Symbol.PRIVATE )
				error.show("Attempt to declare a public instance variable");
			else{
				//Declaração de variaveis
				value = new InstanceVariable(name, t, qualifier);
				
				if(symbolTable.putInLocal(name, value) == null){
					kra.getInstanceVariableList().addElement(value);
				}
				while (lexer.token == Symbol.COMMA) {
					lexer.nextToken();
					if ( lexer.token != Symbol.IDENT )
						error.show("Identifier expected");
					String variableName = lexer.getStringValue();
					kra.getInstanceVariableList().addElement(new InstanceVariable(variableName, t,qualifier));
					lexer.nextToken();
				}
				if ( lexer.token != Symbol.SEMICOLON )
					error.show(CompilerError.semicolon_expected);
				lexer.nextToken();
				
				//Fim da declaração
			}
		}
		if ( lexer.token != Symbol.RIGHTCURBRACKET )
			error.show("public/private or \"}\" expected");
		lexer.nextToken();
		return kra;

	}
	/*
	private InstanceVariable instanceVarDec(Type type, String name) {
		// InstVarDec ::= [ "static" ] "private" Type IdList ";"
		InstanceVariable inst = new InstanceVariable();
		while (lexer.token == Symbol.COMMA) {
			lexer.nextToken();
			if ( lexer.token != Symbol.IDENT )
				error.show("Identifier expected");
			String variableName = lexer.getStringValue();
			inst.addElement(new InstanceVariable(variableName, type));
			lexer.nextToken();
		}
		if ( lexer.token != Symbol.SEMICOLON )
			error.show(CompilerError.semicolon_expected);
		lexer.nextToken();
		return inst;
	}
	*/

	private Method methodDec(Type type, String name, Symbol qualifier) {
		/*
		 * MethodDec ::= Qualifier Return Id "("[ FormalParamDec ] ")" "{"
		 *                StatementList "}"
		 */
		Method m = new Method(name, type, qualifier);
	
		lexer.nextToken();
		if ( lexer.token != Symbol.RIGHTPAR ) {
			InstanceVariableList inst = formalParamDec();
			m.setVariableList(inst);
		}
		if ( lexer.token != Symbol.RIGHTPAR ) error.show(") expected");

		lexer.nextToken();
		if ( lexer.token != Symbol.LEFTCURBRACKET ) error.show("{ expected");

		lexer.nextToken();
		StatementList s = statementList();
		if ( lexer.token != Symbol.RIGHTCURBRACKET ) error.show("} expected");
		m.setStmList(s);
		lexer.nextToken();
		return m;

	}

	private void localDec() {
		// LocalDec ::= Type IdList ";"

		Type type = type();
		if ( lexer.token != Symbol.IDENT ) error.show("Identifier expected");
		Variable v = new Variable(lexer.getStringValue(), type);
		lexer.nextToken();
		while (lexer.token == Symbol.COMMA) {
			lexer.nextToken();
			if ( lexer.token != Symbol.IDENT )
				error.show("Identifier expected");
			v = new Variable(lexer.getStringValue(), type);
			lexer.nextToken();
		}
	}

	private InstanceVariableList formalParamDec() {
		// FormalParamDec ::= ParamDec { "," ParamDec }
		InstanceVariableList l = new InstanceVariableList();
		l.addElement(paramDec());
		while (lexer.token == Symbol.COMMA) {
			lexer.nextToken();
			l.addElement(paramDec());
		}
		return l;
	}

	private InstanceVariable paramDec() {
		// ParamDec ::= Type Id

		Type t = type();
		if ( lexer.token != Symbol.IDENT ) error.show("Identifier expected");
		String name = lexer.toString();
		InstanceVariable i = new InstanceVariable(name,t,null);
		lexer.nextToken();
		return i;
	}

	private Type type() {
		// Type ::= BasicType | Id
		Type result;

		switch (lexer.token) {
		case VOID:
			result = Type.voidType;
			break;
		case INT:
			result = Type.intType;
			break;
		case BOOLEAN:
			result = Type.booleanType;
			break;
		case STRING:
			result = Type.stringType;
			break;
		case IDENT:
			// # corrija: faça uma busca na TS para buscar a classe
			// IDENT deve ser uma classe.
			result = symbolTable.getInGlobal(lexer.token.toString());
			if(result == null){
				error.show("Type expected");
				result = Type.undefinedType;
			}
			break;
		default:
			error.show("Type expected");
			result = Type.undefinedType;
		}
		lexer.nextToken();
		return result;
	}

	private void compositeStatement() {

		lexer.nextToken();
		statementList();
		if ( lexer.token != Symbol.RIGHTCURBRACKET )
			error.show("} expected");
		else
			lexer.nextToken();
	}

	private StatementList statementList() {
		// CompStatement ::= "{" { Statement } "}"
		Symbol tk;
		ArrayList<Statement> s = new ArrayList<>();
		
		// statements always begin with an identifier, if, read, write, ...
		while ((tk = lexer.token) != Symbol.RIGHTCURBRACKET	&& tk != Symbol.ELSE){
			s.add(statement());
		}
		return new StatementList(s);
	}

	private Statement statement() {
		/*
		 * Statement ::= Assignment ``;'' | IfStat |WhileStat | MessageSend
		 *                ``;'' | ReturnStat ``;'' | ReadStat ``;'' | WriteStat ``;'' |
		 *               ``break'' ``;'' | ``;'' | CompStatement | LocalDec
		 */

		switch (lexer.token) {
		case THIS:
		case IDENT:
		case SUPER:
		case INT:
		case BOOLEAN:
		case STRING:
			assignExprLocalDec();
			break;
		case RETURN:
			return returnStatement();
			
		case READ:
			readStatement();
			break;
		case WRITE:
			writeStatement();
			break;
		case WRITELN:
			writelnStatement();
			break;
		case IF:
			ifStatement();
			break;
		case BREAK:
			breakStatement();
			break;
		case WHILE:
			return whileStatement();
		
		case SEMICOLON:
			nullStatement();
			break;
		case LEFTCURBRACKET:
			compositeStatement();
			break;
		default:
			error.show("Statement expected");
		}
		return null;
	}

	/*
	 * retorne true se 'name' é uma classe declarada anteriormente. É necessário
	 * fazer uma busca na tabela de símbolos para isto.
	 */
	private boolean isType(String name) {
		return this.symbolTable.getInGlobal(name) != null;
	}

	/*
	 * AssignExprLocalDec ::= Expression [ ``$=$'' Expression ] | LocalDec
	 */
	private Expr assignExprLocalDec() {

		if ( lexer.token == Symbol.INT || lexer.token == Symbol.BOOLEAN
				|| lexer.token == Symbol.STRING ||
				// token é uma classe declarada textualmente antes desta
				// instrução
				(lexer.token == Symbol.IDENT && isType(lexer.getStringValue())) ) {
			/*
			 * uma declaração de variável. 'lexer.token' é o tipo da variável
			 * 
			 * AssignExprLocalDec ::= Expression [ ``$=$'' Expression ] | LocalDec 
			 * LocalDec ::= Type IdList ``;''
			 */
			localDec();
		}
		else {
			/*
			 * AssignExprLocalDec ::= Expression [ ``$=$'' Expression ]
			 */
			expr();
			if ( lexer.token == Symbol.ASSIGN ) {
				lexer.nextToken();
				expr();
			}
		}
		return null;
	}

	private ExprList realParameters() {
		ExprList anExprList = null;

		if ( lexer.token != Symbol.LEFTPAR ) error.show("( expected");
		lexer.nextToken();
		if ( startExpr(lexer.token) ) anExprList = exprList();
		if ( lexer.token != Symbol.RIGHTPAR ) error.show(") expected");
		lexer.nextToken();
		return anExprList;
	}

	private Statement whileStatement() {

		lexer.nextToken();
		if ( lexer.token != Symbol.LEFTPAR ) 
			error.show("( expected");
		lexer.nextToken();
		Expr whilePart = expr();
		if ( lexer.token != Symbol.RIGHTPAR ) 
			error.show(") expected");
		lexer.nextToken();
		ArrayList<Statement> s = null;
		s.add(statement());
		StatementList st = new StatementList(s);
		return new WhileStatement(whilePart,st);
	}

	private void ifStatement() {

		lexer.nextToken();
		if ( lexer.token != Symbol.LEFTPAR ) error.show("( expected");
		lexer.nextToken();
		expr();
		if ( lexer.token != Symbol.RIGHTPAR ) error.show(") expected");
		lexer.nextToken();
		statement();
		if ( lexer.token == Symbol.ELSE ) {
			lexer.nextToken();
			statement();
		}
	}

	private Statement returnStatement() {

		lexer.nextToken();
		ReturnStatement r = new ReturnStatement(expr());
		if ( lexer.token != Symbol.SEMICOLON )
			error.show(CompilerError.semicolon_expected);
		lexer.nextToken();
		return r;
	}

	private void readStatement() {
		lexer.nextToken();
		if ( lexer.token != Symbol.LEFTPAR ) error.show("( expected");
		lexer.nextToken();
		while (true) {
			if ( lexer.token == Symbol.THIS ) {
				lexer.nextToken();
				if ( lexer.token != Symbol.DOT ) error.show(". expected");
				lexer.nextToken();
			}
			if ( lexer.token != Symbol.IDENT )
				error.show(CompilerError.ident_expected);

			String name = lexer.getStringValue();
			lexer.nextToken();
			if ( lexer.token == Symbol.COMMA )
				lexer.nextToken();
			else
				break;
		}

		if ( lexer.token != Symbol.RIGHTPAR ) error.show(") expected");
		lexer.nextToken();
		if ( lexer.token != Symbol.SEMICOLON )
			error.show(CompilerError.semicolon_expected);
		lexer.nextToken();
	}

	private void writeStatement() {

		lexer.nextToken();
		if ( lexer.token != Symbol.LEFTPAR ) error.show("( expected");
		lexer.nextToken();
		exprList();
		if ( lexer.token != Symbol.RIGHTPAR ) error.show(") expected");
		lexer.nextToken();
		if ( lexer.token != Symbol.SEMICOLON )
			error.show(CompilerError.semicolon_expected);
		lexer.nextToken();
	}

	private void writelnStatement() {

		lexer.nextToken();
		if ( lexer.token != Symbol.LEFTPAR ) error.show("( expected");
		lexer.nextToken();
		exprList();
		if ( lexer.token != Symbol.RIGHTPAR ) error.show(") expected");
		lexer.nextToken();
		if ( lexer.token != Symbol.SEMICOLON )
			error.show(CompilerError.semicolon_expected);
		lexer.nextToken();
	}

	private void breakStatement() {
		lexer.nextToken();
		if ( lexer.token != Symbol.SEMICOLON )
			error.show(CompilerError.semicolon_expected);
		lexer.nextToken();
	}

	private void nullStatement() {
		lexer.nextToken();
	}

	private ExprList exprList() {
		// ExpressionList ::= Expression { "," Expression }

		ExprList anExprList = new ExprList();
		anExprList.addElement(expr());
		while (lexer.token == Symbol.COMMA) {
			lexer.nextToken();
			anExprList.addElement(expr());
		}
		return anExprList;
	}

	private Expr expr() {

		Expr left = simpleExpr();
		Symbol op = lexer.token;
		if ( op == Symbol.EQ || op == Symbol.NEQ || op == Symbol.LE
				|| op == Symbol.LT || op == Symbol.GE || op == Symbol.GT ) {
			lexer.nextToken();
			Expr right = simpleExpr();
			left = new CompositeExpr(left, op, right);
		}
		return left;
	}

	private Expr simpleExpr() {
		Symbol op;

		Expr left = term();
		while ((op = lexer.token) == Symbol.MINUS || op == Symbol.PLUS
				|| op == Symbol.OR) {
			lexer.nextToken();
			Expr right = term();
			left = new CompositeExpr(left, op, right);
		}
		return left;
	}

	private Expr term() {
		Symbol op;

		Expr left = signalFactor();
		while ((op = lexer.token) == Symbol.DIV || op == Symbol.MULT
				|| op == Symbol.AND) {
			lexer.nextToken();
			Expr right = signalFactor();
			left = new CompositeExpr(left, op, right);
		}
		return left;
	}

	private Expr signalFactor() {
		Symbol op;
		if ( (op = lexer.token) == Symbol.PLUS || op == Symbol.MINUS ) {
			lexer.nextToken();
			return new SignalExpr(op, factor());
		}
		else
			return factor();
	}

	/*
	 * Factor ::= BasicValue | "(" Expression ")" | "!" Factor | "null" |
	 *      ObjectCreation | PrimaryExpr
	 * 
	 * BasicValue ::= IntValue | BooleanValue | StringValue 
	 * BooleanValue ::=  "true" | "false" 
	 * ObjectCreation ::= "new" Id "(" ")" 
	 * PrimaryExpr ::= "super" "." Id "(" [ ExpressionList ] ")"  | 
	 *                 Id  |
	 *                 Id "." Id | 
	 *                 Id "." Id "(" [ ExpressionList ] ")" |
	 *                 Id "." Id "." Id "(" [ ExpressionList ] ")" |
	 *                 "this" | 
	 *                 "this" "." Id | 
	 *                 "this" "." Id "(" [ ExpressionList ] ")"  | 
	 *                 "this" "." Id "." Id "(" [ ExpressionList ] ")"
	 */
	private Expr factor() {

		Expr e;
		ExprList exprList;
		String messageName, ident;

		switch (lexer.token) {
		// IntValue
		case NUMBER:
			return literalInt();
			// BooleanValue
		case TRUE:
			lexer.nextToken();
			return LiteralBoolean.True;
			// BooleanValue
		case FALSE:
			lexer.nextToken();
			return LiteralBoolean.False;
			// StringValue
		case LITERALSTRING:
			String literalString = lexer.getLiteralStringValue();
			lexer.nextToken();
			return new LiteralString(literalString);
			// "(" Expression ")" |
		case LEFTPAR:
			lexer.nextToken();
			e = expr();
			if ( lexer.token != Symbol.RIGHTPAR ) error.show(") expected");
			lexer.nextToken();
			return new ParenthesisExpr(e);

			// "!" Factor
		case NOT:
			lexer.nextToken();
			e = expr();
			return new UnaryExpr(e, Symbol.NOT);
			// "null"
		case NULL:
			lexer.nextToken();
			return new NullExpr();
			// ObjectCreation ::= "new" Id "(" ")"
		case NEW:
			lexer.nextToken();
			if ( lexer.token != Symbol.IDENT )
				error.show("Identifier expected");

			String className = lexer.getStringValue();
			/*
			 * // encontre a classe className in symbol table KraClass 
			 *      aClass = symbolTable.getInGlobal(className); 
			 *      if ( aClass == null ) ...
			 */

			lexer.nextToken();
			if ( lexer.token != Symbol.LEFTPAR ) error.show("( expected");
			lexer.nextToken();
			if ( lexer.token != Symbol.RIGHTPAR ) error.show(") expected");
			lexer.nextToken();
			/*
			 * return an object representing the creation of an object
			 */
			return null;
			/*
          	 * PrimaryExpr ::= "super" "." Id "(" [ ExpressionList ] ")"  | 
          	 *                 Id  |
          	 *                 Id "." Id | 
          	 *                 Id "." Id "(" [ ExpressionList ] ")" |
          	 *                 Id "." Id "." Id "(" [ ExpressionList ] ")" |
          	 *                 "this" | 
          	 *                 "this" "." Id | 
          	 *                 "this" "." Id "(" [ ExpressionList ] ")"  | 
          	 *                 "this" "." Id "." Id "(" [ ExpressionList ] ")"
			 */
		case SUPER:
			// "super" "." Id "(" [ ExpressionList ] ")"
			lexer.nextToken();
			if ( lexer.token != Symbol.DOT ) {
				error.show("'.' expected");
			}
			else
				lexer.nextToken();
			if ( lexer.token != Symbol.IDENT )
				error.show("Identifier expected");
			messageName = lexer.getStringValue();
			/*
			 * para fazer as conferências semânticas, procure por 'messageName'
			 * na superclasse/superclasse da superclasse etc
			 */
			lexer.nextToken();
			exprList = realParameters();
			break;
		case IDENT:
			/*
          	 * PrimaryExpr ::=  
          	 *                 Id  |
          	 *                 Id "." Id | 
          	 *                 Id "." Id "(" [ ExpressionList ] ")" |
          	 *                 Id "." Id "." Id "(" [ ExpressionList ] ")" |
			 */

			String firstId = lexer.getStringValue();
			lexer.nextToken();
			if ( lexer.token != Symbol.DOT ) {
				// Id
				// retorne um objeto da ASA que representa um identificador
				return null;
			}
			else { // Id "."
				lexer.nextToken(); // coma o "."
				if ( lexer.token != Symbol.IDENT ) {
					error.show("Identifier expected");
				}
				else {
					// Id "." Id
					lexer.nextToken();
					ident = lexer.getStringValue();
					if ( lexer.token == Symbol.DOT ) {
						// Id "." Id "." Id "(" [ ExpressionList ] ")"
						/*
						 * se o compilador permite variáveis estáticas, é possível
						 * ter esta opção, como
						 *     Clock.currentDay.setDay(12);
						 * Contudo, se variáveis estáticas não estiver nas especificações,
						 * sinalize um erro neste ponto.
						 */
						lexer.nextToken();
						if ( lexer.token != Symbol.IDENT )
							error.show("Identifier expected");
						messageName = lexer.getStringValue();
						lexer.nextToken();
						exprList = this.realParameters();

					}
					else if ( lexer.token == Symbol.LEFTPAR ) {
						// Id "." Id "(" [ ExpressionList ] ")"
						exprList = this.realParameters();
						/*
						 * para fazer as conferências semânticas, procure por
						 * método 'ident' na classe de 'firstId'
						 */
					}
					else {
						// retorne o objeto da ASA que representa Id "." Id
					}
				}
			}
			break;
		case THIS:
			/*
			 * Este 'case THIS:' trata os seguintes casos: 
          	 * PrimaryExpr ::= 
          	 *                 "this" | 
          	 *                 "this" "." Id | 
          	 *                 "this" "." Id "(" [ ExpressionList ] ")"  | 
          	 *                 "this" "." Id "." Id "(" [ ExpressionList ] ")"
			 */
			lexer.nextToken();
			if ( lexer.token != Symbol.DOT ) {
				// only 'this'
				// retorne um objeto da ASA que representa 'this'
				// confira se não estamos em um método estático
				return null;
			}
			else {
				lexer.nextToken();
				if ( lexer.token != Symbol.IDENT )
					error.show("Identifier expected");
				ident = lexer.getStringValue();
				lexer.nextToken();
				// já analisou "this" "." Id
				if ( lexer.token == Symbol.LEFTPAR ) {
					// "this" "." Id "(" [ ExpressionList ] ")"
					/*
					 * Confira se a classe corrente possui um método cujo nome é
					 * 'ident' e que pode tomar os parâmetros de ExpressionList
					 */
					exprList = this.realParameters();
				}
				else if ( lexer.token == Symbol.DOT ) {
					// "this" "." Id "." Id "(" [ ExpressionList ] ")"
					lexer.nextToken();
					if ( lexer.token != Symbol.IDENT )
						error.show("Identifier expected");
					lexer.nextToken();
					exprList = this.realParameters();
				}
				else {
					// retorne o objeto da ASA que representa "this" "." Id
					/*
					 * confira se a classe corrente realmente possui uma
					 * variável de instância 'ident'
					 */
					return null;
				}
			}
			break;
		default:
			error.show("Expression expected");
		}
		return null;
	}

	private LiteralInt literalInt() {

		LiteralInt e = null;

		// the number value is stored in lexer.getToken().value as an object of
		// Integer.
		// Method intValue returns that value as an value of type int.
		int value = lexer.getNumberValue();
		lexer.nextToken();
		return new LiteralInt(value);
	}

	private static boolean startExpr(Symbol token) {

		return token == Symbol.FALSE || token == Symbol.TRUE
				|| token == Symbol.NOT || token == Symbol.THIS
				|| token == Symbol.NUMBER || token == Symbol.SUPER
				|| token == Symbol.LEFTPAR || token == Symbol.NULL
				|| token == Symbol.IDENT || token == Symbol.LITERALSTRING;

	}

	private SymbolTable		symbolTable;
	private Lexer			lexer;
	private CompilerError	error;

}
