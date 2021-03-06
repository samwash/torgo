/*
 * Copyright 2015-2017 Matthew Aguirre
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
package org.tros.torgo.interpreter;

import java.util.Collection;
import org.antlr.v4.runtime.ParserRuleContext;
import org.tros.utils.HaltListener;

/**
 * Abstract representation of code to execute.
 *
 * @author matta
 */
public interface CodeBlock extends InterpreterType, HaltListener {

    /**
     * Add a command to the list.
     *
     * @param command
     */
    void addCommand(CodeBlock command);

    /**
     * Add a collection of commands to the list.
     *
     * @param commands
     */
    void addCommand(Collection<CodeBlock> commands);

    /**
     * Add an interpreter listener.
     *
     * @param listener
     */
    void addInterpreterListener(InterpreterListener listener);

    /**
     * Get the commands to interpret.
     *
     * @return
     */
    CodeBlock[] getCommands();

    /**
     * Is the current block halted.
     *
     * @return true if halted, false if the monitor is null or the monitor is
     * not halted.
     */
    boolean isHalted();

    /**
     * Process the statement(s).
     *
     * @param scope
     * @return true if we should continue, false otherwise
     */
    ReturnValue process(Scope scope);

    /**
     * Add listener to this object.
     *
     * @param listener
     */
    void removeInterpreterListener(InterpreterListener listener);

    /**
     * Check to see if this code block defines a function.
     *
     * @param name
     * @return
     */
    boolean hasFunction(String name);

    /**
     * Get a function if it is defined.
     *
     * @param name
     * @return
     */
    CodeFunction getFunction(String name);

    /**
     * Add a function to this code block.
     *
     * @param function
     */
    void addFunction(CodeFunction function);

    /**
     * Get the local context of this ANTLR generated parse tree stub.
     *
     * @return
     */
    ParserRuleContext getParserRuleContext();

    /**
     * Check to see if there is a variable in the block.
     *
     * @param name
     * @return
     */
    boolean hasVariable(String name);

    /**
     * Set the value of the variable in the block.
     *
     * @param name
     * @param value
     */
    void setVariable(String name, InterpreterValue value);

    /**
     * Get the value of a variable in the block.
     *
     * @param name
     * @return
     */
    InterpreterValue getVariable(String name);

    /**
     * Get the names of local variables.
     *
     * @return
     */
    Collection<String> localVariables();

    /**
     * Get the lexical parent.
     *
     * @return
     */
    CodeBlock getParent();
}
