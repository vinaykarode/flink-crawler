package com.scaleunlimited.flinkcrawler.webgraph;

import java.io.Serializable;
import java.util.Iterator;

@SuppressWarnings("serial")
public abstract class BaseWebGraph implements Serializable {

    public static BaseWebGraph EMPTY_GRAPH = new BaseWebGraph() {

        @Override
        public Iterator<String> getChildren(String parent) {
            throw new RuntimeException("Can't call getChildren on missing page");
        }

        @Override
        public boolean hasPage(String url) {
            return false;
        }
    };

    /**
     * Return an iterator that emits node names for all children of <parent>.
     * 
     * @param parent
     *            Name of parent node
     * @return iterator over children, or null if <parent> doesn't exist.
     */
    public abstract Iterator<String> getChildren(String parent);

    public abstract boolean hasPage(String url);

    public float getScore(String urlToFetch) {
        return 1.0f;
    }

}
