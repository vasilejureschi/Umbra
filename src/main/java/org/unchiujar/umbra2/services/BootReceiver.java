package org.unchiujar.umbra2.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unchiujar.umbra2.activities.FogOfExplore;

public class BootReceiver extends BroadcastReceiver {

    private static final Logger LOGGER = LoggerFactory.getLogger(BootReceiver.class);

    @Override
    public void onReceive(Context context, Intent intent) {
        LOGGER.info("Umbra boot receiver called {}", intent);
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // check if need to start
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            boolean startAtBoot = prefs.getBoolean("org.unchiujar.umbra.settings.boot_start", false);
            LOGGER.debug("Starting at boot {}", startAtBoot);
            if (!startAtBoot) {
                return;
            }
            Intent serviceIntent = new Intent(FogOfExplore.SERVICE_INTENT_NAME);
            context.startService(serviceIntent);
        }
    }
}
