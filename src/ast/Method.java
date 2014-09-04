package ast;

import lexer.Symbol;

public class Method {
	public Method( String name, Type type, Symbol qualifier ) {
        this.name = name;
        this.type = type;
        this.qualifier = qualifier;
    }
	public String getName() { return name; }

    public Type getType() {
        return type;
    }
    
    public Symbol getQual(){
    	return qualifier;
    }

    public InstanceVariableList getVariableList() {
		return variableList;
	}
	public void setVariableList(InstanceVariableList variableList) {
		this.variableList = variableList;
	}

	public StatementList getStmList() {
		return stmList;
	}
	public void setStmList(StatementList stmList) {
		this.stmList = stmList;
	}

	private String name;
    private Type type;
    private Symbol qualifier;
    private InstanceVariableList variableList;
    private StatementList stmList;
}
