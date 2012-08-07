package org.nick.cryptfs.passwdmanager;

import java.util.List;

import android.os.Build;
import android.util.Log;

public class CryptfsCommands {

    private static final String TAG = CryptfsCommands.class.getSimpleName();

    private static final boolean IS_JB = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;

    private static final String GET_PROP_CMD_PATH = "/system/bin/getprop";
    private static final String CRYPTO_STATE_PROP = "ro.crypto.state";
    private static final String CRYPTO_STATE_ENCRYPTED = "encrypted";

    private static final String VDC_CMD_PATH = "/system/bin/vdc";
    private static final String CRYPTFS_VERIFYPW_CMD = VDC_CMD_PATH
            + " cryptfs verifypw %s";
    private static final String CRYPTFS_CHANGEPW_CMD = VDC_CMD_PATH
            + " cryptfs changepw %s";

    private static final String VDC_STATUS_OK = "200";
    private static final String VDC_OK_RC = "0";


    public CryptfsCommands() {
    }

    public static boolean checkCryptfsPassword(String password) {
        List<String> response = SuShell.runWithSu(String.format(
                CRYPTFS_VERIFYPW_CMD, password));
        return checkVdcResponse(response);
    }

    private static boolean checkVdcResponse(List<String> result) {
        if (result.isEmpty()) {
            Log.wtf(TAG, "No result from vdc command?");
            return false;
        }

        String status = result.get(0);
        String[] fields = status.split(" ");
        if (IS_JB && fields.length != 3) {
            Log.wtf(TAG, "Unrecognized vdc output format: " + status);
            return false;
        }

        if (!IS_JB && fields.length != 2) {
            Log.wtf(TAG, "Unrecognized vdc output format: " + status);
            return false;
        }

        if (!fields[0].equals(VDC_STATUS_OK)) {
            Log.e(TAG, "vdc returned error: " + status);
            return false;
        }

        return IS_JB ? fields[2].equals(VDC_OK_RC) : fields[1]
                .equals(VDC_OK_RC);
    }

    public static boolean changeCryptfsPassword(String newPassword) {
        List<String> response = SuShell.run("su",
                String.format(CRYPTFS_CHANGEPW_CMD, newPassword));

        return checkVdcResponse(response);
    }

    public static boolean isDeviceEncrypted() {
        try {
            String value = getProp(CRYPTO_STATE_PROP);
            Log.d(TAG, CRYPTO_STATE_PROP + "= " + value);

            return value.equals(CRYPTO_STATE_ENCRYPTED);
        } catch (Exception e) {
            return false;
        }
    }

    private static String getProp(String propertyName) {
        return SuShell
                .runWithShell(GET_PROP_CMD_PATH + " " + CRYPTO_STATE_PROP).get(
                        0);
    }

}
