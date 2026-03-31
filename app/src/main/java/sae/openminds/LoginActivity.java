package sae.openminds;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.koushikdutta.ion.Ion;

import sae.openminds.utils.LocaleHelper;
import sae.openminds.utils.NotificationReceiver;

// ============================================================
//  app/src/main/java/sae/openminds/LoginActivity.java
// ============================================================
public class LoginActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.applyLocale(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        findViewById(R.id.tvForgotPassword).setOnClickListener(v -> showForgotPasswordDialog());
        findViewById(R.id.tvCreateAccount).setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
        findViewById(R.id.btnLogin).setOnClickListener(v -> login());
    }

    private void login() {
        EditText etEmail    = findViewById(R.id.etLoginEmail);
        EditText etPassword = findViewById(R.id.etLoginPassword);

        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError(getString(R.string.err_invalid_email));
            etEmail.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            etPassword.setError(getString(R.string.err_fill_fields));
            etPassword.requestFocus();
            return;
        }

        Ion.with(this)
                .load("POST", Config.BASE_URL + "login.php")
                .setBodyParameter("email",    email)
                .setBodyParameter("password", password)
                .asString()
                .setCallback((e, result) -> {
                    if (e != null || result == null) {
                        Toast.makeText(this, getString(R.string.err_server), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        JsonObject json   = JsonParser.parseString(result).getAsJsonObject();
                        String     status = json.get("status").getAsString();

                        if (status.equals("success")) {
                            String token     = json.get("token").getAsString();
                            String role      = json.get("role").getAsString();
                            String firstname = json.has("firstname") ? json.get("firstname").getAsString() : "";
                            String lastname  = json.has("lastname")  ? json.get("lastname").getAsString()  : "";

                            // Enregistrer dans SharedPreferences
                            SharedPreferences.Editor editor = getSharedPreferences(Config.PREFS_NAME, MODE_PRIVATE).edit();
                            editor.putString(Config.KEY_TOKEN,     token);
                            editor.putString(Config.KEY_ROLE,      role);
                            editor.putString(Config.KEY_FIRSTNAME, firstname);
                            editor.putString(Config.KEY_LASTNAME,  lastname);
                            editor.putString(Config.KEY_EMAIL,     email);
                            editor.apply();

                            // Planifier la notification quotidienne
                            NotificationReceiver.scheduleDailyNotification(this);

                            startActivity(new Intent(this, MainActivity.class));
                            finish();
                        } else {
                            Toast.makeText(this, getString(R.string.err_wrong_credentials), Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception ex) {
                        Toast.makeText(this, getString(R.string.err_server), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.dialog_forgot_title));
        builder.setMessage(getString(R.string.dialog_forgot_msg));

        EditText etEmail = new EditText(this);
        etEmail.setHint(getString(R.string.hint_email));
        etEmail.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        LinearLayout layout = new LinearLayout(this);
        layout.setPadding(50, 20, 50, 0);
        layout.addView(etEmail, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        builder.setView(layout);

        builder.setPositiveButton(getString(R.string.btn_send), (dialog, which) -> {
            String emailVal = etEmail.getText().toString().trim();
            if (emailVal.isEmpty()) {
                Toast.makeText(this, getString(R.string.err_fill_fields), Toast.LENGTH_SHORT).show();
            } else {
                requestPasswordReset(emailVal);
            }
        });
        builder.setNegativeButton(getString(R.string.btn_cancel), null);
        builder.show();
    }

    private void requestPasswordReset(String email) {
        Ion.with(this)
                .load("POST", Config.BASE_URL + "resetPassword.php")
                .setBodyParameter("email", email)
                .asString()
                .setCallback((e, result) -> {
                    if (e != null || result == null) {
                        Toast.makeText(this, getString(R.string.err_server), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        JsonObject json   = JsonParser.parseString(result).getAsJsonObject();
                        String     status = json.get("status").getAsString();
                        if (status.equals("success")) {
                            String newPass = json.get("new_password").getAsString();
                            new AlertDialog.Builder(this)
                                    .setTitle(getString(R.string.dialog_forgot_title))
                                    .setMessage(getString(R.string.success_temp_password, newPass))
                                    .setPositiveButton(getString(R.string.btn_ok), null)
                                    .show();
                        } else if (status.equals("email_not_found")) {
                            Toast.makeText(this, getString(R.string.err_email_not_found), Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, getString(R.string.err_server), Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception ex) {
                        Toast.makeText(this, getString(R.string.err_server), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
