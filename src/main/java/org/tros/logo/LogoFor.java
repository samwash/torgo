/*
 * Copyright 2015 Matthew Aguirre
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
package org.tros.logo;

import java.text.MessageFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.antlr.v4.runtime.ParserRuleContext;
import org.tros.logo.antlr.logoParser;
import org.tros.torgo.InterpreterValue;
import org.tros.torgo.ReturnValue;
import org.tros.torgo.ReturnValue.ProcessResult;
import org.tros.torgo.Scope;
import org.tros.torgo.types.NumberType;

/**
 * Supports for (up-to and down-to) with and without a specified step value.
 *
 * @author matta
 */
class LogoFor extends LogoBlock {

    private static final Log logger = LogFactory.getLog(LogoFor.class);

    /**
     * How will we step.
     */
    enum ForType {

        UNDETERMINED,
        INCREASE,
        DECREASE
    }

    private ForType type = ForType.UNDETERMINED;

    /**
     * Constructor
     *
     * @param ctx
     */
    protected LogoFor(ParserRuleContext ctx) {
        super(ctx);
    }

    /**
     * Process the for loop.
     *
     * @param scope
     * @param canvas
     * @return
     */
    @Override
    public ReturnValue process(Scope scope) {
        logger.trace(MessageFormat.format("[{0}]: Line: {1}, Start: {2}, End: {3}", new Object[]{ctx.getClass().getName(), ctx.getStart().getLine(), ctx.getStart().getStartIndex(), ctx.getStart().getStopIndex()}));
        scope.push(this);
        listeners.fire().currStatement(this, scope);

        logoParser.ForeContext fore = (logoParser.ForeContext) ctx;
        String variable = fore.name().STRING().getText();

        double start = ((Number) ExpressionListener.evaluate(scope, fore.expression(0)).getValue()).doubleValue();
        double stop = ((Number) ExpressionListener.evaluate(scope, fore.expression(1)).getValue()).doubleValue();

        //Are we increasing/decreasing.
        //set the default step accordingly.
        double step;
        if (start > stop) {
            type = ForType.DECREASE;
            step = -1.0;
        } else if (stop > start) {
            type = ForType.INCREASE;
            step = 1.0;
        } else {
            type = ForType.UNDETERMINED;
            step = 0.0;
        }

        //if the step value is specified, evalutate.
        if (fore.expression().size() > 2) {
            step = ((Number) ExpressionListener.evaluate(scope, fore.expression(2)).getValue()).doubleValue();
        }

        //process and step
        boolean success = true;
        if (step != 0) {
            //not sure if this should be <=
            boolean doMore = type == ForType.INCREASE ? start < stop : stop < start;
            while (success && doMore) {
                scope.setNew(variable, new InterpreterValue(NumberType.Instance, start));
                success = success && super.process(scope).getResult() == ProcessResult.SUCCESS;

                switch (type) {
                    case INCREASE:
                        start += step;
                        break;
                    case DECREASE:
                        start -= step;
                        break;
                }
                //not sure if this should be <=
                doMore = type == ForType.INCREASE ? start < stop : stop < start;
            }
        }

        scope.pop();

        return ReturnValue.SUCCESS;
    }
}
