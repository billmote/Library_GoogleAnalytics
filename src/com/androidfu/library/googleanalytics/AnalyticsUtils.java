package com.androidfu.library.googleanalytics;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

/**
 * Helper singleton class for the Google Analytics tracking library.
 */
public class AnalyticsUtils {

    private static final String TAG = AnalyticsUtils.class.getSimpleName();
    private static final String FIRST_RUN_KEY = "firstRun";
    private static final int NETWORK_TIMEOUT = 300;
    private static final int VISITOR_SCOPE = 1;
    private static boolean _analyticsEnabled = true;
    private static AnalyticsUtils _instance;
    private static String _uaCode;
    private GoogleAnalyticsTracker _tracker;

    /**
     * Call this from your application class before using trackPage() or
     * trackEvent()
     * 
     * @param uaCode
     *            GoogleAnalytics key
     */
    public static void configure(Application application, String uaCode) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "configure()");
        }
        _uaCode = uaCode;
        if (_instance == null) {
            _instance = new AnalyticsUtils(application);
        }
    }

    /**
     * Analytics are enabled by default, but setting them is still the safest bet.
     * 
     * @param enabled
     */
    public void setAnalyticsEnabled(boolean enabled) {
        _analyticsEnabled = enabled;
    }

    /**
     * Returns the global {@link AnalyticsUtils} singleton object, creating one
     * if necessary.
     */
    public static AnalyticsUtils getInstance() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "getInstance()");
        }
        if (_instance == null) {
            throw new ClassCastException("You must call configure() first.");
        }
        if (!_analyticsEnabled) {
            return _emptyAnalyticsUtils;
        }
        return _instance;
    }

    /**
     * Our primary constructor.
     * 
     * @param application
     */
    @SuppressWarnings("deprecation")
    private AnalyticsUtils(Application application) {
        if (application == null) {
            // This should only occur for the empty AnalyticsUtils object.
            return;
        }
        _tracker = GoogleAnalyticsTracker.getInstance();
        // Unfortunately this needs to be synchronous.
        _tracker.start(_uaCode, NETWORK_TIMEOUT, application);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(application);
        final boolean firstRun = prefs.getBoolean(FIRST_RUN_KEY, true);
        if (firstRun) {
            String apiLevel = Build.VERSION.SDK;
            String model = Build.MODEL;
            _tracker.setCustomVar(1, "apiLevel", apiLevel, VISITOR_SCOPE);
            _tracker.setCustomVar(2, "model", model, VISITOR_SCOPE);
            prefs.edit().putBoolean(FIRST_RUN_KEY, false).commit();
        }
    }

    /**
     * Track an event.
     * 
     * @param category
     * @param action
     * @param label
     * @param value
     */
    public void trackEvent(final String category, final String action, final String label, final int value) {
        // We wrap the call in an AsyncTask since the Google Analytics library
        // writes to disk on its calling thread.
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    _tracker.trackEvent(category, action, label, value);
                } catch (Exception e) {
                    // We don't want to crash if there's an Analytics library
                    // exception.
                    Log.e(TAG, "Analytics trackEvent error: " + category + " / " + action + " / " + label + " / " + value, e);
                }
                return null;
            }
        }.execute();
    }

    /**
     * Track a page view.
     * 
     * @param path
     */
    public void trackPageView(final String path) {
        // We wrap the call in an AsyncTask since the Google Analytics library
        // writes to disk on its calling thread.
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    _tracker.trackPageView(path);
                } catch (Exception e) {
                    // We don't want to crash if there's an Analytics library
                    // exception.
                    Log.e(TAG, "Analytics trackPageView error: " + path, e);
                }
                return null;
            }
        }.execute();
    }

    /**
     * Empty instance for use when Analytics is disabled or there was no Context
     * available.
     */
    private static AnalyticsUtils _emptyAnalyticsUtils = new AnalyticsUtils(null) {

        @Override
        public void trackEvent(String category, String action, String label, int value) {
        }

        @Override
        public void trackPageView(String path) {
        }
    };
}