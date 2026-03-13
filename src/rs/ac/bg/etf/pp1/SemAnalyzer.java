package rs.ac.bg.etf.pp1;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

public class SemAnalyzer extends VisitorAdaptor {
	
	private boolean errorDetected = false;
	Logger log = Logger.getLogger(getClass());
	private Obj currentProgram;
	private Struct currentType;
	private Obj currentMethod;
	private Obj mainMethod;
	private Obj currentEnum;
	private boolean returnHappened;
	private int constant;
	private Struct constantType;
	
	private Struct boolType = Tab.find("bool").getType();
	private Integer enumVal;
	private int loopCnt = 0; 
	private boolean inCase = false;
	int nVars;
	private Stack<HashSet<Integer>> cases = new Stack<>();
	//private Stack<String> gotoStack = new Stack();
	

	
	/* ----------------------- LOG MESSAGES ----------------------- */
	public void report_error(String message, SyntaxNode info) {
    	errorDetected = true;
    	StringBuilder msg = new StringBuilder(message); 
    	int line = (info == null) ? 0 : info.getLine();
    	if (line != 0)
            msg.append (" na liniji ").append(line);
        log.error(msg.toString());
    }
	
	public void report_info(String message, SyntaxNode info) {
    	StringBuilder msg = new StringBuilder(message); 
    	int line = (info == null) ? 0 : info.getLine();
    	if (line != 0)
            msg.append (" na liniji ").append(line);
        log.info(msg.toString());
    }
	
	public boolean passed() {
		return !errorDetected;
	}
	
	private boolean compatibleWith(Struct src, Struct dst) {
		return src.compatibleWith(dst);
	}
	
	private boolean assignableTo(Struct src, Struct dst) {
		return src.assignableTo(dst);
	}
	
	/*------------------------------- SEMANTIC PASS -------------------------------*/
	
	@Override
	public void visit(ProgramName programName) {
		currentProgram = Tab.insert(Obj.Prog, programName.getI1(), Tab.noType);
		Tab.openScope();
	}
	
	@Override
	public void visit(Program program) {
		nVars = Tab.currentScope().getnVars();
		Tab.chainLocalSymbols(currentProgram);
		Tab.closeScope();
		currentProgram = null;
		
		if (mainMethod == null || mainMethod.getLevel() > 0) {
			report_error("Program nema adekvatnu main metodu", program);
		} 
	}
	
	/* ------------------------------- CONSTANT DECLARATIONS ----------------------------------- */
	@Override
	public void visit(ConstDecl constDecl) {
		Obj conObj = Tab.find(constDecl.getI1());
		if (conObj != Tab.noObj) {
			report_error("Dvostruka definicija konstante: " + constDecl.getI1(), constDecl);
		} else {
			
			if (constantType.assignableTo(currentType)) {
				Obj newObj = Tab.insert(Obj.Con, constDecl.getI1(), currentType);
				newObj.setAdr(constant);
			} else 
				report_error("Neadekvatna dodela konstanti: " + constDecl.getI1(), constDecl);
			
		}
		
	}
	
	@Override
	public void visit(Constant_num constant_num) {
		constant = constant_num.getN1();
		constantType = Tab.intType;
	}
	@Override
	public void visit(Constant_char constant_char) {
		constant = constant_char.getC1();
		constantType = Tab.charType;
	}
	@Override
	public void visit(Constant_bool constant_bool) {
		constant = constant_bool.getB1();
		constantType = boolType;
	}
	
	/* ------------------------------- VAR DECLARATIONS ----------------------------------- */
	
	//basic
	@Override
	public void visit(VarDecl_var varDecl_var) {
		Obj varObj = null;
		if (currentMethod == null) {
			varObj = Tab.find(varDecl_var.getI1());
		} else {
			varObj = Tab.currentScope().findSymbol(varDecl_var.getI1());
		}
		
		if (varObj == null || varObj == Tab.noObj) {
			varObj = Tab.insert(Obj.Var, varDecl_var.getI1(), currentType);

		} else {
			report_error("Dvostruka definicija promenljive: " + varDecl_var.getI1(), varDecl_var);
		}
	}
	
