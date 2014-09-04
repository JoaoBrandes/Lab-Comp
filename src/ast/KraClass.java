package ast;
/*
 * Krakatoa Class
 */
public class KraClass extends Type {
	
   public KraClass( String name ) {
      super(name);
      
   }
   
   public String getCname() {
      return getName();
   }
   
   private String name;
   private KraClass superclass;
   private InstanceVariableList instanceVariableList = new InstanceVariableList();
   private MethodList publicMethodList = new MethodList(), privateMethodList = new MethodList();
   // métodos públicos get e set para obter e iniciar as variáveis acima,
   // entre outros métodos
   
   public void genC(PW pw){
	   pw.print("class "+getName()+" ");
	   if(superclass!=null)
		   pw.print("extends "+superclass.getName()+" ");
	   
	   pw.println("{");
	   pw.add();
	   if(instanceVariableList!=null)
		   instanceVariableList.genKra(pw);
	   if(publicMethodList!=null)
		   publicMethodList.genKra(pw);
	   if(privateMethodList!=null)
		   privateMethodList.genKra(pw);
	   pw.sub();
	   pw.println("}");
   }
   
public String getName() {
	return super.getName();
}

public void setName(String name) {
	this.name = name;
}

public KraClass getSuperclass() {
	return superclass;
}

public void setSuperclass(KraClass superclass) {
	this.superclass = superclass;
}

public InstanceVariableList getInstanceVariableList() {
	return instanceVariableList;
}

public void setInstanceVariableList(InstanceVariableList instanceVariableList) {
	this.instanceVariableList = instanceVariableList;
}

public MethodList getPrivateMethodList() {
	return privateMethodList;
}

public void setPrivateMethodList(MethodList privateMethodList) {
	this.privateMethodList = privateMethodList;
}

public MethodList getPublicMethodList() {
	return publicMethodList;
}

public void setPublicMethodList(MethodList publicMethodList) {
	this.publicMethodList = publicMethodList;
}


}
