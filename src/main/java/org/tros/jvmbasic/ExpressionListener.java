/*
 * Copyright 2015-2016 Matthew Aguirre
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tros.jvmbasic;

import java.util.ArrayList;
import java.util.Stack;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.tros.jvmbasic.antlr.jvmBasicBaseListener;
import org.tros.jvmbasic.antlr.jvmBasicParser;
import org.tros.torgo.interpreter.InterpreterValue;
import org.tros.torgo.interpreter.Scope;
import org.tros.torgo.interpreter.types.BooleanType;
import org.tros.torgo.interpreter.types.NullType;
import org.tros.torgo.interpreter.types.NumberType;
import org.tros.torgo.interpreter.types.StringType;

/**
 * Evaluates expressions. Builds a stack/tree of expressions and evaluates them
 * As the tree walker visits/exists nodes in the parse tree. The scope is passed
 * in to allow for variable dereferencing.
 *
 * @author matta
 */
class ExpressionListener extends jvmBasicBaseListener {

    private final Scope scope;
    private final Stack<ArrayList<InterpreterValue>> value = new Stack<>();

    /**
     * Evaluate an expression as defined in the jvmBasic.g4 grammar.
     *
     * @param scope
     * @param ctx
     * @return
     */
    public static InterpreterValue evaluate(Scope scope, ParseTree ctx) {
        org.tros.jvmbasic.ExpressionListener el = new org.tros.jvmbasic.ExpressionListener(scope);
        ParseTreeWalker.DEFAULT.walk(el, ctx);
        return el.getValue();
    }

    /**
     * Hidden constructor, forces use of "evaluateDouble" method.
     *
     * @param scope
     */
    private ExpressionListener(Scope scope) {
        this.scope = scope;
        value.push(new ArrayList<InterpreterValue>());
    }

    private InterpreterValue mathExpression(InterpreterValue val1, InterpreterValue val2, String op) {
        double num1 = ((Number) val1.getValue()).doubleValue();
        double num2 = ((Number) val2.getValue()).doubleValue();
        if (null != op) {
            switch (op) {
                case "-":
                    num1 = num1 - num2;
                    break;
                case "+":
                    num1 = num1 + num2;
                    break;
                case "*":
                    num1 = num1 * num2;
                    break;
                case "%":
                    num1 = num1 % num2;
                    break;
                case "/":
                    num1 = num1 / num2;
                    break;
                case "\\":
                    num1 = (int) (num1 / num2);
                    break;
                case "^":
                    num1 = Math.pow(num1, num2);
                    break;
                default:
                    break;
            }
        }
        return new InterpreterValue(NumberType.INSTANCE, num1);
    }

    private InterpreterValue relOp(InterpreterValue val1, InterpreterValue val2, String op) {
        double num1 = ((Number) val1.getValue()).doubleValue();
        double num2 = ((Number) val2.getValue()).doubleValue();
        boolean ret = false;
        boolean success = false;
        if (null != op) {
            switch (op) {
                case "=>":  //GTE
                case ">=":  //GTE
                case ">: ": //GTE
                    success = true;
                    ret = num1 >= num2;
                    break;
                case "<=":  //LTE
                case "=<":  //LTE
                case "<: ": //LTE
                    success = true;
                    ret = num1 <= num2;
                    break;
                case "<":   //lt
                    success = true;
                    ret = num1 < num2;
                    break;
                case ">":   //gt
                    success = true;
                    ret = num1 > num2;
                    break;
                case "=":   //eq
                    success = true;
                    ret = num1 == num2;
                    break;
                case "<>":  //neq
                    success = true;
                    ret = num1 != num2;
                    break;
                default:
                    //fail...
                    break;
            }
        }
        if (success) {
            return new InterpreterValue(BooleanType.INSTANCE, ret);
        } else {
            return new InterpreterValue(NullType.INSTANCE, null);
        }
    }

    /**
     * TODO: add in a conversion scheme, (widening/narrowing)
     *
     * @param val1
     * @param val2
     * @param op
     * @return
     */
    private InterpreterValue boolOp(InterpreterValue val1, InterpreterValue val2, String op) {
        boolean num1;
        boolean num2;

        if (val1.getType().getClass() != BooleanType.class) {
            double val = ((Number) val1.getValue()).doubleValue();
            num1 = val != 0;
        } else {
            num1 = ((Boolean) val1.getValue());
        }
        if (val2.getType().getClass() != BooleanType.class) {
            double val = ((Number) val2.getValue()).doubleValue();
            num2 = val != 0;
        } else {
            num2 = ((Boolean) val2.getValue());
        }

        boolean ret = false;
        boolean success = false;
        if (null != op) {
            switch (op.toLowerCase()) {
                case "and":
                    success = true;
                    ret = num1 && num2;
                    break;
                case "or":  //OR
                    success = true;
                    ret = num1 || num2;
                    break;
                default:
                    //fail...
                    break;
            }
        }
        if (success) {
            return new InterpreterValue(BooleanType.INSTANCE, ret);
        } else {
            return new InterpreterValue(NullType.INSTANCE, null);
        }
    }

    @Override
    public void enterExpression(jvmBasicParser.ExpressionContext ctx) {
        value.push(new ArrayList<InterpreterValue>());
    }

