package com.jetbrains.jetpad.vclang.typechecking.termination;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.core.definition.FunctionDefinition;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by user on 12/16/16.
 */
public class DefinitionCallGraph extends BaseCallGraph<Definition> {
    public Map<Definition, Set<RecursiveBehavior<Definition>>> myErrorInfo = new HashMap<>();

    protected String getLabel(Definition vertex) {
        return vertex.getName();
    }

    protected void formErrorMessage(Definition vertex, Set<RecursiveBehavior<Definition>> behavior) {
        myErrorInfo.put(vertex, behavior);
    }

    public DefinitionCallGraph() {}

    public DefinitionCallGraph(DefinitionCallGraph cg) {
        super(cg);
    }

    public void add(Definition def) {
        add(def, null);
    }

    public void add(Definition def, Set<Definition> cycle) {
        if (def instanceof FunctionDefinition)
            add(new CollectCallVisitor((FunctionDefinition) def, cycle).getResult()); else {
            throw new IllegalStateException();
        }
    }
}