	//array
	@Override
	public void visit(VarDecl_array varDecl_array) {
		Obj varObj = null;
		if (currentMethod == null) {
			varObj = Tab.find(varDecl_array.getI1());
		} else {
			varObj = Tab.currentScope().findSymbol(varDecl_array.getI1());
		}
		
		if (varObj == null || varObj == Tab.noObj) {
			varObj = Tab.insert(Obj.Var, varDecl_array.getI1(), new Struct(Struct.Array, currentType));
		} else {
			report_error("Dvostruka definicija promenljive: " + varDecl_array.getI1(), varDecl_array);
		
		}
	}
	/* ------------------------------- ENUM DECLARATION ----------------------------------- */
	
	@Override
	public void visit(EnumName enumName) {
		currentEnum = Tab.find(enumName.getI1());
		if (currentEnum != Tab.noObj) {
			report_error("Dvostruka definicija enuma: " + enumName.getI1(), enumName);
		} else {
			currentEnum = Tab.insert(Obj.Type, enumName.getI1(), new Struct(Struct.Enum));
		}
		enumVal = 0;
		Tab.openScope();
	}
	
	@Override
	public void visit(EnumDeclList enumDeclList) {
		Tab.chainLocalSymbols(currentEnum);
		Tab.closeScope();
		currentEnum = null;
	}
	
	@Override
	public void visit(EnumDecl enumDecl) {
		Obj varObj = null;
		if (currentEnum == null) {
			report_error("Semanticka greska. [EnumDecl]", enumDecl);
		} else
			varObj = Tab.currentScope().findSymbol(enumDecl.getI1());
		
		if (varObj == null || varObj == Tab.noObj) {
			
			if (Tab.currentScope().getLocals() != null) {
				//check duplicate values, not just names
				for (Obj o : Tab.currentScope().getLocals().symbols()) {
					if (o.getAdr() == enumVal)
						report_error("Enum vrednost vec postoji:", enumDecl);
				}
			}
			
			varObj = Tab.insert(Obj.Con, enumDecl.getI1(), Tab.intType);
			varObj.setAdr(enumVal);
			enumVal++;
			

		} else {
			report_error("Dvostruka definicija enum parametra: " + enumDecl.getI1(), enumDecl);
		}
	}
	@Override
	public void visit(EnumAssignNumber_assign enumAssignNumber_assign) {
		enumVal = enumAssignNumber_assign.getN1();
	}
	
	
	/* ------------------------------- METHOD DECLARATION ----------------------------------- */
	
	@Override
	public void visit(MethRetAndName_void methRetAndName_void) {
		methRetAndName_void.obj = currentMethod = Tab.insert(Obj.Meth, methRetAndName_void.getI1(), Tab.noType);
		if (methRetAndName_void.getI1().equalsIgnoreCase("main")) {
			mainMethod = currentMethod;
		} 
		Tab.openScope();
	}
	@Override
	public void visit(MethRetAndName_type methRetAndName_type) {
		methRetAndName_type.obj = currentMethod = Tab.insert(Obj.Meth, methRetAndName_type.getI2(), currentType);
		Tab.openScope();
	}
	@Override
	public void visit(MethodDecl methodDecl) {
		/*
		if (!gotoStack.empty()) {
			for (String l : gotoStack) {
				boolean found = false;
					if (Tab.currentScope().getLocals() != null) {
					for (Obj o : Tab.currentScope().getLocals().symbols()) {
						if (o.getName().equals(l)) {
							found = true; break;
						}
					}
				}
				if (!found) {
					report_error("Labela na koju se skace ne postoji ", methodDecl);
					break;
				}
			}
			gotoStack.removeAllElements();;
		}
		*/

		Tab.chainLocalSymbols(currentMethod);
		Tab.closeScope();
		
		//return type je bio char, int, bool, a nije bilo returna na kraju
		if (currentMethod.getType() != Tab.noType && !returnHappened) {
			report_error("Ne postoji iskaz return unutar metode " + currentMethod.getName(), methodDecl);
		}
		returnHappened = false;
		currentMethod = null;
	}
	
	/* ------------------------------- FORMPAR DECLARATION ----------------------------------- */
	
