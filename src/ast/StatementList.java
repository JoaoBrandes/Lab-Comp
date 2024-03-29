/*
    Universidade Federal de São Carlos - Campus Sorocaba
    Compiladores: Trabalho 2
    Professora: Tiemi Christine Sakata

    Alunos:
        Arthur Pessoa de Souza
        João Eduardo Brandes Luiz
*/

package ast;

import java.util.*;
public class StatementList {

    public StatementList(ArrayList<Statement> v) {
        this.v = v;
    }
    
    public void genC( PW pw ) {

      for( Statement s : v )
          s.genC(pw);
    }
    
    private ArrayList<Statement> v;
}
    