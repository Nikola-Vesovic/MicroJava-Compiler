package rs.ac.bg.etf.pp1;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;

public class CodeGenerator extends VisitorAdaptor {
	
	private int mainPc = 0;
	
	
	public int getMainPc() {
		return mainPc;
	}
	Logger log = Logger.getLogger(getClass());
	public void report_info(String message, SyntaxNode info) {
    	StringBuilder msg = new StringBuilder(message); 
    	int line = (info == null) ? 0 : info.getLine();
    	if (line != 0)
            msg.append (" na liniji ").append(line);
        log.info(msg.toString());
    }
	
	
	@Override
	public void visit(MethRetAndName_void methRetAndName_void) {
		methRetAndName_void.obj.setAdr(Code.pc);
		if(methRetAndName_void.getI1().equalsIgnoreCase("main"))
			this.mainPc = Code.pc;
		
		Code.put(Code.enter);
		Code.put(methRetAndName_void.obj.getLevel()); 
		Code.put(methRetAndName_void.obj.getLocalSymbols().size()); 
	}
	
	@Override
	public void visit(MethRetAndName_type methRetAndName_type) {
		methRetAndName_type.obj.setAdr(Code.pc);
		if(methRetAndName_type.getI2().equalsIgnoreCase("main"))
			this.mainPc = Code.pc;
		
		Code.put(Code.enter);
		Code.put(methRetAndName_type.obj.getLevel()); 
		Code.put(methRetAndName_type.obj.getLocalSymbols().size()); 
	}
	
	@Override
	public void visit(MethodDecl methodDecl) {
		
		if (methodDecl.getMethRetAndName() instanceof MethRetAndName_type) {	
			Code.put(Code.trap);
			Code.put(1);
		} else {
			Code.put(Code.exit);
			Code.put(Code.return_);
		}
		
	}
	
	/* STATEMENTS */
	
	/* ------------------------------- Designator statements ------------------------------- */
	
	@Override
	public void visit(DesignatorStatement_assign designatorStatement_assign) {
		Code.store(designatorStatement_assign.getDesignator().obj);
	}
	
	@Override
	public void visit(DesignatorStatement_meth designatorStatement_meth) {
		String fName = designatorStatement_meth.getDesignator().obj.getName();
		if (fName.equals("len")) {
				
			Code.put(Code.arraylength);
				
		} else if (!fName.equals("ord") && !fName.equals("chr")) {
			
			int offset = designatorStatement_meth.getDesignator().obj.getAdr() - Code.pc;
			Code.put(Code.call);
			Code.put2(offset);
			
			if(designatorStatement_meth.getDesignator().obj.getType() != Tab.noType)
				Code.put(Code.pop);
		}
	}
	
	
	@Override
	public void visit(DesignatorStatement_inc designatorStatement_inc) {
		if(designatorStatement_inc.getDesignator().obj.getKind() == Obj.Elem)
			Code.put(Code.dup2);
		Code.load(designatorStatement_inc.getDesignator().obj);
		Code.loadConst(1);
		Code.put(Code.add);
		Code.store(designatorStatement_inc.getDesignator().obj);
	}
	
	@Override
	public void visit(DesignatorStatement_dec designatorStatement_dec) {
		if(designatorStatement_dec.getDesignator().obj.getKind() == Obj.Elem)
			Code.put(Code.dup2);
		Code.load(designatorStatement_dec.getDesignator().obj);
		Code.loadConst(1);
		Code.put(Code.sub);
		Code.store(designatorStatement_dec.getDesignator().obj);
	}
	
	
	/* ------------------------------- Single statements ------------------------------- */
	
	@Override
	public void visit(SingleStatement_print1 singleStatement_print1) {
		Code.loadConst(0);
		if(singleStatement_print1.getExpr().struct.equals(Tab.charType))
			Code.put(Code.bprint);
		else
			Code.put(Code.print);
		
	}
	
	@Override
	public void visit(SingleStatement_print2 singleStatement_print2) {
		Code.loadConst(singleStatement_print2.getN2());
		if(singleStatement_print2.getExpr().struct.equals(Tab.charType))
			Code.put(Code.bprint);
		else
			Code.put(Code.print);
		
	}
	
