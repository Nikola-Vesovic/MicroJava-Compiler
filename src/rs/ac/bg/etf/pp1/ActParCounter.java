package rs.ac.bg.etf.pp1;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import rs.ac.bg.etf.pp1.ast.VisitorAdaptor;
import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.concepts.Struct;

public class ActParCounter extends VisitorAdaptor {
	
	List<Struct> finalActParList;
	Stack<List<Struct>> actParLists = new Stack<>();
	
	@Override
	public void visit(ActParListStart actParListStart) {
		actParLists.push(new ArrayList<>());
	}
	@Override
	public void visit(ActPar actPar) {
		actParLists.peek().add(actPar.getExpr().struct);
	}
	@Override
	public void visit(ActParList_rec actParList_rec) {
		finalActParList = actParLists.pop();
	}
	@Override
	public void visit(ActParList_epsi actParList_epsi) {
		finalActParList = actParLists.pop();
	}
	
}