    @Override
    public void exitExpression(jvmBasicParser.ExpressionContext ctx) {
        ArrayList<InterpreterValue> values = value.pop();
        for (int ii = 1; ii < ctx.getChildCount(); ii += 2) {
            values.add(0, boolOp(values.remove(0), values.remove(0), ctx.getChild(ii).getText()));
        }
        value.peek().add(values.get(0));
    }

    @Override
    public void enterNumber(jvmBasicParser.NumberContext ctx) {
        if (ctx.NUMBER() != null) {
            InterpreterValue v = new InterpreterValue(NumberType.INSTANCE, Integer.parseInt(ctx.getText()));
            value.peek().add(v);
        } else if (ctx.FLOAT() != null) {
            InterpreterValue v = new InterpreterValue(NumberType.INSTANCE, Float.parseFloat(ctx.getText()));
            value.peek().add(v);
        }
    }

    @Override
    public void enterRelationalExpression(jvmBasicParser.RelationalExpressionContext ctx) {
        value.push(new ArrayList<InterpreterValue>());
    }

    @Override
    public void exitRelationalExpression(jvmBasicParser.RelationalExpressionContext ctx) {
        ArrayList<InterpreterValue> values = value.pop();
        for (int ii = 1; ii < ctx.getChildCount(); ii += 2) {
            values.add(0, relOp(values.remove(0), values.remove(0), ctx.getChild(ii).getText()));
        }
        value.peek().add(values.get(0));
    }

    @Override
    public void enterAddingExpression(jvmBasicParser.AddingExpressionContext ctx) {
        value.push(new ArrayList<InterpreterValue>());
    }

    @Override
    public void exitAddingExpression(jvmBasicParser.AddingExpressionContext ctx) {
        ArrayList<InterpreterValue> values = value.pop();
        for (int ii = 1; ii < ctx.getChildCount(); ii += 2) {
            values.add(0, mathExpression(values.remove(0), values.remove(0), ctx.getChild(ii).getText()));
        }
        value.peek().add(values.get(0));
    }

    @Override
    public void enterMultiplyingExpression(jvmBasicParser.MultiplyingExpressionContext ctx) {
        value.push(new ArrayList<InterpreterValue>());
    }

    @Override
    public void exitMultiplyingExpression(jvmBasicParser.MultiplyingExpressionContext ctx) {
        ArrayList<InterpreterValue> values = value.pop();
        for (int ii = 1; ii < ctx.getChildCount(); ii += 2) {
            values.add(0, mathExpression(values.remove(0), values.remove(0), ctx.getChild(ii).getText()));
        }
        value.peek().add(values.get(0));
    }

    @Override
    public void enterExponentExpression(jvmBasicParser.ExponentExpressionContext ctx) {
        value.push(new ArrayList<InterpreterValue>());
    }

    @Override
    public void exitExponentExpression(jvmBasicParser.ExponentExpressionContext ctx) {
        ArrayList<InterpreterValue> values = value.pop();
        for (int ii = 1; ii < ctx.getChildCount(); ii += 2) {
            values.add(0, mathExpression(values.remove(0), values.remove(0), ctx.getChild(ii).getText()));
        }
        value.peek().add(values.get(0));
    }

    @Override
    public void exitSignExpression(jvmBasicParser.SignExpressionContext ctx) {
        boolean inverse = ctx.NOT() != null;
        int neg_count = ctx.MINUS().size();
        int pos_count = ctx.PLUS().size();

        /**
         * I don't know what the grammar is thinking, but you can't have
         * '--++--++-+-+++--+-+'. You can have 1 '+' to denote positive, but
         * that won't actually do anything. Multiple negatives '-----' will keep
         * flipping to the negative/positive value.
         */
        if (neg_count > 0 && pos_count > 0) {
            //error
        } else if (neg_count == 0 && pos_count > 1) {
            //error
        }

        ArrayList<InterpreterValue> peek = this.value.peek();
        int index = peek.size() - 1;
        if (peek.get(index).getType().equals(NumberType.INSTANCE)) {
            InterpreterValue val = peek.remove(index);
            double n = ((Number) val.getValue()).doubleValue();
            neg_count = (int) Math.pow(-1, neg_count);
            if (inverse) {
                peek.add(index, new InterpreterValue(BooleanType.INSTANCE, n != 0));
            } else {
                peek.add(index, new InterpreterValue(NumberType.INSTANCE, n *= neg_count));
            }
        } else if (inverse && peek.get(index).getType().equals(BooleanType.INSTANCE)) {
            InterpreterValue val = peek.remove(index);
            boolean n = ((Boolean) val.getValue());
            peek.add(index, new InterpreterValue(BooleanType.INSTANCE, !n));
        }
    }

    @Override
    public void enterFunc(jvmBasicParser.FuncContext ctx) {
        if (ctx.STRINGLITERAL() != null) {
            InterpreterValue v = new InterpreterValue(StringType.INSTANCE, ctx.getText());
            value.peek().add(v);
        } else {
        }
    }

    @Override
    public void exitFunc(jvmBasicParser.FuncContext ctx) {
    }

    public InterpreterValue getValue() {
        return value.peek().get(0);
    }
}
