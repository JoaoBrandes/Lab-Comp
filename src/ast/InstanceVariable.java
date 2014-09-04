package ast;

import lexer.Symbol;

public class InstanceVariable extends Variable {

    public InstanceVariable( String name, Type type, Symbol qualifier ) {
        super(name, type);
        this.qualifier = qualifier;
    }
      
    public Symbol getQualifier() {
		return qualifier;
	}


	private Symbol qualifier;

}