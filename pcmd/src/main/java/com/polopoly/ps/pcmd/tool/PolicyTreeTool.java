package com.polopoly.ps.pcmd.tool;

import static com.polopoly.util.policy.Util.util;

import java.util.List;

import com.polopoly.cm.policy.Policy;
import com.polopoly.pcmd.tool.Tool;
import com.polopoly.ps.pcmd.field.policy.PolicyField;
import com.polopoly.util.client.PolopolyContext;
import com.polopoly.util.collection.ContentIdToPolicyIterator;

public class PolicyTreeTool implements Tool<PolicyTreeParameters> {
    private static final int TAB_SIZE = 2;
    private List<PolicyField> fieldList;
    private String delimiter;

    public PolicyTreeParameters createParameters() {
        return new PolicyTreeParameters();
    }

    public void execute(PolopolyContext context, PolicyTreeParameters parameters) {
        fieldList = parameters.getFieldList();
        delimiter = parameters.getDelimiter();

        ContentIdToPolicyIterator it =
            new ContentIdToPolicyIterator(context, parameters.getContentIds(), parameters.isStopOnException());

        StringBuffer line = new StringBuffer(100);

        while (it.hasNext()) {
            line.setLength(0);

            printLevel(0, it.next(), context);

            System.out.println(line);
        }

        it.printInfo(System.err);
    }

    private StringBuffer line = new StringBuffer(100);

    private void printLevel(int level, Policy policy, PolopolyContext context) {
        line.setLength(0);

        int tab = level * TAB_SIZE;

        for (int j = 0; j < tab; j++) {
            System.out.print(' ');
        }

        boolean first = true;

        for (PolicyField field : fieldList) {
            if (!first) {
                line.append(delimiter);
            }
            else {
                first = false;
            }

            line.append(field.get(policy, context));
        }

        System.out.println(line);

        for (Policy childPolicy : util(policy)) {
            printLevel(level+1, childPolicy, context);
        }
    }

    public String getHelp() {
        return "Prints the policy tree hierarchy.";
    }

}
