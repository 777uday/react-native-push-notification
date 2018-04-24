package com.dieam.reactnativepushnotification.modules;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.dieam.reactnativepushnotification.helpers.ApplicationBadgeHelper;
import com.facebook.react.ReactApplication;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.google.android.gms.gcm.GcmListenerService;

import org.json.JSONObject;

import java.util.List;
import java.util.Random;

import static com.dieam.reactnativepushnotification.modules.RNPushNotification.LOG_TAG;

public class RNPushNotificationListenerService extends GcmListenerService {

    @Override
    public void onMessageReceived(String from, final Bundle bundle) {
        JSONObject data = getPushData(bundle.getString("data"));
        if (data != null) {
            if (!bundle.containsKey("message")) {
                bundle.putString("message", data.optString("alert", "Notification received"));
            }
            if (!bundle.containsKey("title")) {
                bundle.putString("title", data.optString("title", null));
            }
            if (!bundle.containsKey("sound")) {
                bundle.putString("soundName", data.optString("sound", null));
            }
            if (!bundle.containsKey("color")) {
                bundle.putString("color", data.optString("color", null));
            }

            final int badge = data.optInt("badge", -1);
            if (badge >= 0) {
                ApplicationBadgeHelper.INSTANCE.setApplicationIconBadgeNumber(this, badge);
            }
        }

        Log.v(LOG_TAG, "onMessageReceived: " + bundle);

        // We need to run this on the main thread, as the React code assumes that is true.
        // Namely, DevServerHelper constructs a Handler() without a Looper, which triggers:
        // "Can't create handler inside thread that has not called Looper.prepare()"
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            public void run() {
                // Construct and load our normal React JS code bundle
                ReactInstanceManager mReactInstanceManager = ((ReactApplication) getApplication()).getReactNativeHost().getReactInstanceManager();
                ReactContext context = mReactInstanceManager.getCurrentReactContext();
                Log.v(LOG_TAG, "uday:context: " + context);
                // If it's constructed, send a notification
                if (context != null) {
                    String message = bundle.getString("message");
                    if (message.equals("Asset assigned to zone.")){
                        String assetName = bundle.getString("assetName");
                        String zoneName = bundle.getString("zoneName");
                        String messageForNotification = "Tank" + " - " + assetName + " assigned to " + zoneName;
                        bundle.putString("message", messageForNotification);
                        bundle.putString("initMessage", message);
                        handleRemotePushNotification((ReactApplicationContext) context, bundle);
                    } else if (message.equals("Asset removed from zone.")){
                        String assetName = bundle.getString("assetName");
                        String zoneName = bundle.getString("zoneName");
                        String messageForNotification = "Tank" + " - " + assetName + " unassigned from " + zoneName;
                        bundle.putString("message", messageForNotification);
                        bundle.putString("initMessage", message);
                        handleRemotePushNotification((ReactApplicationContext) context, bundle);
                    } else if (message.equals("Asset deleted.")){
                        String assetName = bundle.getString("assetName");
                        String messageForNotification = "Tank"+ " - " + assetName + " - " + message;
                        bundle.putString("message", messageForNotification);
                        bundle.putString("initMessage", message);
                        handleRemotePushNotification((ReactApplicationContext) context, bundle);
                    } else if (message.equals("New alert generated.")){
                        String assetName = bundle.getString("assetName");
                        String sensorName = bundle.getString("sensorName");
                        String alert = bundle.getString("alert");
                        String messageForNotification = assetName + " - " + sensorName + " - " + alert;
                        bundle.putString("message", messageForNotification);
                        bundle.putString("initMessage", message);
                        handleRemotePushNotification((ReactApplicationContext) context, bundle);
                    }
                } else {
                    // Otherwise wait for construction, then send the notification
                    mReactInstanceManager.addReactInstanceEventListener(new ReactInstanceManager.ReactInstanceEventListener() {
                        public void onReactContextInitialized(ReactContext context) {
                            String message = bundle.getString("message");
                            if (message.equals("Asset assigned to zone.")){
                                String assetName = bundle.getString("assetName");
                                String zoneName = bundle.getString("zoneName");
                                String messageForNotification = "Tank" + " - " + assetName + " assigned to " + zoneName;
                                bundle.putString("message", messageForNotification);
                                bundle.putString("initMessage", message);
                                handleRemotePushNotification((ReactApplicationContext) context, bundle);
                            } else if (message.equals("Asset removed from zone.")){
                                String assetName = bundle.getString("assetName");
                                String zoneName = bundle.getString("zoneName");
                                String messageForNotification = "Tank" + " - " + assetName + " unassigned from " + zoneName;
                                bundle.putString("message", messageForNotification);
                                bundle.putString("initMessage", message);
                                handleRemotePushNotification((ReactApplicationContext) context, bundle);
                            } else if (message.equals("Asset deleted.")){
                                String assetName = bundle.getString("assetName");
                                String messageForNotification = "Tank"+ " - " + assetName + " - " + message;
                                bundle.putString("message", messageForNotification);
                                bundle.putString("initMessage", message);
                                handleRemotePushNotification((ReactApplicationContext) context, bundle);
                            } else if (message.equals("New alert generated.")){
                                String assetName = bundle.getString("assetName");
                                String sensorName = bundle.getString("sensorName");
                                String alert = bundle.getString("alert");
                                String messageForNotification = assetName + " - " + sensorName + " - " + alert;
                                bundle.putString("message", messageForNotification);
                                bundle.putString("initMessage", message);
                                handleRemotePushNotification((ReactApplicationContext) context, bundle);
                            }
                            handleRemotePushNotification((ReactApplicationContext) context, bundle);
                        }
                    });
                    if (!mReactInstanceManager.hasStartedCreatingInitialContext()) {
                        // Construct it in the background
                        mReactInstanceManager.createReactContextInBackground();
                    }
                }
            }
        });
    }

    private JSONObject getPushData(String dataString) {
        try {
            return new JSONObject(dataString);
        } catch (Exception e) {
            return null;
        }
    }

    private void handleRemotePushNotification(ReactApplicationContext context, Bundle bundle) {

        // If notification ID is not provided by the user for push notification, generate one at random
        if (bundle.getString("id") == null) {
            Random randomNumberGenerator = new Random(System.currentTimeMillis());
            bundle.putString("id", String.valueOf(randomNumberGenerator.nextInt()));
        }

        Boolean isForeground = isApplicationInForeground();

        RNPushNotificationJsDelivery jsDelivery = new RNPushNotificationJsDelivery(context);
        bundle.putBoolean("foreground", isForeground);
        bundle.putBoolean("userInteraction", false);
        jsDelivery.notifyNotification(bundle);

        // If contentAvailable is set to true, then send out a remote fetch event
        if (bundle.getString("contentAvailable", "false").equalsIgnoreCase("true")) {
            jsDelivery.notifyRemoteFetch(bundle);
        }

        Log.v(LOG_TAG, "sendNotification: " + bundle);

        if (!isForeground) {
            Application applicationContext = (Application) context.getApplicationContext();
            RNPushNotificationHelper pushNotificationHelper = new RNPushNotificationHelper(applicationContext);
            pushNotificationHelper.sendToNotificationCentre(bundle);
        }
    }

    private boolean isApplicationInForeground() {
        ActivityManager activityManager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
        List<RunningAppProcessInfo> processInfos = activityManager.getRunningAppProcesses();
        if (processInfos != null) {
            for (RunningAppProcessInfo processInfo : processInfos) {
                if (processInfo.processName.equals(getApplication().getPackageName())) {
                    if (processInfo.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                        for (String d : processInfo.pkgList) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}