	//basic
	@Override
	public void visit(FormPar_var formPar_var) {
		Obj varObj = null;
		if (currentMethod == null) {
			report_error("Semanticka greska. [FormPar_var]", formPar_var);
		} else
			varObj = Tab.currentScope().findSymbol(formPar_var.getI2());
		
		
		if (varObj == null || varObj == Tab.noObj) {
			varObj = Tab.insert(Obj.Var, formPar_var.getI2(), currentType);
			varObj.setFpPos(1);
			currentMethod.setLevel(currentMethod.getLevel()+1);

		} else {
			report_error("Dvostruka definicija formalnog parametra: " + formPar_var.getI2(), formPar_var);
		}
	}
	
	//array
	@Override
	public void visit(FormPar_array formPar_array) {
		Obj varObj = null;
		if (currentMethod == null) {
			report_error("Semanticka greska. [FormPar_var]", formPar_array);
		} else
			varObj = Tab.currentScope().findSymbol(formPar_array.getI2());
		
		
		if (varObj == null || varObj == Tab.noObj) {
			varObj = Tab.insert(Obj.Var, formPar_array.getI2(), new Struct(Struct.Array, currentType));
			varObj.setFpPos(1);
			currentMethod.setLevel(currentMethod.getLevel()+1);

		} else {
			report_error("Dvostruka definicija formalnog parametra: " + formPar_array.getI2(), formPar_array);
		}
	}
	
	/* ------------------------------- TYPE ----------------------------------- */
	
	@Override
	public void visit(Type type) {
		Obj typeObj = Tab.find(type.getI1());
		if (typeObj == Tab.noObj) {
			report_error("Nepostojeci tip podatka " + type.getI1(), type);
			type.struct = currentType = Tab.noType;
		}
		else if (typeObj.getKind() != Obj.Type) {
			report_error("Neadekvatan tip podatka: " + type.getI1(), type);
			type.struct = currentType = Tab.noType;
		}
		else {
			type.struct = currentType = typeObj.getType();
			//!!!!!!!!!
			if (typeObj.getType().getKind() == Struct.Enum) {
				type.struct = currentType = Tab.intType;
			}
		}
		
	}
	
	/* ----------------------------------------- CONTEXT CONDITIONS ----------------------------------------- */
	
	/* ------------------- Designator ----------------- */
	
