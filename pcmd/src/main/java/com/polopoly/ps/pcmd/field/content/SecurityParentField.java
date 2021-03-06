package com.polopoly.ps.pcmd.field.content;

import com.polopoly.cm.ContentId;
import com.polopoly.cm.client.CMException;
import com.polopoly.cm.client.ContentRead;
import com.polopoly.util.client.PolopolyContext;

public class SecurityParentField extends AbstractContentIdField {
    @Override
    protected ContentId getContentId(ContentRead content, PolopolyContext context) throws CMException {
        return content.getSecurityParentId();
    }
}
