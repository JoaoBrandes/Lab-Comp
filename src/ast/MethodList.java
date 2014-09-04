package ast;

import java.util.ArrayList;
import java.util.Iterator;

public class MethodList {
	 public MethodList() {
	       methodList = new ArrayList<Method>();
	    }

	    public void addElement(Method method) {
	       methodList.add( method );
	    }

	    public Iterator<Method> elements() {
	    	return this.methodList.iterator();
	    }

	    public int getSize() {
	        return methodList.size();
	    }

	    private ArrayList<Method> methodList;

		public void genKra(PW pw) {
			// TODO Auto-generated method stub
			
			for(Method m:methodList){
				pw.printIdent("");
				pw.print(m.getQual().toString()+" ");
				pw.print(m.getType().getCname()+" ");
				pw.print(m.getName()+" ");
				pw.println("{");
				pw.printlnIdent("}");
			}
		}
}