	@Override
	public void visit(Designator_var designator_var) {
		Obj varObj = Tab.find(designator_var.getI1());
		if(varObj == Tab.noObj) {
			report_error("Pristup nedefinisanoj promenljivi: " + designator_var.getI1(), designator_var);
			designator_var.obj = Tab.noObj;
		}
		else if (varObj.getKind() != Obj.Var && varObj.getKind() != Obj.Con && varObj.getKind() != Obj.Meth) {
			report_error("Neadekvatna promenljiva: " + designator_var.getI1(), designator_var);
			designator_var.obj = Tab.noObj;
		}
		else {
			designator_var.obj = varObj;
			if (varObj.getFpPos() == 1) {
				report_info("Koriscenje formalnog parametra " + varObj.getName(), designator_var);
			} 
			else if (varObj.getKind() == Obj.Con) {
				report_info("Koriscenje simbolicke konstante " + varObj.getName(), designator_var);
			}
			else if (varObj.getLevel() == 0 && varObj.getKind() != Obj.Meth) {
				report_info("Koriscenje globalne promenljive " + varObj.getName(), designator_var);
			}
			
		}
	}
	@Override
	public void visit(DesignatorEnumName designatorEnumName) {
		Obj enumObj = Tab.find(designatorEnumName.getI1());
		if(enumObj == Tab.noObj) {
			report_error("Pristup nedefinisanoj promenljivi enuma: " + designatorEnumName.getI1(), designatorEnumName);
			designatorEnumName.obj = Tab.noObj;
		}
		else if (enumObj.getKind() != Obj.Type || enumObj.getType().getKind() != Struct.Enum) {
			report_error("Neadekvatna promenljiva enuma: " + designatorEnumName.getI1(), designatorEnumName);
			designatorEnumName.obj = Tab.noObj;
		}
		else {
			designatorEnumName.obj = enumObj;
		}
	}
	@Override
	public void visit(Designator_enum designator_enum) {
		Obj enumObj = designator_enum.getDesignatorEnumName().obj;
		if(enumObj == Tab.noObj) {
			designator_enum.obj = Tab.noObj;
		} 
		else {
			boolean found = false;
			for (Obj o: enumObj.getLocalSymbols()) {
				if (o.getName().equalsIgnoreCase(designator_enum.getI2())) {
					found = true;
					designator_enum.obj = o;
					break;
				}
			}
			if (!found) {
				report_error("Konstanta sa tim imenom ne postoji u tom enumu.", designator_enum);
				designator_enum.obj = Tab.noObj;
			}
		}
	}
	@Override
	public void visit(DesignatorArrayLength designatorArrayLength) {
		Obj arrObj = Tab.find(designatorArrayLength.getI1());
		if (arrObj == Tab.noObj || arrObj.getKind() != Obj.Var || arrObj.getType().getKind() != Struct.Array) {
			report_error("Length se moze odrediti samo za niz: " + designatorArrayLength.getI1(), designatorArrayLength);
			designatorArrayLength.obj = Tab.noObj;
		}
		else {
			designatorArrayLength.obj = arrObj;//new Obj(Obj.Con, arrObj.getName() + ".length", Tab.intType);
			if (arrObj.getFpPos() == 1) {
				report_info("Koriscenje formalnog parametra " + arrObj.getName(), designatorArrayLength);
			}
		}
	}
	@Override
	public void visit(Designator_length designator_length) {
		Obj lengthObj = designator_length.getDesignatorArrayLength().obj;
		if(lengthObj == Tab.noObj) {
			designator_length.obj = Tab.noObj;
		} 
		else {
			designator_length.obj = new Obj(Obj.Con, lengthObj.getName() + ".length", Tab.intType);//lengthObj;
		}
	}
	@Override
	public void visit(DesignatorArrayName designatorArrayName) {
		Obj arrObj = Tab.find(designatorArrayName.getI1());
		if(arrObj == Tab.noObj) {
			report_error("Pristup nedefinisanoj promenljivi niza: " + designatorArrayName.getI1(), designatorArrayName);
			designatorArrayName.obj = Tab.noObj;
		}
		else if (arrObj.getKind() != Obj.Var || arrObj.getType().getKind() != Struct.Array) {
			report_error("Neadekvatna promenljiva niza: " + designatorArrayName.getI1(), designatorArrayName);
			designatorArrayName.obj = Tab.noObj;
		}
		else {
			designatorArrayName.obj = arrObj;
			if (arrObj.getFpPos() == 1) {
				report_info("Koriscenje formalnog parametra " + arrObj.getName(), designatorArrayName);
			}
		}
	}
	
	/*
	public void visit(DesignatorMaxArray designatorMaxArray) {
		Obj arrObj = Tab.find(designatorMaxArray.getI1());
		if (arrObj == Tab.noObj || arrObj.getKind() != Obj.Var || arrObj.getType().getKind() != Struct.Array) {
			report_error("Length se moze odrediti samo za niz: " + designatorMaxArray.getI1(), designatorMaxArray);
			designatorMaxArray.obj = Tab.noObj;
		}
		else {
			designatorMaxArray.obj = arrObj;//new Obj(Obj.Con, arrObj.getName() + ".length", Tab.intType);
			if (arrObj.getFpPos() == 1) {
				report_info("Koriscenje formalnog parametra " + arrObj.getName(), designatorMaxArray);
			}
		}
	}
	@Override
	public void visit(Designator_max_array designator_max_array) {
		Obj lengthObj = designator_max_array.getDesignatorMaxArray().obj;
		if(lengthObj == Tab.noObj) {
			designator_max_array.obj = Tab.noObj;
		} 
		else {
			designator_max_array.obj = new Obj(Obj.Con, lengthObj.getName() + ".length", Tab.intType);//lengthObj;
		}
	}
	
	*/
	
	
	@Override
	public void visit(Designator_elem designator_elem) {
		Obj arrObj = designator_elem.getDesignatorArrayName().obj;
		if(arrObj == Tab.noObj) {
			designator_elem.obj = Tab.noObj;
		} 
		else if (!designator_elem.getExpr().struct.equals(Tab.intType)) {
			report_error("Indeksiranje sa ne int vrednosti", designator_elem);
			designator_elem.obj = Tab.noObj; 
		}
		else {
			designator_elem.obj = new Obj(Obj.Elem, arrObj.getName() + "[$]", arrObj.getType().getElemType());
			report_info("Pristup elementu niza: " + arrObj.getName() + "[Kind: " + designator_elem.obj.getKind() + "]", designator_elem);
		}
	}
	
