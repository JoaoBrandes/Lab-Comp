package ast;

import java.util.*;

public class InstanceVariableList {

    public InstanceVariableList() {
       instanceVariableList = new ArrayList<InstanceVariable>();
    }

    public void addElement(InstanceVariable instanceVariable) {
       instanceVariableList.add( instanceVariable );
    }

    public Iterator<InstanceVariable> elements() {
    	return this.instanceVariableList.iterator();
    }

    public int getSize() {
        return instanceVariableList.size();
    }

    private ArrayList<InstanceVariable> instanceVariableList;

	public void genKra(PW pw) {
		for(InstanceVariable i:instanceVariableList){
			pw.printIdent("");
			if(i.getQualifier()!=null)
				pw.print(i.getQualifier().toString()+" ");
			pw.print(i.getType().getName()+" ");
			pw.print(i.getName());
			pw.println(";");
		}
	}

}