	@Override
	public void visit(SingleStatement_return1 singleStatement_return1) {
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	@Override
	public void visit(SingleStatement_return2 singleStatement_return2) {
		Code.put(Code.exit);
		Code.put(Code.return_);
		
	}
	
	@Override
	public void visit(SingleStatement_read singleStatement_read) {
		if(singleStatement_read.getDesignator().obj.getType().equals(Tab.charType))
			Code.put(Code.bread);
		else
			Code.put(Code.read);
		Code.store(singleStatement_read.getDesignator().obj);
	}
	
	
	/* ------------------------------- Expr ------------------------------- */
	
	@Override
	public void visit(AddopTermList_add addopTermList_add) {
		if(addopTermList_add.getAddop() instanceof Addop_plus)
			Code.put(Code.add);
		else if(addopTermList_add.getAddop() instanceof Addop_minus)
			Code.put(Code.sub);
	}
	
	@Override
	public void visit(MulopFactorList_mul mulopFactorList_mul) {
		if(mulopFactorList_mul.getMulop() instanceof Mulop_mul)
			Code.put(Code.mul);
		else if(mulopFactorList_mul.getMulop() instanceof Mulop_div)
			Code.put(Code.div);
		else if(mulopFactorList_mul.getMulop() instanceof Mulop_mod)
			Code.put(Code.rem);
	}
	
	@Override
	public void visit(Factor factor) {
		if(factor.getUnary() instanceof Unary_m)
			Code.put(Code.neg);
	}
	
	/* ------------------------------- Factor ------------------------------- */
	
	@Override
	public void visit(FactorSub_num factorSub_num) {
		Code.loadConst(factorSub_num.getN1());
	}
	
	@Override
	public void visit(FactorSub_char factorSub_char) {
		Code.loadConst(factorSub_char.getC1());
	}
	
	@Override
	public void visit(FactorSub_bool factorSub_bool) {
		Code.loadConst(factorSub_bool.getB1());
	}
	
	@Override
	public void visit(FactorSub_des factorSub_des) {
		if (!(factorSub_des.getDesignator() instanceof Designator_length)) {
	        Code.load(factorSub_des.getDesignator().obj);
		}
	}
	
	@Override
	public void visit(FactorSub_new factorSub_new) {
		Code.put(Code.newarray);
		if(factorSub_new.getType().struct.equals(Tab.charType))
			Code.put(0);
		else
			Code.put(1);
	}
	
	@Override
	public void visit(FactorSub_meth factorSub_meth) {
		String fName = factorSub_meth.getDesignator().obj.getName();
		if (fName.equals("len")) {
			
			Code.put(Code.arraylength);
			
		} else if (!fName.equals("ord") && !fName.equals("chr")) {
			
			int offset = factorSub_meth.getDesignator().obj.getAdr() - Code.pc;
			Code.put(Code.call);
			Code.put2(offset);
		}
	}
	
	/* ------------------------------- Designator ------------------------------- */
	
	@Override
	public void visit(DesignatorArrayName designatorArrayName) {
		Code.load(designatorArrayName.obj);
	}
	
	@Override
	public void visit(Designator_length designator_length) {
	    Obj arrObj = designator_length.getDesignatorArrayLength().obj;
	    Obj lenObj = designator_length.obj;
	    
	    Code.load(arrObj);
	    Code.put(Code.arraylength);
	}
	
	/* ------------------------------- Condition ------------------------------- */

		
	private int returnRelOp(Relop relop) {
		if(relop instanceof Relop_eql)
			return Code.eq;
		else if(relop instanceof Relop_neql)
			return Code.ne;
		else if(relop instanceof Relop_lwr)
			return Code.lt;
		else if(relop instanceof Relop_leql)
			return Code.le;
		else if(relop instanceof Relop_grt)
			return Code.gt;
		else if(relop instanceof Relop_geql)
			return Code.ge;
		else
			return 0;
	}
	
	private Stack<Integer> skipCondFact = new Stack<>();
	private Stack<Integer> skipCondition = new Stack<>();
	private Stack<Integer> skipThen = new Stack<>();
	private Stack<Integer> skipElse = new Stack<>();
	
	@Override
	public void visit(CondFact_expr condFact_expr) {
		Code.loadConst(0);
		Code.putFalseJump(Code.ne, 0); 
		skipCondFact.push(Code.pc - 2);
	}
	
	@Override
	public void visit(CondFact_expr_r_expr condFact_expr_r_expr) {
		Code.putFalseJump(returnRelOp(condFact_expr_r_expr.getRelop()), 0); 
		skipCondFact.push(Code.pc - 2);
	}
	
	@Override
	public void visit(CondTerm condTerm) {
		Code.putJump(0);
		skipCondition.push(Code.pc - 2);
		while(!skipCondFact.empty())
			Code.fixup(skipCondFact.pop());
	}
	
	@Override
	public void visit(Condition condition) {
		Code.putJump(0);
		skipThen.push(Code.pc - 2);
		//THEN
		while(!skipCondition.empty())
			Code.fixup(skipCondition.pop());
	}
	
	@Override
	public void visit(ElseStmt_epsi elseStmt_epsi) {
		Code.fixup(skipThen.pop());
	}
	
	@Override
	public void visit(Else else_) {
		Code.putJump(0);
		skipElse.push(Code.pc - 2);
		Code.fixup(skipThen.pop());
	}
	
	@Override
	public void visit(ElseStmt_yes elseStmt_yes) {
		Code.fixup(skipElse.pop()); 
	}
	

	private Stack<Integer> stackForCond = new Stack<>();
	private Stack<Integer> stackForInc = new Stack<>();
	private Stack<Integer> stackForBody = new Stack<>();

	@Override
	public void visit(Semi1 semi1) {
		breakJump.push(new ArrayList<Integer>());
		continueJump.push(new ArrayList<Integer>());
	    stackForCond.push(Code.pc);
	}

	@Override
	public void visit(Semi2 semi2) {
	    Code.putJump(0);
	    stackForBody.push(Code.pc - 2);
	    
	    stackForInc.push(Code.pc);
	}

	@Override
	public void visit(BodyStart bodyStart) {
	    Code.putJump(stackForCond.pop());
	    
	    Code.fixup(stackForBody.pop());
	}

	@Override
	public void visit(SingleStatement_for singleStatement_for) {
		
		while(!continueJump.peek().isEmpty())
			Code.fixup(continueJump.peek().remove(0));
		continueJump.pop();
		
	    Code.putJump(stackForInc.pop());
	    
	    Code.fixup(skipThen.pop());
	    
	    while(!breakJump.peek().isEmpty())
			Code.fixup(breakJump.peek().remove(0));
		breakJump.pop();
	}
	
	@Override
	public void visit(ForStart forStart) {
		switchCnt.push(0);
	}
	
	private Stack<List<Integer>> breakJump = new Stack<>();
	private Stack<List<Integer>> continueJump = new Stack<>();
	
	@Override
	public void visit(SingleStatement_break singleStatement_break) {
		Code.putJump(0);
		breakJump.peek().add(Code.pc - 2);
	}
	
	@Override
	public void visit(SingleStatement_continue singleStatement_continue) {
		while(switchCnt.peek() != 0) {
			Code.put(Code.pop);
			switchCnt.push(switchCnt.pop()-1);
		}
		switchCnt.pop();
		
		Code.putJump(0);
		continueJump.peek().add(Code.pc - 2);
	}
	
	/*
	// 	WHILE

	private Stack<Integer> whileJump = new Stack<>();
	@Override
	public void visit(WhileStart whileStart) {
		whileJump.push(Code.pc);
	}

	@Override
	public void visit(WhileEnd whileEnd) {
		Code.putJump(whileJump.pop());
		Code.fixup(skipThen.pop());
	}
	
	*/
	
	//	CASE
	
	private Stack<List<Integer>> caseStack = new Stack<>();
	private Stack<List<Integer>> fallThroughStack = new Stack<>();
	private Stack<Integer> switchCnt = new Stack<>();
	
	@Override
	public void visit(SwitchStart switchStart) {
		breakJump.push(new ArrayList<>());	
		caseStack.push(new ArrayList<>());
		fallThroughStack.push(new ArrayList<>());
		switchCnt.push(switchCnt.pop()+1);
	}
	
	@Override
	public void visit(CaseNumber caseNumber) {
		if(!caseStack.peek().isEmpty())
			Code.fixup(caseStack.peek().remove(caseStack.peek().size() - 1));
		
		Code.put(Code.dup);
		Code.loadConst(caseNumber.getN1());
		Code.putFalseJump(Code.eq, 0);
		
		if(!fallThroughStack.peek().isEmpty())
			Code.fixup(fallThroughStack.peek().remove(fallThroughStack.peek().size() - 1));
		caseStack.peek().add(Code.pc - 2);
	}
	
	@Override
	public void visit(CaseEnd switchCase) {
		Code.put(Code.jmp);
		Code.put2(0);
		fallThroughStack.peek().add(Code.pc - 2);
	}
	
	@Override
	public void visit(SingleStatement_switch singleStatement_switch) {
		//default
		/*
		if (defaultAdr.peek() != -1 && !caseStack.empty()) {
			List<Integer> list = caseStack.pop();
			for(int adr : list) 
				Code.fixup(adr);
			Code.putJump(defaultAdr.pop());
		}
		
		*/
		while(!breakJump.peek().isEmpty())
	    	Code.fixup(breakJump.peek().remove(0));
		List<Integer> list = fallThroughStack.pop();
		for(int adr : list) 
			Code.fixup(adr);
		list = caseStack.pop();
		for(int adr : list) 
			Code.fixup(adr);
	    list = breakJump.pop();
		for(int adr : list) 
			Code.fixup(adr);
		
		Code.put(Code.pop);

	}
	
	/*
	// Goto
	
	private HashMap<String, Integer> labelJump = new HashMap<>();
	private Stack<Integer> waitForJump = new Stack<>();
	
	@Override
	public void visit(SingleStatement_label singleStatement_label) {
		while (!waitForJump.empty()) {
			Code.fixup(waitForJump.pop());
		}
		labelJump.put(singleStatement_label.getI1(), Code.pc);
	}
	
	@Override
	public void visit(SingleStatement_goto singleStatement_goto) {
		if (labelJump.containsKey(singleStatement_goto.getI1())) {
			Code.putJump(labelJump.get(singleStatement_goto.getI1()));
		} else {
			Code.putJump(0);
			waitForJump.push(Code.pc - 2);
		}
	}	

	*/
	
	// Ternarni
	
	@Override
	public void visit(ColonElse colonElse) {
		Code.putJump(0);
		skipElse.push(Code.pc - 2);
		Code.fixup(skipThen.pop());
	}
		
	@Override
	public void visit(Expr_ternary expr_ternary) {
		Code.fixup(skipElse.pop()); 
	}
	
}