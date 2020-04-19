package io.github.jeffshee.nekumorimod;

import java.util.HashMap;
import java.util.Map;

import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XposedMod implements IXposedHookLoadPackage {

    private static final String RAKUTEN_EDY = "jp.edy.edyapp";
    private static final String GOOGLE_DAYDREAM = "com.google.android.vr.home";

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
        }
    }

    private void deviceSpoofing(ClassLoader classLoader, Map<String, String> build) {
        Class findClass = XposedHelpers.findClass("android.os.Build", classLoader);
        XposedHelpers.setStaticObjectField(findClass, "MODEL", build.get("MODEL"));
        XposedHelpers.setStaticObjectField(findClass, "BRAND", build.get("BRAND"));
        XposedHelpers.setStaticObjectField(findClass, "PRODUCT", build.get("PRODUCT"));
        XposedHelpers.setStaticObjectField(findClass, "DEVICE", build.get("DEVICE"));
    }

}