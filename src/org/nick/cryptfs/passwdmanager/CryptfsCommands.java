
package org.nick.cryptfs.passwdmanager;

import android.os.Build;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.regex.Pattern;

public class CryptfsCommands {

    private static final String TAG = CryptfsCommands.class.getSimpleName();

    private static final boolean IS_JB = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;

    private static final boolean IS_M = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;

    private static final String GET_PROP_CMD_PATH = "/system/bin/getprop";
    private static final String CRYPTO_STATE_PROP = "ro.crypto.state";
    private static final String CRYPTO_STATE_ENCRYPTED = "encrypted";

    private static final String VDC_CMD_PATH = "/system/bin/vdc";
    private static final String CRYPTFS_VERIFYPW_CMD = VDC_CMD_PATH
            + " cryptfs verifypw '%s'";
    private static final String CRYPTFS_VERIFYPW_LOLLIPOP_CMD = VDC_CMD_PATH
            + " cryptfs verifypw %s";
    private static final String CRYPTFS_CHANGEPW_CMD = VDC_CMD_PATH
            + " cryptfs changepw '%s'";
    private static final String CRYPTFS_CHANGEPW_PIN_CMD = VDC_CMD_PATH
            + " cryptfs changepw pin %s";
    private static final String CRYPTFS_CHANGEPW_PASSWORD_CMD = VDC_CMD_PATH
            + " cryptfs changepw password %s";
    private static final String CRYPTFS_CHANGEPW_PIN_CM_CMD = VDC_CMD_PATH
            + " cryptfs changepw pin %s %s";
    private static final String CRYPTFS_CHANGEPW_PASSWORD_CM_CMD = VDC_CMD_PATH
            + " cryptfs changepw password %s %s";

    private static final String CRYPTFS_GETPWTYPE_CMD = VDC_CMD_PATH
            + " cryptfs getpwtype";

    private static final int VDC_STATUS_OK = 200;
    private static final int VDC_STATUS_PWTYPE_RESULT = 213;
    private static final String VDC_OK_RC = "0";

    private static final Pattern PIN_PATTERN = Pattern.compile("\\d+");

    public static final String PWTYPE_DEFAULT = "default";
    public static final String DEFAULT_PASSWORD = "default_password";

    public CryptfsCommands() {
    }

    public static boolean checkCryptfsPassword(String password) {
        List<String> response = SuShell.runWithSu(String.format(
                CRYPTFS_VERIFYPW_CMD, escape(password)));
        return checkVdcResponse(response);
    }

    public static boolean checkCryptfsPasswordLollipop(String password) {
        String encodedPassword = IS_M ? password : toHexAscii(password);
        List<String> response = SuShell.runWithSu(String.format(
                CRYPTFS_VERIFYPW_LOLLIPOP_CMD, encodedPassword));
        return checkVdcResponse(response);
    }

    public static String getPasswordType() {
        List<String> response = SuShell.runWithSu(CRYPTFS_GETPWTYPE_CMD);
        boolean responseOk = checkVdcResponse(response);
        // command not supported on older versions
        if (!responseOk) {
            return null;
        }

        return response.get(0).split(" ")[2];
    }

    private static String toHexAscii(String password) {
        if (password == null) {
            return "";
        }

        try {
            return toHex(password.getBytes("ASCII"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

    }

    public static String toHex(byte[] bytes) {
        StringBuffer buff = new StringBuffer();
        for (byte b : bytes) {
            buff.append(String.format("%02X", b));
        }

        return buff.toString();
    }

    private static String escape(String str) {
        // escape double quotes and backslashes
        // FrameworkListener::dispatchCommand checks for this
        String result = str.replaceAll("\\\"", "\\\\\"");
        // only do this if the original string had a backslash
        if (str.contains("\\")) {
            result = result.replaceAll("\\\\", "\\\\\\\\");
        }
        // escape single quotes for the shell
        result = result.replaceAll("'", "'\\\\''");

        return result;
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

        int responseCode = Integer.parseInt(fields[0]);
        if (responseCode != VDC_STATUS_OK && responseCode != VDC_STATUS_PWTYPE_RESULT) {
            Log.e(TAG, "vdc returned error: " + status);
            return false;
        }

        if (responseCode == VDC_STATUS_PWTYPE_RESULT) {
            return true;
        }

        return IS_JB ? fields[2].equals(VDC_OK_RC) : fields[1]
                .equals(VDC_OK_RC);
    }

    public static boolean changeCryptfsPassword(String newPassword,
            String oldPassword) {
        List<String> response = SuShell.run("su",
                String.format(CRYPTFS_CHANGEPW_CMD, escape(newPassword)));

        boolean changeResult = checkVdcResponse(response);
        boolean verifyResult = checkCryptfsPassword(newPassword);
        if (!verifyResult) {
            // rollback
            boolean rollbackResult = changePasswordNoVerify(oldPassword);
            Log.d(TAG, "Password rollback succeeded: " + rollbackResult);

            return false;
        }

        return changeResult && verifyResult;
    }

    public static boolean changeCryptfsPasswordLollipop(String newPassword,
            String oldPassword) {
        oldPassword = (oldPassword == null || "".equals(oldPassword)) ? DEFAULT_PASSWORD : oldPassword;
        String encodedNewPassword = IS_M ? newPassword : toHexAscii(newPassword);
        String encodedOldPassword = IS_M ? oldPassword : toHexAscii(oldPassword);
        boolean isCyanogenmod13 = IS_M && SuShell.isCyanogenmod();

        String command = isCyanogenmod13 ? CRYPTFS_CHANGEPW_PASSWORD_CM_CMD
                                         : CRYPTFS_CHANGEPW_PASSWORD_CMD;
        if (PIN_PATTERN.matcher(newPassword).matches()) {
            command = isCyanogenmod13 ? CRYPTFS_CHANGEPW_PIN_CM_CMD
                                      : CRYPTFS_CHANGEPW_PIN_CMD;
        }
        List<String> response = SuShell.run("su",
                isCyanogenmod13 ? String.format(command, encodedOldPassword, encodedNewPassword)
                                : String.format(command, encodedNewPassword));

        boolean changeResult = checkVdcResponse(response);
        boolean verifyResult = checkCryptfsPasswordLollipop(newPassword);
        if (!verifyResult) {
            // rollback
            boolean rollbackResult = changePasswordNoVerifyLollipop(oldPassword);
            Log.d(TAG, "Password rollback succeeded: " + rollbackResult);

            return false;
        }

        return changeResult && verifyResult;
    }

    private static boolean changePasswordNoVerify(String newPassword) {
        List<String> response = SuShell.run("su",
                String.format(CRYPTFS_CHANGEPW_CMD, escape(newPassword)));

        return checkVdcResponse(response);
    }

    private static boolean changePasswordNoVerifyLollipop(String newPassword) {
        String encodedPassword = IS_M ? newPassword : toHexAscii(newPassword);
        boolean isCyanogenmod13 = IS_M && SuShell.isCyanogenmod();

        String command = isCyanogenmod13 ? CRYPTFS_CHANGEPW_PASSWORD_CM_CMD
                                         : CRYPTFS_CHANGEPW_PASSWORD_CMD;
        if (PIN_PATTERN.matcher(newPassword).matches()) {
            command = isCyanogenmod13 ? CRYPTFS_CHANGEPW_PIN_CM_CMD
                                      : CRYPTFS_CHANGEPW_PIN_CMD;
        }
        List<String> response = SuShell.run("su",
                isCyanogenmod13 ? String.format(command, encodedPassword, encodedPassword)
                                : String.format(command, encodedPassword));

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
