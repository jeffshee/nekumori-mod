package io.github.jeffshee.nekumorimod;

import android.content.ContentResolver;
import android.os.Build;
import android.provider.Settings;

import java.util.HashMap;
import java.util.Map;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XposedMod implements IXposedHookLoadPackage {

    private static final String RAKUTEN_EDY = "jp.edy.edyapp";
    private static final String GOOGLE_DAYDREAM = "com.google.android.vr.home";
    private static final String JAPAN_POST = "jp.japanpost.jp_bank.bankbookapp";
    private static final String SYSYEM_UI = "com.android.systemui";

    private static final Map<String, String> BUILD_NEXUS_6 = new HashMap<String, String>() {
        {
            put("MODEL", "Nexus 6");
            put("BRAND", "google");
            put("PRODUCT", "shamu");
            put("DEVICE", "shamu");

        }
    };
    private static final Map<String, String> BUILD_PIXEL = new HashMap<String, String>() {
        {
            put("MODEL", "Pixel");
            put("BRAND", "google");
            put("PRODUCT", "sailfish");
            put("DEVICE", "sailfish");

        }
    };

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
                deviceSpoofing(lpparam.classLoader, BUILD_NEXUS_6);
                break;
            case GOOGLE_DAYDREAM:
                /*
                With Magisk Pix3lify module, we can get Daydream VR support on unsupported device.
                Almost everything work, however, the main scene of Daydream app is visually distorted for unknown reason.
                Issue on Pix3lify:
                https://github.com/Magisk-Modules-Repo/Pix3lify/issues/104
                And if we spoof Pixel device here, the distortion bug disappear, very weird indeed...
                (Tested on OnePlus7, not sure if this can solve the same bug on different device)
                 */
                XposedBridge.log("(NekumoriMOD) " + lpparam.packageName);
                deviceSpoofing(lpparam.classLoader, BUILD_PIXEL);
                break;
            case JAPAN_POST:
                /*
                USB debug mode bypass for Japan Post Bank app. Sometimes the so-called "security check" is just ridiculous.
                */
                XposedBridge.log("(NekumoriMOD) " + lpparam.packageName);
                debugModeSpoofing(lpparam.classLoader);
                break;
            case SYSYEM_UI:
                /*
                Disable lock screen album art (Android 10 only)
                 */
                if (android.os.Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                    XposedBridge.log("(NekumoriMOD) " + lpparam.packageName);
                    disableLockScreenAlbumArt(lpparam.classLoader);
                }
                break;
        }
    }

    private void deviceSpoofing(ClassLoader classLoader, Map<String, String> build) {
        Class findClass = XposedHelpers.findClass("android.os.Build", classLoader);
        XposedHelpers.setStaticObjectField(findClass, "MODEL", build.get("MODEL"));
        XposedHelpers.setStaticObjectField(findClass, "BRAND", build.get("BRAND"));
        XposedHelpers.setStaticObjectField(findClass, "PRODUCT", build.get("PRODUCT"));
        XposedHelpers.setStaticObjectField(findClass, "DEVICE", build.get("DEVICE"));
    }

    private void debugModeSpoofing(ClassLoader classLoader) {
        // https://github.com/redlee90/Hide-USB-Debugging-Mode
        XposedHelpers.findAndHookMethod("android.provider.Settings.Global", classLoader, "getInt", ContentResolver.class, String.class, int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                if (param.args[1].equals(Settings.Global.ADB_ENABLED)) {
                    param.setResult(0);
                }
            }
        });

        XposedHelpers.findAndHookMethod("android.provider.Settings.Global", classLoader, "getInt", ContentResolver.class, String.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (param.args[1].equals(Settings.Global.ADB_ENABLED)) {
                    param.setResult(0);
                }
            }
        });

        XposedHelpers.findAndHookMethod("android.provider.Settings.Secure", classLoader, "getInt", ContentResolver.class, String.class, int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (param.args[1].equals(Settings.Secure.ADB_ENABLED)) {
                    param.setResult(0);
                }
            }
        });

        XposedHelpers.findAndHookMethod("android.provider.Settings.Secure", classLoader, "getInt", ContentResolver.class, String.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (param.args[1].equals(Settings.Secure.ADB_ENABLED)) {
                    param.setResult(0);
                }
            }
        });
    }

    private void disableLockScreenAlbumArt(ClassLoader classLoader) {
        /*
        Since only updateMediaMetaData called getMediaMetadata, replacing this method to return null should be OK
         */
        XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.NotificationMediaManager", classLoader, "getMediaMetadata", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                XposedBridge.log("(NekumoriMOD) " + "disableLockScreenAlbumArt");
                return null;
            }
        });
    }

}