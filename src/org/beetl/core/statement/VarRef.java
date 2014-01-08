package org.beetl.core.statement;

import org.beetl.core.Context;
import org.beetl.core.exception.TempException;

public class VarRef extends Expression implements IVarIndex {

	public VarAttribute[] attributes;
	public Expression safe = null;
	int varIndex;

	public VarRef(VarAttribute[] attributes, Expression safe, Token token) {
		super(token);

		this.attributes = attributes;
		this.safe = safe;

	}

	@Override
	public Object evaluate(Context ctx) {

		Object value = ctx.vars[varIndex];
		if (value == null || value == Context.NOT_EXIST_OBJECT) {
			if (safe != null) {
				return safe.evaluate(ctx);
			} else {
				throw new TempException("未定义或者是空" + this.token.text);
			}
		}

		if (attributes.length == 0) {
			return value;
		}
		Object attrExp = null;
		for (VarAttribute attr : attributes) {

			value = attr.evaluate(ctx);

			if (value == null && safe != null) {
				return safe.evaluate(ctx);
			}
		}
		return value;

	}

	@Override
	public void setVarIndex(int index) {
		this.varIndex = index;

	}

	@Override
	public int getVarIndex() {
		return this.varIndex;
	}

	@Override
	public void infer(Type[] types, Object temp) {
		Type type = types[this.varIndex];
		Type lastType = type;
		for (VarAttribute attr : attributes) {
			attr.type = lastType;
			attr.infer(types, lastType);
			lastType = attr.type;

		}
		this.type = lastType;
	}

}
