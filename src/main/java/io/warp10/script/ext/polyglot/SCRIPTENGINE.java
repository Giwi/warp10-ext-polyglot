//
//   Copyright 2018  SenX S.A.S.
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
//

package io.warp10.script.ext.polyglot;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import io.warp10.script.NamedWarpScriptFunction;
import io.warp10.script.WarpScriptException;
import io.warp10.script.WarpScriptStack;
import io.warp10.script.WarpScriptStackFunction;

/**
 * Make available a scripting language to WarpScript via JSR 223
 * 
 * Expects on the stack one of the following configuration
 * 
 * 1: script
 * 
 * 2: script
 * 1: [ input symbols ]
 * 
 * 3: script
 * 2: [ input symbols ]
 * 1: [ output symbols ]
 */
public class SCRIPTENGINE extends NamedWarpScriptFunction implements WarpScriptStackFunction {
  
  private final String lang;
  
  public SCRIPTENGINE(String name, String lang) {
    super(name);
    this.lang = lang;
  }
   
  @Override
  public Object apply(WarpScriptStack stack) throws WarpScriptException {
    
    //
    // Extract parameters
    //
    
    Object top = stack.pop();
    
    Set<Object> input = new HashSet<Object>();
    Set<Object> output = new HashSet<Object>();
    
    String script = null;
    
    if (top instanceof List || top instanceof Set) {
      input.addAll((Collection) top);
      top = stack.pop();
    }
    
    if (top instanceof List || top instanceof Set) {
      output = input;
      input = new HashSet<Object>();
      input.addAll((Collection) top);
      top = stack.pop();
    }
    
    if (!(top instanceof String)) {
      throw new WarpScriptException(getName() + " expects a string on top of the stack or a string and one or two lists.");
    }
    
    script = top.toString();
    
    //
    // Create script engine
    //
        
    try {      
      final ScriptEngine engine = getEngine();

      //
      // Copy symbol table from stack
      //
      
      Bindings bindings = getBindings(engine);
      
      for (Entry<String,Object> entry: stack.getSymbolTable().entrySet()) {
        if (input.isEmpty() || input.contains(entry.getKey())) {
          bindings.put(entry.getKey(), entry.getValue());
        }
      }
      
      Object result = engine.eval(script, bindings);

      for (Entry<String,Object> entry: bindings.entrySet()) {
        if (output.isEmpty() || output.contains(entry.getKey())) {
          stack.getSymbolTable().put(entry.getKey(), entry.getValue());
        }
      }
      
      stack.push(result);
    } catch (ScriptException se) {
      se.printStackTrace();
      throw new WarpScriptException(se);
    } catch (Throwable t) {
      t.printStackTrace();
      throw new WarpScriptException(t);
    }
    
    return stack;
  }

  protected ScriptEngine getEngine() {
    return new ScriptEngineManager().getEngineByName(lang);
  }
  
  protected Bindings getBindings(ScriptEngine engine) {
    return engine.createBindings();
  }
}
