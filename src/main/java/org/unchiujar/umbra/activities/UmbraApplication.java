
package org.unchiujar.umbra.activities;

import org.unchiujar.umbra.backend.ExploredProvider;
import org.unchiujar.umbra.backend.VisitedAreaCache;

import android.app.Application;

public class UmbraApplication extends Application {
    private ExploredProvider cache;

    @Override
    public void onCreate() {
        cache = new VisitedAreaCache(this);
    };

    public ExploredProvider getCache() {
        return cache;
    }

    public void setCache(ExploredProvider cache) {
        this.cache = cache;
    }

}
