package io.github.jeffshee.nekumorimod;

import android.app.Notification;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XposedMod implements IXposedHookLoadPackage {

    private static final String RAKUTEN_EDY = "jp.edy.edyapp";
    private static final String JAPAN_POST = "jp.japanpost.jp_bank.bankbookapp";
    private static final String SYSYEM_UI = "com.android.systemui";
    // Basic Daydream VR packages
    private static final String GOOGLE_DAYDREAM = "com.google.android.vr.home";
    private static final String GOOGLE_VR_SERVICES = "com.google.vr.vrcore";
    private static final String DAYDREAM_KEYBOARD = "com.google.android.vr.inputmethod";
    // These are the packages detecting android.hardware.vr.high_performance as well
    private static final String FUSED_LOCATION = "com.android.location.fused";
    private static final String ANDROID = "android";
    private static final String SETTINGS_STORAGE = "com.android.providers.settings";
    private static final String CALL_MANAGEMENT = "com.android.server.telecom";
    private static final String ANT_HAL_SERVICE = "com.dsi.ant.server";
    // Dummy app for testing purpose
    private static final String DAYDREAM_DETECTOR = "io.github.jeffshee.daydreamdetector";
    // AptX Bluetooth
    private static final String APTX_BLUETOOTH = "com.qualcomm.qtil.aptxui";
    //
    private static final String ENABLED_VR_LISTENERS = "enabled_vr_listeners";
    private static final String ENABLED_VR_LISTENERS_RESULT = "com.google.vr.vrcore/com.google.vr.vrcore.common.VrCoreListenerService";


    /*
    ro.product.model=Pixel 3 XL
    ro.product.brand=google
    ro.product.name=crosshatch
    ro.product.device=crosshatch

    ro.product.model=Pixel 3
    ro.product.brand=google
    ro.product.name=blueline
    ro.product.device=blueline

    ro.product.model=Pixel
    ro.product.brand=google
    ro.product.name=sailfish
    ro.product.device=sailfish

    ro.product.model=Nexus 6
    ro.product.brand=google
    ro.product.name=shamu
    ro.product.device=shamu
    */

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        switch (lpparam.packageName) {
            case RAKUTEN_EDY:
                /*
                The Rakuten Edy can work on a device without NFC-F, however it's not well tested.
                To use the app on an unsupported device, we need to bypass the device checking.
                We spoof one of the supported device, Nexus 6 here. For more info, refer:
                https://edy.rakuten.co.jp/howto/android/nfc/support/
                 */
                XposedBridge.log("(NekumoriMOD) " + lpparam.packageName);
                deviceSpoofing(lpparam, "Nexus 6", "google", "shamu", "shamu");
                break;
            case GOOGLE_DAYDREAM:
            case GOOGLE_VR_SERVICES:
            case DAYDREAM_KEYBOARD:
                /*
                With Magisk Pix3lify module, we can get Daydream VR support on unsupported device.
                Almost everything work, however, the main scene of Daydream app is visually distorted for unknown reason.
                Issue on Pix3lify:
                https://github.com/Magisk-Modules-Repo/Pix3lify/issues/104
                And if we spoof Pixel device here, the distortion bug disappear, very weird indeed...
                (Tested on OnePlus7, not sure if this can solve the same bug on different device)
                 */
                XposedBridge.log("(NekumoriMOD) " + lpparam.packageName);
                deviceSpoofing(lpparam, "Pixel", "google", "sailfish", "sailfish");
            case FUSED_LOCATION:
            case ANDROID:
            case SETTINGS_STORAGE:
            case CALL_MANAGEMENT:
            case ANT_HAL_SERVICE:
            case DAYDREAM_DETECTOR:
                /*
                Trying a new approach to enable Daydream using Xposed Framework.
                 */
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    enableDaydreamVR(lpparam);
                break;
            case JAPAN_POST:
                /*
                USB debug mode bypass for Japan Post Bank app. Sometimes the so-called "security check" is just ridiculous.
                */
                XposedBridge.log("(NekumoriMOD) " + lpparam.packageName);
                debugModeSpoofing(lpparam);
                break;
            case SYSYEM_UI:
                /*
                Disable lock screen album art (Android 10, 11 only)
                 */
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    XposedBridge.log("(NekumoriMOD) " + lpparam.packageName);
                    disableLockScreenAlbumArt(lpparam);
                }
                break;
            case APTX_BLUETOOTH:
                /*
                Disable the stupid "Device supports Qualcomm® aptX™" notification.
                */
                disableNotification(lpparam);
                break;
            default:
                /*
                Enable DayDreamVR by default, as some VR apps might actually check for hasSystemFeature.
                VR apps should be added to the LSPosed's scope for it to work.
                 */
                XposedBridge.log("(NekumoriMOD) " + lpparam.packageName);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    enableDaydreamVR(lpparam);
                disableNotification(lpparam);
        }
    }

    private void deviceSpoofing(LoadPackageParam lpparam, String model, String brand, String product, String device) {
        ClassLoader classLoader = lpparam.classLoader;
        Class findClass = XposedHelpers.findClass("android.os.Build", classLoader);
        XposedHelpers.setStaticObjectField(findClass, "MODEL", model);
        XposedHelpers.setStaticObjectField(findClass, "BRAND", brand);
        XposedHelpers.setStaticObjectField(findClass, "PRODUCT", product);
        XposedHelpers.setStaticObjectField(findClass, "DEVICE", device);
    }

    private void debugModeSpoofing(LoadPackageParam lpparam) {
        // Resource:
        // https://github.com/redlee90/Hide-USB-Debugging-Mode
        ClassLoader classLoader = lpparam.classLoader;
        XC_MethodHook methodHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (param.args[1].equals(Settings.Secure.ADB_ENABLED)) {
                    param.setResult(0);
                }
            }
        };
        XposedHelpers.findAndHookMethod("android.provider.Settings.Global", classLoader, "getInt", ContentResolver.class, String.class, int.class, methodHook);
        XposedHelpers.findAndHookMethod("android.provider.Settings.Global", classLoader, "getInt", ContentResolver.class, String.class, methodHook);
        XposedHelpers.findAndHookMethod("android.provider.Settings.Secure", classLoader, "getInt", ContentResolver.class, String.class, int.class, methodHook);
        XposedHelpers.findAndHookMethod("android.provider.Settings.Secure", classLoader, "getInt", ContentResolver.class, String.class, methodHook);
    }

    private void disableLockScreenAlbumArt(LoadPackageParam lpparam) {
        /*
        Since only updateMediaMetaData called getMediaMetadata, replacing this method to return null should be OK
         */
        ClassLoader classLoader = lpparam.classLoader;
        XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.NotificationMediaManager", classLoader, "getMediaMetadata", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                return null;
            }
        });
    }

    private void disableNotification(LoadPackageParam lpparam) {
        ClassLoader classLoader = lpparam.classLoader;
        final String packageName= lpparam.packageName;
        XposedHelpers.findAndHookMethod("android.app.NotificationManager", classLoader, "notify", String.class, int.class, Notification.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                Notification notification = (Notification) param.args[2];
                String content = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    content = notification.extras.getString(Notification.EXTRA_TEXT);
                }
                XposedBridge.log("(NekumoriMOD) notification disabled \"" + content + "\" @" + packageName);
                return null;
            }
        });
    }

    private void enableDaydreamVR(LoadPackageParam lpparam) {
        /*
         Resource:
         https://developer.android.com/reference/android/content/pm/FeatureInfo
         https://stackoverflow.com/questions/18485170/how-to-check-specific-features-through-code
         https://stackoverflow.com/questions/66523720/xposed-cant-hook-getinstalledapplications

         Magisk module:
         https://github.com/Magisk-Modules-Repo/Pix3lify
         https://github.com/Magisk-Modules-Repo/PIXELARITY
         https://forum.xda-developers.com/t/daydream-unlocker-nfc-workaround-controller-magisk.3917601/
         https://forum.xda-developers.com/t/magisk-module-bring-back-google-daydream-vr.4002823/ (<- best!)
        */
        ClassLoader classLoader = lpparam.classLoader;
        final String packageName = lpparam.packageName;
        XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", classLoader, "getSystemAvailableFeatures", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                XposedBridge.log("(NekumoriMOD) getSystemAvailableFeatures hooked" + " @" + packageName);

                Map<String, FeatureInfo> mAvailableFeatures = (Map<String, FeatureInfo>) param.getResult();
                ArrayList<String> vrFeatures = new ArrayList<>();
                vrFeatures.add(PackageManager.FEATURE_VR_MODE);
                vrFeatures.add(PackageManager.FEATURE_VR_MODE_HIGH_PERFORMANCE);
                for (String feature : vrFeatures) {
                    if (!mAvailableFeatures.containsKey(feature)) {
                        FeatureInfo featureInfo = new FeatureInfo();
                        featureInfo.name = feature;
                        mAvailableFeatures.put(feature, featureInfo);
                    }
                }
                param.setResult(mAvailableFeatures);
            }
        });
        XC_MethodHook methodHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                switch (param.args[0].toString()) {
                    case PackageManager.FEATURE_VR_MODE:
                    case PackageManager.FEATURE_VR_MODE_HIGH_PERFORMANCE:
                        XposedBridge.log("(NekumoriMOD) hasSystemFeature hooked for " + Arrays.toString(param.args) + " @" + packageName);
                        param.setResult(true);
                }
            }
        };
        XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", classLoader, "hasSystemFeature", String.class, methodHook);
        XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", classLoader, "hasSystemFeature", String.class, int.class, methodHook);

        // Probably redundant.
        XposedHelpers.findAndHookMethod("android.provider.Settings.Secure", classLoader, "getString", ContentResolver.class, String.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (param.args[1].equals(ENABLED_VR_LISTENERS)) {
                    XposedBridge.log("(NekumoriMOD) getString hooked for " + Arrays.toString(param.args));
                    param.setResult(ENABLED_VR_LISTENERS_RESULT);
                }
            }
        });
    }

    private Context getSystemContext(LoadPackageParam lpparam) {
        ClassLoader classLoader = lpparam.classLoader;
        return (Context) XposedHelpers.callMethod(
                XposedHelpers.callStaticMethod(
                        XposedHelpers.findClass("android.app.ActivityThread", classLoader),
                        "currentActivityThread"),
                "getSystemContext");
    }


}