	//FactorSub-----------------------------------------------------------------------------------------------
	@Override
	public void visit(FactorSub_des factorSub_des) {
		if (factorSub_des.getDesignator().obj != null)
			factorSub_des.struct = factorSub_des.getDesignator().obj.getType();
		else factorSub_des.struct = Tab.noType;
	}
	@Override
	public void visit(FactorSub_meth factorSub_meth) {
		Obj methObj = factorSub_meth.getDesignator().obj;
		
		if(methObj.getKind() != Obj.Meth) {
			report_error("Poziv neadekvatne metode: " + methObj.getName(), factorSub_meth);
			factorSub_meth.struct = Tab.noType;
		} else {
			factorSub_meth.struct = factorSub_meth.getDesignator().obj.getType();
			
		
			List<Struct> fpList = new ArrayList<>();
			
			for (Obj o: factorSub_meth.getDesignator().obj.getLocalSymbols()) {
				if (o.getKind() == Obj.Var && o.getLevel() == 1 && (o.getFpPos() == 1 || methObj.getName().equals("chr") || methObj.getName().equals("ord") || methObj.getName().equals("len"))) {
					
					fpList.add(o.getType());
				}
			}
			
			ActParCounter apc = new ActParCounter();
			factorSub_meth.getActParList().traverseBottomUp(apc);
			
			List<Struct> apList = apc.finalActParList; 
			
			try {
				if (fpList.size() != apList.size()) {
					throw new Exception("Greska velicina");
				}
				
				for (int i = 0; i < fpList.size(); i++) {
					Struct fps = fpList.get(i);
					Struct aps = apList.get(i);
					
					if (!aps.assignableTo(fps)) {
						throw new Exception("Greska tipovi");
					}
				}
				
				report_info("Poziv globalne metode " + methObj.getName(), factorSub_meth);
				
			} catch(Exception e) {
				report_error("[" + e.getMessage() + "] Nekompatibilnost parametara pri pozivu metode " + methObj.getName(), factorSub_meth);
			}
			
		}
		
	}
	
	/* ------------------- FactorSub ----------------- */
	
	@Override
	public void visit(FactorSub_num factorSub_num) {
		factorSub_num.struct = Tab.intType;
	}
	@Override
	public void visit(FactorSub_char factorSub_char) {
		factorSub_char.struct = Tab.charType;
	}
	@Override
	public void visit(FactorSub_bool factorSub_bool) {
		factorSub_bool.struct = boolType;
	}
	@Override
	public void visit(FactorSub_new factorSub_new) {
		if(!factorSub_new.getExpr().struct.equals(Tab.intType)) {
			report_error("Velicina niza nije int tipa.", factorSub_new);
			factorSub_new.struct = Tab.noType;
		}
		else 
			factorSub_new.struct = new Struct(Struct.Array, currentType);
	}
	@Override
	public void visit(FactorSub_expr factorSub_expr) {
		factorSub_expr.struct = factorSub_expr.getExpr().struct;
	}
	
	
	/* ------------------- Factor ----------------- */
	
	@Override
	public void visit(Factor factor) {
		if (factor.getUnary() instanceof Unary_m) {
			if (factor.getFactorSub().struct.equals(Tab.intType))
				factor.struct = Tab.intType;
			else {
				report_error("Negacija ne int vrednosti", factor);
				factor.struct = Tab.noType;
			}
				
		}
		else 
			factor.struct = factor.getFactorSub().struct;
	}
	
	/* ------------------- Expr ----------------- */
	
