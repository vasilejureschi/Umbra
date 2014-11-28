package org.unchiujar.umbra2.activities;

import android.app.Application;
import org.unchiujar.umbra2.backend.ExploredProvider;
import org.unchiujar.umbra2.backend.VisitedAreaCache;

public class UmbraApplication extends Application {
    private ExploredProvider cache;

    @Override
    public void onCreate() {
        cache = new VisitedAreaCache(this);
    }

    public ExploredProvider getCache() {
        return cache;
    }

    public void setCache(ExploredProvider cache) {
        this.cache = cache;
    }

}
