package com.polopoly.ps.pcmd.tool;

import static com.polopoly.util.policy.Util.util;

import com.polopoly.pcmd.tool.Tool;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;

import com.polopoly.cm.ContentId;
import com.polopoly.cm.client.CMException;
import com.polopoly.cm.client.CMRuntimeException;
import com.polopoly.cm.client.InputTemplate;
import com.polopoly.cm.policy.Policy;
import com.polopoly.ps.pcmd.FatalToolException;
import com.polopoly.ps.pcmd.field.content.AbstractContentIdField;
import com.polopoly.util.client.PolopolyContext;
import com.polopoly.util.collection.AbstractContentIdIterator;
import com.polopoly.util.collection.ContentIdToPolicyIterator;
import com.polopoly.util.exception.PolicyCreateException;
import com.polopoly.util.exception.PolicyGetException;
import com.polopoly.util.exception.PolicyModificationException;
import com.polopoly.util.policy.PolicyModification;

public class GroovyTool implements Tool<GroovyParameters> {
    private boolean doBreak = false;

    public GroovyParameters createParameters() {
        return new GroovyParameters();
    }

    public void execute(final PolopolyContext context, final GroovyParameters parameters) {
        final Binding binding = new Binding();
        final GroovyShell shell = new GroovyShell(getClass().getClassLoader(), binding);

        binding.setVariable("cmServer", context.getPolicyCMServer());
        binding.setVariable("userServer", context.getUserServer());
        binding.setVariable("context", context);
        binding.setVariable("binding", binding);

        createUtilityMethods(shell);

        AbstractContentIdIterator<Policy> it = null;

        boolean isCreate = parameters.getCreate() > 0;

        if (isCreate) {
            it = getIteratorOfCreatedObjects(context, parameters);
        }
        else {
            it = getIteratorOfSpecifiedObjects(context, parameters);
        }

        int count = 0;

        if (!it.hasNext()) {
            execute(null, shell, binding, context, parameters);
        }
        else {
            while (it.hasNext()) {
                binding.setVariable("count", count);

                Policy policy = it.next();

                if (parameters.isModify() || isCreate) {
                    try {
                        util(policy).modify(new PolicyModification<Policy>() {
                            public void modify(Policy newVersion) throws CMException {
                                execute(newVersion, shell, binding, context, parameters);
                            }}, Policy.class, !isCreate);
                    } catch (PolicyModificationException e) {
                        handleModificationException(e, policy, parameters);
                    }
                }
                else {
                    execute(policy, shell, binding, context, parameters);
                }

                if (doBreak) {
                    break;
                }

                count++;
            }
        }

        it.printInfo(System.err);
    }

    private void handleModificationException(PolicyModificationException e,
            Policy policy, final GroovyParameters parameters) {
        String errorString = "While running Groovy code for " + policy.getContentId().getContentId().getContentIdString() + ": " + e;

        if (parameters.isStopOnException()) {
            throw new CMRuntimeException(errorString, e);
        }
        else {
            System.err.println(errorString);
        }
    }

    private ContentIdToPolicyIterator getIteratorOfSpecifiedObjects(
            final PolopolyContext context, final GroovyParameters parameters) {
        return new ContentIdToPolicyIterator(context, parameters.getContentIds(), parameters.isStopOnException());
    }

    private AbstractContentIdIterator<Policy> getIteratorOfCreatedObjects(
            final PolopolyContext context, final GroovyParameters parameters) {
        final InputTemplate inputTemplate = getInputTemplate(context, parameters);

        try {
            return new AbstractContentIdIterator<Policy>(context, null, parameters.isStopOnException()) {
                int major = getMajor(context, inputTemplate);
                private int left = parameters.getCreate();

                @Override
                protected Policy fetch() {
                    if (left == 0) {
                        return null;
                    }

                    left--;

                    try {
                        return context.createPolicy(major, parameters.getInputTemplate());
                    } catch (PolicyCreateException e) {
                        throw new FatalToolException("While creating policy: " + e.getMessage(), e);
                    }
                }};
        } catch (CMException e) {
            throw new FatalToolException("Could not fetch major of input template " + parameters.getInputTemplate().getContentIdString() + " was unknown.");
        }
    }

    private int getMajor(final PolopolyContext context,
            InputTemplate inputTemplate) throws CMException {
        String majorString = inputTemplate.getComponent("polopoly.Client", "major");

        final int major;

        major = context.getPolicyCMServer().getMajorByName(majorString);
        return major;
    }

    private InputTemplate getInputTemplate(final PolopolyContext context,
            final GroovyParameters parameters) {
        InputTemplate inputTemplate = null;

        try {
            inputTemplate = context.getPolicy(
                    parameters.getInputTemplate(), InputTemplate.class);
        } catch (PolicyGetException e) {
            throw new FatalToolException(
                "Could not use specified input template \"" + parameters.getInputTemplate() + "\" of input template " + parameters.getInputTemplate().getContentIdString() + ": " + e);
        }
        return inputTemplate;
    }

    private void createUtilityMethods(final GroovyShell shell) {
        shell.evaluate("com.polopoly.cm.policy.Policy.metaClass.propertyMissing = { String field -> " +
                    "childPolicy = delegate.getChildPolicy(field); " +
        		"if (childPolicy instanceof com.polopoly.cm.app.policy.SingleValued) {" +
        		"  return childPolicy.value" +
        		"}" +
        		"else if (childPolicy instanceof com.polopoly.cm.client.ContentListAware) {" +
        		"  return childPolicy.contentList" +
        		"}" +
        		"else {" +
        		"  return childPolicy" +
        		"}}");

        shell.evaluate("com.polopoly.cm.policy.Policy.metaClass.propertyMissing = { String field, value -> " +
                "childPolicy = delegate.getChildPolicy(field); " +
                "childPolicy.setValue(value)" +
                "}");

        shell.evaluate("com.polopoly.cm.collections.ContentList.metaClass.getAt = { Number index -> " +
                " delegate.getEntry(index).referredContentId " +
                "}");

        shell.evaluate("com.polopoly.cm.collections.ContentListRead.metaClass.iterator = { -> " +
                "return new com.polopoly.util.collection.ContentListIterator(delegate) }");
    }

    private void execute(Policy policy, GroovyShell shell, Binding binding,
            PolopolyContext context, GroovyParameters parameters) {
        binding.setVariable("policy", policy);

        Object value = shell.evaluate(parameters.getGroovy());

        if (value instanceof ContentId) {
            value = AbstractContentIdField.get((ContentId) value, context);
        }

        if (!parameters.isQuiet()) {
            if (policy != null) {
                System.out.print(AbstractContentIdField.get(policy.getContentId(), context) + ": ");
            }

            System.out.println(value);
        }

        if (value instanceof Boolean && !((Boolean) value).booleanValue()) {
            doBreak = true;
        }
    }

    public String getHelp() {
        return "Executes the specified Groovy statements for each content ID specified. " +
            "Variables are: policy - the current policy, cmServer - the PolicyCMServer, userServer - the UserServer.";
    }

}