	@Override
	public void visit(MulopFactorList_factor mulopFactorList_factor) {
		mulopFactorList_factor.struct = mulopFactorList_factor.getFactor().struct;
	}
	@Override
	public void visit(MulopFactorList_mul mulopFactorList_mul) {
		Struct left = mulopFactorList_mul.getMulopFactorList().struct;
		Struct right = mulopFactorList_mul.getFactor().struct;
		
		if (left.equals(Tab.intType) && right.equals(Tab.intType)) {
			mulopFactorList_mul.struct = Tab.intType;
		} 
		else {
			report_error("Mulop operacija ne int vrednosti.", mulopFactorList_mul);
			mulopFactorList_mul.struct = Tab.noType;
		}
	}
	@Override
	public void visit(Term term) {
		term.struct = term.getMulopFactorList().struct;
	}
	@Override
	public void visit(AddopTermList_term addopTermList_term) {
		addopTermList_term.struct = addopTermList_term.getTerm().struct;
	}
	@Override
	public void visit(AddopTermList_add addopTermList_add) {
		Struct left = addopTermList_add.getAddopTermList().struct;
		Struct right = addopTermList_add.getTerm().struct;
		
		if (left.equals(Tab.intType) && right.equals(Tab.intType)) {
			addopTermList_add.struct = Tab.intType;
		} 
		else {
			report_error("Addop operacija ne int vrednosti.", addopTermList_add);
			addopTermList_add.struct = Tab.noType;
		}
	}
	@Override
	public void visit(ExprBasic exprBasic) {
		exprBasic.struct = exprBasic.getAddopTermList().struct;
	}
	@Override
	public void visit(Expr_exprBasic expr_exprBasic) {
		expr_exprBasic.struct = expr_exprBasic.getExprBasic().struct;
	}
	@Override
	public void visit(Expr_ternary expr_ternary) {
		if (!expr_ternary.getExpr().struct.equals(expr_ternary.getExpr1().struct)) {
			report_error("Obe vrednosti ternarnog operatora treba da budu istog tipa.", expr_ternary);
		} 
		expr_ternary.struct = expr_ternary.getExpr().struct;

	}
	
	/* ------------------- Designator Statement ----------------- */
	
	@Override
	public void visit(DesignatorStatement_assign designatorStatement_assign) {
		Obj desObj = designatorStatement_assign.getDesignator().obj;
		Struct dstType = desObj.getType();
		Struct srcType = designatorStatement_assign.getExpr().struct;
	

		if(desObj.getKind() != Obj.Var && desObj.getKind() != Obj.Elem) {
			report_error("Dodela u neadekvatnu promenljivu: " + desObj.getName(), designatorStatement_assign);
			
		}
		else if(!srcType.assignableTo(desObj.getType())) {
			report_error("Neadekvatna dodela vrednosti u promenljivu: " + desObj.getName(), designatorStatement_assign);
		}
	}
	@Override
	public void visit(DesignatorStatement_inc designatorStatement_inc) {
		Obj desObj = designatorStatement_inc.getDesignator().obj;
		if(desObj.getKind() != Obj.Var && desObj.getKind() != Obj.Elem) {
			report_error("Inkrement neadekvatne promenljive: " + desObj.getName(), designatorStatement_inc);
			
		}
		else if(!desObj.getType().equals(Tab.intType)) {
			report_error("Inkrement ne int promenljive: " + desObj.getName(), designatorStatement_inc);
		} 
	}


	
	@Override
	public void visit(DesignatorStatement_dec designatorStatement_dec) {
		Obj desObj = designatorStatement_dec.getDesignator().obj;
		if(desObj.getKind() != Obj.Var && desObj.getKind() != Obj.Elem) {
			report_error("Dekrement neadekvatne promenljive: " + desObj.getName(), designatorStatement_dec);
			
		}
		else if(!desObj.getType().equals(Tab.intType)) {
			report_error("Dekrement ne int promenljive: " + desObj.getName(), designatorStatement_dec);
		}
			
	}
	@Override
	public void visit(DesignatorStatement_meth designatorStatement_meth) {
		Obj methObj = designatorStatement_meth.getDesignator().obj;
		if(methObj.getKind() != Obj.Meth) {
			report_error("Poziv neadekvatne metode: " + methObj.getName(), designatorStatement_meth);
		} else {
			List<Struct> fpList = new ArrayList<>();
			
			for (Obj o: designatorStatement_meth.getDesignator().obj.getLocalSymbols()) {
				if (o.getKind() == Obj.Var && o.getLevel() == 1 && (o.getFpPos() == 1 || methObj.getName().equals("chr") || methObj.getName().equals("ord") || methObj.getName().equals("len"))) {
					fpList.add(o.getType());
				}
			}
			ActParCounter apc = new ActParCounter();
			designatorStatement_meth.getActParList().traverseBottomUp(apc);
			
			List<Struct> apList = apc.finalActParList; 
			
			try {
				if (fpList.size() != apList.size()) {
					throw new Exception("Greska velicina");
				}
				
				for (int i = 0; i < fpList.size(); i++) {
					Struct fps = fpList.get(i);
					Struct aps = apList.get(i);
					
					if (!aps.assignableTo(fps)) {
						throw new Exception("Greska tipovi");
					}
				}
				report_info("Poziv globalne metode " + methObj.getName(), designatorStatement_meth);
				
			} catch(Exception e) {
				report_error("[" + e.getMessage() + "] Nekompatibilnost parametara pri pozivu metode " + methObj.getName(), designatorStatement_meth);
			}
		}
			
	}
	
	
	/* ------------------- SingleStatement ----------------- */
	
