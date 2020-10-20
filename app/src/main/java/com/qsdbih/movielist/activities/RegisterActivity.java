package com.qsdbih.movielist.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.qsdbih.movielist.R;

import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {

    //Constants for edittexts, so it's easier to change them later on if needed
    private static final int FULLNAME_MINIMUM_SIZE = 3;
    private static final int PASSWORD_MINIMUM_SIZE = 6;

    private static final String TAG = "RegisterActivity";
    TextInputEditText etFullname, etEmail, etPassword;
    TextInputLayout layoutFullname, layoutEmail, layoutPassword;
    TextView tvLogin;
    TextView txtEula;
    CheckBox cbEULA, cbAge;
    ImageButton btnBack;
    MaterialButton btnRegister;
    private FirebaseAuth mAuth;
    FirebaseUser user;
    boolean errorFullname = false; // Once an error has been made once, it adds validation after every character you enter, as you type
    boolean errorEmail = false;
    boolean errorPassword = false;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.StartingTheme);
        super.onCreate(savedInstanceState);
        Objects.requireNonNull(getSupportActionBar()).hide();
        setContentView(R.layout.activity_register);

        btnRegister = findViewById(R.id.btnRegister);
        etFullname = findViewById(R.id.etFullname);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        layoutFullname = findViewById(R.id.layoutFullname);
        layoutEmail = findViewById(R.id.layoutEmail);
        layoutPassword = findViewById(R.id.layoutPassword);
        cbEULA = findViewById(R.id.cbEULA);
        cbAge = findViewById(R.id.cbAge);
        btnBack = findViewById(R.id.btnBack);
        tvLogin = findViewById(R.id.tvRegister1);
        txtEula = findViewById(R.id.txtEula);
        progressBar = findViewById(R.id.progressBar);

        tvLogin.setOnClickListener(v -> {
            onBackPressed();
            finish();
        });
        btnBack.setOnClickListener(v -> {
            onBackPressed();
            finish();
        });

        txtEula.setOnClickListener(view -> {
            String url = getString(R.string.eula_link);
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        });

        etFullname.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                if (errorFullname)
                    etFullname.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                        }

                        @Override
                        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                        }

                        @Override
                        public void afterTextChanged(Editable editable) {
                            if (editable.toString().length() >= FULLNAME_MINIMUM_SIZE) {
                                LoginActivity.changeOutlineToSuccess(layoutFullname);
                            } else {
                                LoginActivity.changeOutlineToError(layoutFullname, getString(R.string.fullname_smaller_than_x_characters));
                            }
                        }
                    });
            } else {
                if (!Objects.requireNonNull(etFullname.getText()).toString().equals("")) {
                    if (Objects.requireNonNull(etFullname.getText()).toString().length() < FULLNAME_MINIMUM_SIZE) {
                        LoginActivity.changeOutlineToError(layoutFullname, getString(R.string.fullname_smaller_than_x_characters));
                        errorFullname = true;
                    } else {
                        LoginActivity.changeOutlineToSuccess(layoutFullname);
                    }
                } else {
                    layoutFullname.setError(null);
                }
            }
        });


        etEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                if (errorEmail)
                    etEmail.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                        }

                        @Override
                        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                        }

                        @Override
                        public void afterTextChanged(Editable editable) {
                            if (isEmailValid(editable.toString())) {
                                LoginActivity.changeOutlineToSuccess(layoutEmail);
                            } else {
                                LoginActivity.changeOutlineToError(layoutEmail, getString(R.string.invalid_email_address));
                            }
                        }
                    });
            } else {
                if (!Objects.requireNonNull(etEmail.getText()).toString().equals("")) {
                    if (!isEmailValid(Objects.requireNonNull(etEmail.getText()).toString())) {
                        LoginActivity.changeOutlineToError(layoutEmail, getString(R.string.invalid_email_address));
                        errorEmail = true;
                    } else {
                        LoginActivity.changeOutlineToSuccess(layoutEmail);
                    }
                } else {
                    layoutEmail.setError(null);
                }
            }
        });

        etPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                if (errorPassword)
                    etPassword.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                        }

                        @Override
                        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                        }

                        @Override
                        public void afterTextChanged(Editable editable) {
                            if (editable.toString().length() >= PASSWORD_MINIMUM_SIZE) {
                                LoginActivity.changeOutlineToSuccess(layoutPassword);
                            } else {
                                LoginActivity.changeOutlineToError(layoutPassword, getString(R.string.you_need_to_enter_a_password));
                            }
                        }
                    });
            } else {
                if (!Objects.requireNonNull(etPassword.getText()).toString().equals("")) {
                    if (Objects.requireNonNull(etPassword.getText()).toString().length() < PASSWORD_MINIMUM_SIZE) {
                        LoginActivity.changeOutlineToError(layoutPassword, getString(R.string.password_length_x_characters));
                        errorPassword = true;
                    } else {
                        LoginActivity.changeOutlineToSuccess(layoutPassword);
                    }
                } else {
                    layoutPassword.setError(null);
                }
            }
        });


        mAuth = FirebaseAuth.getInstance();

        cbAge.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                cbAge.setButtonDrawable(R.drawable.ic_check_fill);
            } else {
                cbAge.setButtonDrawable(R.drawable.ic_check_outline);
            }
        });

        cbEULA.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                txtEula.clearFocus();
                cbEULA.setButtonDrawable(R.drawable.ic_check_fill);
            } else {
                cbEULA.setButtonDrawable(R.drawable.ic_check_outline);
            }
        });

        btnRegister.setOnClickListener(v -> {
            v.requestFocus();
            String fullname = Objects.requireNonNull(etFullname.getText()).toString();
            String email = Objects.requireNonNull(etEmail.getText()).toString();
            String password = Objects.requireNonNull(etPassword.getText()).toString();

            if (!fullname.equals("") && !email.equals("") && isEmailValid(email) && password.length() > 5 && cbEULA.isChecked() && cbAge.isChecked()) {
                progressBar.setVisibility(View.VISIBLE);
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                createAccount(email, password, fullname);
            }
            if (password.length() < PASSWORD_MINIMUM_SIZE) {
                errorPassword = true;
                LoginActivity.changeOutlineToError(layoutPassword, getString(R.string.password_length_x_characters));
            }
            if (!isEmailValid(email)) {
                errorEmail = true;
                LoginActivity.changeOutlineToError(layoutEmail, getString(R.string.invalid_email_format));
            }
            if (fullname.length() < FULLNAME_MINIMUM_SIZE) {
                errorFullname = true;
                LoginActivity.changeOutlineToError(layoutFullname, getString(R.string.enter_full_name));
            }
            if (!cbAge.isChecked()) {
                cbAge.setError(getString(R.string.checkbox_age_text));
            } else {
                cbAge.setError(null);
            }
            if (!cbEULA.isChecked()) {
                txtEula.requestFocus();
                txtEula.setCompoundDrawables(null, null, getDrawable(R.drawable.ic_baseline_error_24), null);
                txtEula.setError(getString(R.string.agree_to_eula_checkbox));

            } else {
                txtEula.clearFocus();
                txtEula.setError(null);
                txtEula.setCompoundDrawables(null, null, null, null);
            }
        }); //Order in which they are written matters. When cbEula is last, it will show the error message for it

        etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                //Clear focus here from edittext
                etPassword.clearFocus();
            }
            return false;
        });
    }

    public static boolean isEmailValid(CharSequence email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void createAccount(String email, String password, final String fullname) {

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "createUserWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        UserProfileChangeRequest upcr = new UserProfileChangeRequest.Builder().setDisplayName(fullname).build();
                        Objects.requireNonNull(user).updateProfile(upcr);
                        Toast toast = Toast.makeText(getApplicationContext(), R.string.successful_registration, Toast.LENGTH_SHORT);
                        toast.setMargin(50, 50);
                        toast.show();
                        progressBar.setVisibility(View.GONE);
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                        finish();
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        Toast.makeText(RegisterActivity.this, getString(R.string.authentication_failed),
                                Toast.LENGTH_SHORT).show();
                        LoginActivity.changeOutlineToError(layoutEmail, getString(R.string.invalid_email_address));
                        progressBar.setVisibility(View.GONE);
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    }
                });
        user = FirebaseAuth.getInstance().getCurrentUser();
    }
}