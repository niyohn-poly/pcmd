package com.polopoly.pcmd.util;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.polopoly.cm.ContentId;
import com.polopoly.cm.client.CMException;
import com.polopoly.cm.collections.ContentListRead;
import com.polopoly.util.collection.FetchingIterator;

public class ContentListIterator extends FetchingIterator<ContentId> implements Iterable<ContentId> {
    private static final Logger logger =
        Logger.getLogger(ContentListIterator.class.getName());

    private ContentListRead contentList;
    private int size;
    private int i;

    public ContentListIterator(ContentListRead contentList) {
        this.contentList = contentList;
        this.size = contentList.size();
    }

    @Override
    public ContentId fetch() {
        if (i < size) {
            try {
                return contentList.getEntry(i++).getReferredContentId();
            } catch (CMException e) {
                logger.log(Level.WARNING, "While fetching entry " + i + " in content list: " + e.getMessage(), e);

                return fetch();
            }
        }
        else {
            return null;
        }
    }

    @Override
    public void remove() {
    }

    public Iterator<ContentId> iterator() {
        return new ContentListIterator(contentList);
    }
}
