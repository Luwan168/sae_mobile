package sae.openminds;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.koushikdutta.ion.Ion;

import sae.openminds.utils.LocaleHelper;

// ============================================================
//  app/src/main/java/sae/openminds/RegisterActivity.java
// ============================================================
public class RegisterActivity extends AppCompatActivity {

    private EditText etFirstname, etLastname, etEmail, etPassword, etPhone;
    private Spinner  spinnerPrefix;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.applyLocale(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        etFirstname = findViewById(R.id.etFirstname);
        etLastname  = findViewById(R.id.etLastname);
        etEmail     = findViewById(R.id.etEmail);
        etPassword  = findViewById(R.id.etPassword);
        etPhone     = findViewById(R.id.etPhone);
        spinnerPrefix = findViewById(R.id.spinnerPrefix);

        String[] prefixes = {"+33 (FR)", "+32 (BE)", "+41 (CH)", "+44 (UK)", "+1 (US)", "+34 (ES)"};
        spinnerPrefix.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, prefixes));

        Button btnRegister = findViewById(R.id.btnRegister);
        btnRegister.setOnClickListener(v -> registerUser());

        findViewById(R.id.tvBackToLogin).setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void registerUser() {
        String firstname = etFirstname.getText().toString().trim();
        String lastname  = etLastname.getText().toString().trim();
        String email     = etEmail.getText().toString().trim();
        String password  = etPassword.getText().toString().trim();
        String prefix    = spinnerPrefix.getSelectedItem().toString();
        String phone     = prefix + " " + etPhone.getText().toString().trim();

        if (firstname.isEmpty() || lastname.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, getString(R.string.err_fill_fields), Toast.LENGTH_SHORT).show();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError(getString(R.string.err_invalid_email));
            return;
        }
        if (password.length() < 8) {
            etPassword.setError(getString(R.string.err_password_length));
            return;
        }

        Ion.with(this)
                .load("POST", Config.BASE_URL + "register.php")
                .setBodyParameter("firstname", firstname)
                .setBodyParameter("lastname",  lastname)
                .setBodyParameter("email",     email)
                .setBodyParameter("password",  password)
                .setBodyParameter("phone",     phone)
                .asString()
                .setCallback((e, result) -> {
                    if (e != null || result == null) {
                        Toast.makeText(this, getString(R.string.err_server), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        JsonObject json   = JsonParser.parseString(result).getAsJsonObject();
                        String     status = json.get("status").getAsString();
                        switch (status) {
                            case "success":
                                Toast.makeText(this, getString(R.string.register_success), Toast.LENGTH_LONG).show();
                                startActivity(new Intent(this, LoginActivity.class));
                                finish();
                                break;
                            case "email_exists":
                                etEmail.setError(getString(R.string.err_email_exists));
                                break;
                            default:
                                Toast.makeText(this, getString(R.string.err_server), Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception ex) {
                        Toast.makeText(this, getString(R.string.err_server), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