	/* ------------------- READ ----------------- */
	
	@Override
	public void visit(SingleStatement_read singleStatement_read) {
		Obj desObj = singleStatement_read.getDesignator().obj;
		if(desObj.getKind() != Obj.Var && desObj.getKind() != Obj.Elem) {
			report_error("Read operacija neadekvatne promenljive: " + desObj.getName(), singleStatement_read);
			
		}
		else if(!desObj.getType().equals(Tab.intType) && !desObj.getType().equals(Tab.charType) && !desObj.getType().equals(boolType)) {
			report_error("Read ne int/char/bool promenljive: " + desObj.getName(), singleStatement_read);
		}
			
	}
	
	/* ------------------- PRINT ----------------- */
	
	@Override
	public void visit(SingleStatement_print1 singleStatement_print1) {
		Struct desType = singleStatement_print1.getExpr().struct;
		if(!desType.equals(Tab.intType) && !desType.equals(Tab.charType) && !desType.equals(boolType)) {
			report_error("Print operacija ne int/char/bool izraza.", singleStatement_print1);
		}	
	}
	@Override
	public void visit(SingleStatement_print2 singleStatement_print2) {
		Struct desType = singleStatement_print2.getExpr().struct;
		if(!desType.equals(Tab.intType) && !desType.equals(Tab.charType) && !desType.equals(boolType)) {
			report_error("Print operacija ne int/char/bool izraza.", singleStatement_print2);
		}	
	}
	
	/* ------------------- RETURN ----------------- */
	
	@Override
	public void visit(SingleStatement_return1 singleStatement_return1) {
		returnHappened = true;
		if (currentMethod.getType() != Tab.noType) {
			report_error("Desio se nevalidan return iskaz unutar metode: " + currentMethod.getName(), singleStatement_return1);
		}
	}
	@Override
	public void visit(SingleStatement_return2 singleStatement_return2) {
		returnHappened = true;
		if (currentMethod.getType() == Tab.noType) {
			report_error("Desio se nevalidan return iskaz unutar metode: " + currentMethod.getName(), singleStatement_return2);
		}
		
		
	/* ------------------- FOR, BREAK, CONTINUE ----------------- */	
		
	}
	@Override
	public void visit(ForStart forStart) {
		loopCnt++;
	}
	@Override
	public void visit(SingleStatement_for singleStatement_for) {
		loopCnt-- ;
	}
	@Override
	public void visit(SingleStatement_break singleStatement_break) {
		if(loopCnt == 0 && inCase == false) {
			report_error("Break naredba se nije desila u okviru for-a ili case-a.", singleStatement_break);
		}
	}
	@Override
	public void visit(SingleStatement_continue singleStatement_continue) {
		if(loopCnt == 0 && inCase == false) {
			report_error("Continue naredba se nije desila u okviru for-a ili case-a.", singleStatement_continue);
		}
	}
	@Override
	public void visit(CaseStart caseStart) {
		inCase = true;
	}
	@Override
	public void visit(SwichCase swichCase) {
		inCase = false;
	}
	
