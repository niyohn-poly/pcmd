package com.polopoly.pcmd.tool;

import com.polopoly.cm.ExternalContentId;
import com.polopoly.cm.VersionedContentId;
import com.polopoly.cm.client.CMException;
import com.polopoly.cm.client.CMRuntimeException;
import com.polopoly.cm.client.ContentRead;
import com.polopoly.cm.client.WorkflowAware;
import com.polopoly.pcmd.argument.ContentIdListParameters;
import com.polopoly.pcmd.field.content.AbstractContentIdField;
import com.polopoly.pcmd.field.content.ContentRefField;
import com.polopoly.pcmd.parser.ContentFieldListParser;
import com.polopoly.pcmd.util.ContentIdToContentIterator;

public class InspectTool implements Tool<ContentIdListParameters> {
    public ContentIdListParameters createParameters() {
        return new ContentIdListParameters();
    }

    public void execute(PolopolyContext context,
            ContentIdListParameters parameters) {
        ContentIdToContentIterator it =
            new ContentIdToContentIterator(context, parameters.getContentIds(), parameters.isStopOnException());

        while (it.hasNext()) {
            ContentRead content = it.next();

            try {
                ExternalContentId externalId = content.getExternalId();

                if (externalId != null) {
                    System.out.println(ContentFieldListParser.ID + ContentFieldListParser.PREFIX_FIELD_SEPARATOR +
                            externalId.getExternalId());
                    System.out.println(ContentFieldListParser.NUMERICAL_ID + ContentFieldListParser.PREFIX_FIELD_SEPARATOR +
                            content.getContentId().getContentIdString());
                }
                else {
                    System.out.println(ContentFieldListParser.ID + ContentFieldListParser.PREFIX_FIELD_SEPARATOR +
                            content.getContentId().getContentIdString());
                }

                if (content instanceof WorkflowAware) {
                    VersionedContentId workflowId = ((WorkflowAware) content).getWorkflowId();

                    if (workflowId != null) {
                        System.out.println(ContentFieldListParser.WORKFLOW + ContentFieldListParser.PREFIX_FIELD_SEPARATOR +
                                AbstractContentIdField.get(workflowId, context));
                    }
                }

                String[] groups = content.getComponentGroupNames();

                for (String group : groups) {
                    String[] names = content.getComponentNames(group);

                    for (String name : names) {
                        String value = content.getComponent(group, name);

                        System.out.println(
                                ContentFieldListParser.COMPONENT + ContentFieldListParser.PREFIX_FIELD_SEPARATOR +
                                group + ':' + name + '=' + value);
                    }

                }

                groups = content.getContentReferenceGroupNames();

                for (String group : groups) {
                    String[] names = content.getContentReferenceNames(group);

                    for (String name : names) {
                        System.out.println(
                                ContentFieldListParser.CONTENT_REF + ContentFieldListParser.PREFIX_FIELD_SEPARATOR +
                                group + ':' + name + '=' + new ContentRefField(group, name).get(content, context));
                    }

                }
            } catch (CMException e) {
                if (parameters.isStopOnException()) {
                    throw new CMRuntimeException(e);
                }
                else {
                    System.err.println(content.getContentId().getContentIdString() + ": " + e);
                }
            }
        }

        it.printInfo(System.err);
    }

    public String getHelp() {
        return "Prints the components, content references and meta data of the specified objects.";
    }
}
