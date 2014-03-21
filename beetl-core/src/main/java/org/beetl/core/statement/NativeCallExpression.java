package org.beetl.core.statement;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import org.beetl.core.Context;
import org.beetl.core.InferContext;
import org.beetl.core.exception.TempException;
import org.beetl.core.statement.nat.ClassNode;
import org.beetl.core.statement.nat.InstanceNode;
import org.beetl.core.statement.nat.NativeArrayNode;
import org.beetl.core.statement.nat.NativeAtrributeNode;
import org.beetl.core.statement.nat.NativeMethodNode;
import org.beetl.core.statement.nat.NativeNode;
import org.beetl.core.util.MethodMatchConf;
import org.beetl.core.util.ObjectUtil;

/**
 * @xxx.xx()[0].xxx
 * @author joelli
 *
 */
public class NativeCallExpression extends Expression
{

	InstanceNode insNode;
	ClassNode clsNode;
	NativeNode[] chain;

	public NativeCallExpression(InstanceNode insNode, NativeNode[] chain, GrammarToken token)
	{
		super(token);
		this.insNode = insNode;
		this.chain = chain;
	}

	public NativeCallExpression(ClassNode clsNode, NativeNode[] chain, GrammarToken token)
	{
		super(token);
		//可以做某些优化，如提前得到final 属性

		this.clsNode = clsNode;
		this.chain = chain;
	}

	public Object evaluate(Context ctx)
	{
		Class targetCls = null;
		Object targetObj = null;
		if (insNode != null)
		{
			targetObj = insNode.ref.evaluate(ctx);
			//todo check nullpointer
			targetCls = targetObj.getClass();
		}
		else
		{
			targetCls = ctx.gt.loadClassBySimpleName(this.clsNode.cls);

		}

		for (NativeNode node : chain)
		{
			if (node instanceof NativeAtrributeNode)
			{
				String attr = ((NativeAtrributeNode) node).attribute;
				try
				{
					Field f = targetCls.getField(attr);
					targetObj = f.get(targetObj);
					targetCls = f.getType();

				}
				catch (SecurityException e)
				{
					throw new TempException("can not access filed" + attr);
				}
				catch (NoSuchFieldException e)
				{
					throw new TempException(" no such filed" + attr);
				}
				catch (IllegalArgumentException e)
				{
					throw new TempException(" can not get attribute" + attr);
				}
				catch (IllegalAccessException e)
				{
					throw new TempException(" can not access attribute" + attr);
				}

			}
			else if (node instanceof NativeArrayNode)
			{
				if (!targetCls.isArray())
				{
					throw new TempException(" must be array");
				}
				Expression exp = ((NativeArrayNode) node).exp;
				Object value = exp.evaluate(ctx);
				if (value instanceof Number)
				{
					int index = ((Number) value).intValue();
					targetObj = ((Object[]) targetObj)[index];
					if (targetObj != null)
					{
						targetCls = targetObj.getClass();
					}
					else
					{
						//todo or component of array
						targetCls = null;
					}
				}
				else
				{
					throw new TempException(" array index mustbe number");
				}
			}
			else if (node instanceof NativeMethodNode)
			{
				NativeMethodNode methodNode = (NativeMethodNode) node;
				String method = methodNode.method;
				Expression[] expList = methodNode.paras;
				//use 
				Object[] args = expList.length == 0 ? ObjectUtil.EMPTY_OBJECT_ARRAY : new Object[expList.length];

				for (int i = 0; i < expList.length; i++)
				{
					args[i] = expList[i].evaluate(ctx);

				}

				try
				{
					if (!ctx.gt.getNativeSecurity().permit(ctx.template.program.id, targetCls, targetObj, method))
					{
						throw new TempException("不允许调用Native方法" + method + " of " + targetCls);
					}
					if (targetObj != null)
					{
						targetObj = ObjectUtil.invoke(targetObj, method, args);
					}
					else
					{
						targetObj = ObjectUtil.invokeStatic(targetCls, method, args);
					}

					if (targetObj != null)
					{
						targetCls = targetObj.getClass();
					}
					else
					{
						targetCls = null;
					}

				}
				catch (SecurityException e)
				{
					throw new TempException(" can not get method" + e.getMessage());
				}

				catch (IllegalArgumentException e)
				{
					throw new TempException(" illegal parameter for  method" + e.getMessage());
				}
				catch (IllegalAccessException e)
				{
					throw new TempException(" illegal access  method" + e.getMessage());
				}
				catch (InvocationTargetException e)
				{
					throw new TempException("native call message" + e.getTargetException().getMessage());
				}
			}

		}
		return targetObj;
	}

	@Override
	public void infer(InferContext inferCtx)
	{

		Type type = null;
		if (insNode != null)
		{
			insNode.ref.infer(inferCtx);
			type = insNode.ref.type.copy();
		}
		else
		{
			Class cls = inferCtx.gt.loadClassBySimpleName(this.clsNode.cls);
			type = new Type(cls);

		}

		for (NativeNode node : chain)
		{
			if (type.cls == Object.class)
			{
				this.type = type;
				// do not infer since it's object
				return;
			}
			if (node instanceof NativeAtrributeNode)
			{
				String attr = ((NativeAtrributeNode) node).attribute;
				try
				{
					Field f = type.cls.getField(attr);
					Class c = f.getType();
					type.cls = c;
				}
				catch (SecurityException e)
				{
					throw new TempException("can not access filed" + attr);
				}
				catch (NoSuchFieldException e)
				{
					throw new TempException(" no such filed" + attr);
				}

			}
			else if (node instanceof NativeArrayNode)
			{
				if (!type.cls.isArray())
				{
					throw new TempException(" must be array");
				}
				type.cls = type.cls.getComponentType();
			}
			else if (node instanceof NativeMethodNode)
			{
				NativeMethodNode methodNode = (NativeMethodNode) node;
				String method = methodNode.method;
				Expression[] expList = methodNode.paras;

				Class[] argTypes = expList.length == 0 ? ObjectUtil.EMPTY_CLASS_ARRAY : new Class[expList.length];

				for (int i = 0; i < expList.length; i++)
				{
					expList[i].infer(inferCtx);
					argTypes[i] = expList[i].type.cls;

				}

				try
				{
					MethodMatchConf conf = ObjectUtil.findMethod(type.cls, method, argTypes);
					if (conf == null)
					{
						throw new TempException(" can not get method " + method);
					}
					else
					{
						type.cls = conf.method.getReturnType();
					}

				}
				catch (SecurityException e)
				{
					throw new TempException(" can not get method" + e.getMessage());
				}

			}

		}
		this.type = type;

	}

}
