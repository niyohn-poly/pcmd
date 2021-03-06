package com.polopoly.ps.pcmd.tool;

import static com.polopoly.ps.pcmd.util.Plural.count;

import com.polopoly.cm.ContentId;
import com.polopoly.cm.LockInfo;
import com.polopoly.cm.client.CMException;
import com.polopoly.cm.client.CMRuntimeException;
import com.polopoly.cm.client.Content;
import com.polopoly.cm.client.ContentRead;
import com.polopoly.pcmd.tool.Tool;
import com.polopoly.ps.pcmd.FatalToolException;
import com.polopoly.ps.pcmd.field.content.AbstractContentIdField;
import com.polopoly.util.client.PolopolyContext;
import com.polopoly.util.collection.ContentIdToContentIterator;

public class UnlockTool implements Tool<UnlockParameters> {
    public UnlockParameters createParameters() {
        return new UnlockParameters();
    }

    public void execute(PolopolyContext context,
            UnlockParameters parameters) {
        if (parameters.isUnlockAll()) {
            unlockAll(context);
        }
        else {
            unlockSome(context, parameters);
        }
    }

    private void unlockSome(PolopolyContext context, UnlockParameters parameters) {
        ContentIdToContentIterator it =
            new ContentIdToContentIterator(context, parameters.getContentIds(), parameters.isStopOnException());


        while (it.hasNext()) {
            ContentRead content = it.next();

            try {
                if (content.getLockInfo() != null) {
                    ((Content) content).forcedUnlock();

                    System.out.println(AbstractContentIdField.get(content.getContentId(), context));
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

    private void unlockAll(PolopolyContext context) {
        System.err.println("Unlocking all locked content.");

//        SearchExpression searchExpr =
//            DBSearchUtil.getLockedContentExpr(ContentId.UNDEFINED_MAJOR, null);

        try {
            LockInfo[] allLocks = context.getPolicyCMServer().findAllLocks();
            
//            ContentId[] locked =
//                context.getPolicyCMServer().findContentIdsBySearchExpression(searchExpr);

            System.out.println(count(allLocks.length, "object") + " are locked.");

            for (int i = 0; i < allLocks.length;i++) {
                ContentId id = allLocks[i].getLocked();
                unlock(id, context);
            }
        } catch (CMException e) {
            throw new FatalToolException(
                    "Finding all locked content failed: " + e.getMessage(), e);
        }
    }

    public void unlock(ContentId id, PolopolyContext context) {
        try {
            //Unlock the latest version since latest committed might not yet exist
            Content content = (Content)
                context.getPolicyCMServer().getContent(id.getLatestVersionId());

            content.forcedUnlock();

            System.out.println(AbstractContentIdField.get(id, context));
        } catch (CMException e){
            System.err.println(AbstractContentIdField.get(id, context) + ": " + e);
        }
    }

    public String getHelp() {
        return "Forces an unlock of the specified content objects in case they were locked. " +
    		"Not specifying any objects unlocks all locked content.";
    }
}
