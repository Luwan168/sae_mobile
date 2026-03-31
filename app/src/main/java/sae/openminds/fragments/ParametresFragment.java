package sae.openminds.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.gson.JsonParser;
import com.koushikdutta.ion.Ion;

import sae.openminds.Config;
import sae.openminds.LoginActivity;
import sae.openminds.R;
import sae.openminds.utils.LocaleHelper;

public class ParametresFragment extends Fragment {

    private String token;
    private String role;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_parametres, container, false);

        SharedPreferences prefs = requireActivity()
                .getSharedPreferences(Config.PREFS_NAME, Context.MODE_PRIVATE);
        token = prefs.getString(Config.KEY_TOKEN, "");
        role  = prefs.getString(Config.KEY_ROLE, Config.ROLE_BENEVOLE);

        Switch   switchNotifs     = view.findViewById(R.id.switchNotifs);
        TextView tvChangePassword = view.findViewById(R.id.tvChangePassword);
        TextView tvLanguage       = view.findViewById(R.id.tvLanguage);
        TextView tvDeleteAccount  = view.findViewById(R.id.tvDeleteAccount);

        switchNotifs.setChecked(prefs.getBoolean(Config.KEY_NOTIFS, true));
        switchNotifs.setOnCheckedChangeListener((btn, checked) ->
                prefs.edit().putBoolean(Config.KEY_NOTIFS, checked).apply());

        tvChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        tvLanguage.setOnClickListener(v -> showLanguageDialog());

        // Admin ne peut pas supprimer son compte
        if (role.equals(Config.ROLE_ADMIN)) {
            tvDeleteAccount.setVisibility(View.GONE);
        } else {
            tvDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());
        }

        return view;
    }

    private void showLanguageDialog() {
        String[] langs = { getString(R.string.lang_english), getString(R.string.lang_french), getString(R.string.lang_spanish) };
        String[] codes = {"en", "fr", "es"};
        new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.lbl_language))
                .setItems(langs, (dialog, which) -> {
                    LocaleHelper.saveLanguage(requireActivity(), codes[which]);
                    requireActivity().recreate();
                }).show();
    }

    private void showChangePasswordDialog() {
        LinearLayout layout = new LinearLayout(getActivity());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 40);
        EditText etOld = new EditText(getActivity());
        etOld.setHint(getString(R.string.hint_old_pass));
        etOld.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etOld);
        EditText etNew = new EditText(getActivity());
        etNew.setHint(getString(R.string.hint_new_pass));
        etNew.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etNew);
        new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.dialog_change_pass_title)).setView(layout)
                .setPositiveButton(getString(R.string.btn_ok), (dialog, which) -> {
                    String oldPass = etOld.getText().toString().trim();
                    String newPass = etNew.getText().toString().trim();
                    if (oldPass.isEmpty() || newPass.isEmpty()) { Toast.makeText(getActivity(), getString(R.string.err_fill_fields), Toast.LENGTH_SHORT).show(); return; }
                    if (newPass.length() < 8) { Toast.makeText(getActivity(), getString(R.string.err_password_length), Toast.LENGTH_SHORT).show(); return; }
                    Ion.with(this).load("POST", Config.BASE_URL + "updatePassword.php")
                            .setBodyParameter("token", token).setBodyParameter("old_password", oldPass).setBodyParameter("new_password", newPass)
                            .asString().setCallback((e, result) -> {
                                if (e != null || result == null) { Toast.makeText(getActivity(), getString(R.string.err_server), Toast.LENGTH_SHORT).show(); return; }
                                try {
                                    String status = JsonParser.parseString(result).getAsJsonObject().get("status").getAsString();
                                    Toast.makeText(getActivity(), status.equals("success") ? getString(R.string.success_password_updated) : getString(R.string.err_wrong_password), Toast.LENGTH_SHORT).show();
                                } catch (Exception ex) { Toast.makeText(getActivity(), getString(R.string.err_server), Toast.LENGTH_SHORT).show(); }
                            });
                }).setNegativeButton(getString(R.string.btn_cancel), null).show();
    }

    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.dialog_delete_title))
                .setMessage(getString(R.string.dialog_delete_msg))
                .setPositiveButton(getString(R.string.btn_confirm_delete), (dialog, which) ->
                    Ion.with(this).load("POST", Config.BASE_URL + "deleteAccount.php")
                            .setBodyParameter("token", token).asString()
                            .setCallback((e, result) -> {
                                if (e != null || result == null) { Toast.makeText(getActivity(), getString(R.string.err_server), Toast.LENGTH_SHORT).show(); return; }
                                try {
                                    String status = JsonParser.parseString(result).getAsJsonObject().get("status").getAsString();
                                    if (status.equals("success")) {
                                        requireActivity().getSharedPreferences(Config.PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply();
                                        Intent intent = new Intent(getActivity(), LoginActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                    } else {
                                        Toast.makeText(getActivity(), getString(R.string.err_server), Toast.LENGTH_SHORT).show();
                                    }
                                } catch (Exception ex) { Toast.makeText(getActivity(), getString(R.string.err_server), Toast.LENGTH_SHORT).show(); }
                            }))
                .setNegativeButton(getString(R.string.btn_cancel), null).show();
    }
}