	@Override
	public void visit(CaseNumber caseNumber) {
		if(cases.peek().contains(caseNumber.getN1()))
			report_error("Case sa datom konstantom vec postoji: " + caseNumber.getN1(), caseNumber);
		else cases.peek().add(caseNumber.getN1());
	}
	@Override
	public void visit(SwitchStart switchStart) {
		cases.push(new HashSet<>());
	}
		
	@Override
	public void visit(SingleStatement_switch singleStatement_switch) {
		cases.pop();
	}

	
	/* ------------------- CONDITION ----------------- */
	
	/*
	@Override
	public void visit(SingleStatement_label singleStatement_label) {
		Tab.insert(Obj.Con, singleStatement_label.getI1(), Tab.noType);
	}

	
	@Override
	public void visit(SingleStatement_goto singleStatement_goto) {
		
		gotoStack.push(singleStatement_goto.getI1());
//		boolean found = false;
//		for (Obj o : Tab.currentScope().getLocals().symbols()) {
//			if (o.getName().equals(singleStatement_goto.getI1())) found = true;
//		}
//
//		if (!found) {
//			report_error("Labela na koju se skace ne postoji.", singleStatement_goto);
//		}
	}
	
	*/

	
	@Override
	public void visit(CondFact_expr condFact_expr) {
		if (!condFact_expr.getExprBasic().struct.equals(boolType)) {
			report_error("Logicki operand nije tipa bool.", condFact_expr);
			condFact_expr.struct = Tab.noType;
		} else {
			condFact_expr.struct = boolType;
		}
	}
	@Override
	public void visit(CondFact_expr_r_expr condFact_expr_r_expr) {
		Struct left = condFact_expr_r_expr.getExprBasic().struct;
		Struct right = condFact_expr_r_expr.getExprBasic1().struct;
		
		if (left.compatibleWith(right)) {
			if (left.isRefType() || right.isRefType()) {
				if (condFact_expr_r_expr.getRelop() instanceof Relop_eql || condFact_expr_r_expr.getRelop() instanceof Relop_neql) {
					condFact_expr_r_expr.struct = boolType;
				} else {
					report_error("Poredjenje ref tipova sa neadekvatnim relacionim operatorima", condFact_expr_r_expr);
					condFact_expr_r_expr.struct = Tab.noType;
				}
			} else {
				condFact_expr_r_expr.struct = boolType;
			}
			
		} else {
			report_error("Logicki operandi nisu kompatibilni", condFact_expr_r_expr);
			condFact_expr_r_expr.struct = Tab.noType;
		}
	}
	@Override
	public void visit(CondFactList_cf condFactList_cf) {
		condFactList_cf.struct = condFactList_cf.getCondFact().struct;
	}
	@Override
	public void visit(CondFactList_and condFactList_and) {
		Struct left = condFactList_and.getCondFactList().struct;
		Struct right = condFactList_and.getCondFact().struct;
		
		if (left.equals(boolType) && right.equals(boolType)) {
			condFactList_and.struct = boolType;
		} 
		else {
			report_error("And operacija ne bool vrednostima.", condFactList_and);
			condFactList_and.struct = Tab.noType;
		}
	}
	@Override
	public void visit(CondTerm condTerm) {
		condTerm.struct = condTerm.getCondFactList().struct;
	}
	@Override
	public void visit(CondTermList_ct condTermList_ct) {
		condTermList_ct.struct = condTermList_ct.getCondTerm().struct;
	}
	@Override
	public void visit(CondTermList_or condTermList_or) {
		Struct left = condTermList_or.getCondTermList().struct;
		Struct right = condTermList_or.getCondTerm().struct;
		
		if (left.equals(boolType) && right.equals(boolType)) {
			condTermList_or.struct = boolType;
		} 
		else {
			report_error("Or operacija ne bool vrednostima", condTermList_or);
			condTermList_or.struct = Tab.noType;
		}
	}
	@Override
	public void visit(Condition condition) {
		condition.struct = condition.getCondTermList().struct;
		if (!condition.struct.equals(boolType)) {
			report_error("Uslov nije tipa bool", condition);
			condition.struct = Tab.noType;
		} 
	}
	
	
	
}
