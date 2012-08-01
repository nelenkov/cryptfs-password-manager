package org.nick.cryptfs.passwdmanager;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String ARG_NEW_PASSWD = "newPasswd";
    private static final String ARG_CURRENT_PASSWD = "currentPasswd";

    private EditText currentPasswdText;
    private EditText newPasswdText;
    private EditText confirmNewPasswdText;

    private Button changePasswordButon;

    private ChangePasswdTask changePasswdTask;

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        currentPasswdText = (EditText) findViewById(R.id.currentPasswdText);
        newPasswdText = (EditText) findViewById(R.id.newPasswordText);
        confirmNewPasswdText = (EditText) findViewById(R.id.confirmNewPasswdText);

        changePasswordButon = (Button) findViewById(R.id.changePasswdButton);
        changePasswordButon.setOnClickListener(this);
        changePasswordButon.setEnabled(false);

        if (getLastNonConfigurationInstance() != null) {
            changePasswdTask = (ChangePasswdTask) getLastNonConfigurationInstance();
            changePasswdTask.attach(this);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!CryptfsCommands.isDeviceEncrypted()) {
            Toast.makeText(this, R.string.device_not_encrypted,
                    Toast.LENGTH_LONG).show();
            finish();
        }

        if (!SuShell.isSuperUserInstalled(getApplicationContext())) {
            Toast.makeText(this, R.string.no_su_apk, Toast.LENGTH_LONG).show();
            finish();
        }

        new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected void onPreExecute() {
                setProgressBarIndeterminateVisibility(true);
                changePasswordButon.setEnabled(false);
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    return SuShell.canGainSu(getApplicationContext());
                } catch (Exception e) {
                    Log.e(TAG, "Error: " + e.getMessage(), e);
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean result) {
                setProgressBarIndeterminateVisibility(false);

                if (!result) {
                    Toast.makeText(MainActivity.this, R.string.cannot_get_su,
                            Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    changePasswordButon.setEnabled(true);
                }
            }
        }.execute();
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        if (changePasswdTask != null) {
            return changePasswdTask.detach();
        }

        return null;
    }

    @Override
    public void onClick(View v) {
        String currentPasswd = currentPasswdText.getText().toString().trim();
        if (isEmpty(currentPasswd)) {
            currentPasswdText
                    .setError(getString(R.string.current_passwd_required));
            return;
        }
        String newPasswd = newPasswdText.getText().toString().trim();
        if (isEmpty(newPasswd)) {
            newPasswdText.setError(getString(R.string.new_passwd_required));
            return;
        }
        String confirmPasswd = confirmNewPasswdText.getText().toString().trim();
        if (isEmpty(confirmPasswd)) {
            confirmNewPasswdText
                    .setError(getString(R.string.confirmation_passwd_required));
            return;
        }

        if (!newPasswd.equals(confirmPasswd)) {
            confirmNewPasswdText
                    .setError(getString(R.string.password_mismatch));
            return;
        }

        showConfirmationDialog(currentPasswd, newPasswd);
    }

    private void showConfirmationDialog(String currentPasswd, String newPasswd) {
        DialogFragment confirmationDialog = ConfirmationDialogFragment
                .newInstance(currentPasswd, newPasswd);
        confirmationDialog.show(getFragmentManager(), "confirmationDialog");
    }

    private void changePasswd(String currentPasswd, String newPasswd) {
        changePasswdTask = new ChangePasswdTask(this);
        changePasswdTask.execute(currentPasswd, newPasswd);
    }

    static class ChangePasswdTask extends AsyncTask<String, Void, Integer> {

        private static final int PASSWD_INVALID = 0;
        private static final int PASSWD_CHANGED = 1;
        private static final int PASSWD_CHANGE_ERROR = 2;

        private MainActivity activity;
        private String currentPasswd;
        private String newPasswd;

        ChangePasswdTask(MainActivity activity) {
            this.activity = activity;
        }

        void attach(MainActivity activity) {
            this.activity = activity;
        }

        ChangePasswdTask detach() {
            activity = null;

            return this;
        }

        @Override
        protected void onPreExecute() {
            activity.setProgressBarIndeterminateVisibility(true);
            activity.tooggleButton(false);
        }

        @Override
        protected Integer doInBackground(String... params) {
            if (activity == null) {
                return -1;
            }

            currentPasswd = params[0];
            newPasswd = params[1];
            if (!CryptfsCommands.checkCryptfsPassword(currentPasswd)) {
                return PASSWD_INVALID;
            }

            return CryptfsCommands.changeCryptfsPassword(newPasswd) ? PASSWD_CHANGED
                    : PASSWD_CHANGE_ERROR;
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (activity == null) {
                return;
            }

            activity.setProgressBarIndeterminateVisibility(false);
            activity.tooggleButton(true);

            switch (result) {
            case PASSWD_INVALID:
                activity.showInvalidPasswordError();
                break;
            case PASSWD_CHANGED:
                activity.clearPasswords();

                Toast.makeText(activity, R.string.successfuly_changed_password,
                        Toast.LENGTH_LONG).show();
                PasswordChangedDialogFragment successDialog = PasswordChangedDialogFragment
                        .newInstance(newPasswd);
                successDialog.show(activity.getFragmentManager(),
                        "successDialog");
                break;
            case PASSWD_CHANGE_ERROR:

                Toast.makeText(activity, R.string.failed_to_change_password,
                        Toast.LENGTH_LONG).show();
                break;
            default:
                // detached or cancelled, do nothing
            }
        }
    }

    private void clearPasswords() {
        clearErrors();

        currentPasswdText.setText("");
        newPasswdText.setText("");
        confirmNewPasswdText.setText("");
    }

    private void clearErrors() {
        currentPasswdText.setError(null);
        newPasswdText.setError(null);
        confirmNewPasswdText.setError(null);
    }

    private void showInvalidPasswordError() {
        String invalidPasswordmessage = getString(R.string.invalid_password);
        currentPasswdText.setError(invalidPasswordmessage);

        ErrorDialogFragment errorDialog = ErrorDialogFragment.newInstance(
                invalidPasswordmessage,
                getResources().getString(R.string.current_password_incorrect));
        errorDialog.show(getFragmentManager(), "errorDialog");
    }

    private void tooggleButton(boolean enable) {
        changePasswordButon.setEnabled(enable);
    }

    private static boolean isEmpty(String str) {
        return str == null || "".equals(str);
    }

    public static class ConfirmationDialogFragment extends DialogFragment {

        public static ConfirmationDialogFragment newInstance(
                String currentPasswd, String newPasswd) {
            ConfirmationDialogFragment frag = new ConfirmationDialogFragment();
            Bundle args = new Bundle();
            args.putString(ARG_CURRENT_PASSWD, currentPasswd);
            args.putString(ARG_NEW_PASSWD, newPasswd);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final String currentPasswd = getArguments().getString(
                    ARG_CURRENT_PASSWD);
            final String newPasswd = getArguments().getString(ARG_NEW_PASSWD);

            return new AlertDialog.Builder(getActivity())
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.confirmation_dialog_title)
                    .setMessage(
                            getResources().getString(
                                    R.string.confirmation_dialog_message,
                                    newPasswd))
                    .setPositiveButton(android.R.string.yes,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int whichButton) {
                                    ((MainActivity) getActivity())
                                            .changePasswd(currentPasswd,
                                                    newPasswd);
                                }
                            })
                    .setNegativeButton(android.R.string.no,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int whichButton) {
                                    ((MainActivity) getActivity())
                                            .clearPasswords();
                                }
                            }).create();
        }
    }

    public static class PasswordChangedDialogFragment extends DialogFragment {

        public static PasswordChangedDialogFragment newInstance(String newPasswd) {
            PasswordChangedDialogFragment frag = new PasswordChangedDialogFragment();
            Bundle args = new Bundle();
            args.putString(ARG_NEW_PASSWD, newPasswd);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final String newPasswd = getArguments().getString(ARG_NEW_PASSWD);

            return new AlertDialog.Builder(getActivity())
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .setTitle(R.string.new_passwd_dialog_title)
                    .setMessage(
                            getResources().getString(
                                    R.string.new_passwd_dialog_message,
                                    newPasswd))
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int whichButton) {
                                    dismiss();
                                }
                            }).create();
        }
    }

    public static class ErrorDialogFragment extends DialogFragment {

        private static final String ARG_TITLE = "title";
        private static final String ARG_MESSAGE = "message";

        public static ErrorDialogFragment newInstance(String title,
                String message) {
            ErrorDialogFragment frag = new ErrorDialogFragment();
            Bundle args = new Bundle();
            args.putString(ARG_TITLE, title);
            args.putString(ARG_MESSAGE, message);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final String title = getArguments().getString(ARG_TITLE);
            final String message = getArguments().getString(ARG_MESSAGE);

            return new AlertDialog.Builder(getActivity())
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int whichButton) {
                                    dismiss();
                                }
                            }).create();
        }
    }
}
