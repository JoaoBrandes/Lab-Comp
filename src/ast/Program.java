package ast;

import java.util.*;

import ast.PW;

public class Program {

	public Program(ArrayList<KraClass> classList) {
		this.classList = classList;
	}


	public void genC(PW pw) {
		
		for(KraClass k:classList){
			k.genC(pw);
		}
	}

	private ArrayList<KraClass> classList;
}