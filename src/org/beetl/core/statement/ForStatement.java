package org.beetl.core.statement;

import org.beetl.core.Context;
import org.beetl.core.IteratorStatus;

public class ForStatement extends Statement implements IGoto {
	Expression idNode;
	Expression exp;
	Statement forPart;
	Statement elseforPart;
	boolean hasGoto = false;

	public ForStatement(VarDefineNode idNode, Expression exp,
			Statement forPart, Statement elseforPart, Token token) {
		super(token);
		this.idNode = idNode;
		this.exp = exp;
		this.elseforPart = elseforPart;
		this.forPart = forPart;

	}

	public void execute(Context ctx) {
		// idNode 是其后设置的
		int varIndex = ((IVarIndex) idNode).getVarIndex();
		IteratorStatus it = IteratorStatus.getIteratorStatus(exp.evaluate(ctx),
				0);
		ctx.vars[varIndex + 1] = it;
		if (this.hasGoto) {

			while (it.hasNext()) {
				ctx.vars[varIndex] = it.next();
				forPart.execute(ctx);
				switch (ctx.gotoFlag) {
				case IGoto.NORMAL:
					break;
				case IGoto.CONTINUE:
					ctx.gotoFlag = IGoto.NORMAL;
					continue;
				case IGoto.RETURN:
					return;
				case IGoto.BREAK:
					ctx.gotoFlag = IGoto.NORMAL;
					return;
				}
			}

			if (!it.hasData()) {
				elseforPart.execute(ctx);
			}
			return;

		} else {
			while (it.hasNext()) {
				ctx.vars[varIndex] = it.next();
				forPart.execute(ctx);

			}
			if (!it.hasData()) {
				elseforPart.execute(ctx);
			}

		}

	}

	@Override
	public boolean hasGoto() {
		// TODO Auto-generated method stub
		return hasGoto;
	}

	@Override
	public void setGoto(boolean occour) {
		this.hasGoto = occour;

	}

	@Override
	public void infer(Type[] types, Object temp) {
		exp.infer(types, null);
		idNode.type = exp.getType().types[0];
		int index = ((IVarIndex) idNode).getVarIndex();
		types[index] = idNode.type;
		types[index + 1] = new Type(IteratorStatus.class, idNode.type.cls);
		forPart.infer(types, null);
		if (elseforPart != null) {
			elseforPart.infer(types, null);
		}

	}

}
