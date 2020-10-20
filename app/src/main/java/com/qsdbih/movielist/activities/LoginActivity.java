package com.qsdbih.movielist.activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.qsdbih.movielist.R;

import java.util.Arrays;
import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    TextInputEditText etEmail, etPassword;
    TextInputLayout layoutEmail, layoutPassword;
    MaterialButton btnFacebook;
    MaterialButton btnLogin;
    TextView btnRegister;
    private FirebaseAuth mAuth;
    GoogleSignInClient mGoogleSignInClient;
    MaterialButton btnGoogle;
    private static final int RC_SIGN_IN = 9003;
    private CallbackManager mCallbackManager;
    boolean errorEmail = false;
    boolean errorPassword = false;
    ProgressBar progressBar;

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null)
            updateUI(currentUser);
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            Intent i = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(i);
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.StartingTheme);
        super.onCreate(savedInstanceState);
        Objects.requireNonNull(getSupportActionBar()).hide();
        setContentView(R.layout.activity_login);

        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        layoutEmail = findViewById(R.id.layoutEmail);
        layoutPassword = findViewById(R.id.layoutPassword);
        btnGoogle = findViewById(R.id.btnGoogle);
        etPassword = findViewById(R.id.etPassword);
        etEmail = findViewById(R.id.etEmail);
        progressBar = findViewById(R.id.progressBar);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        etEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                if (errorEmail) {
                    etEmail.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        }

                        @Override
                        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        }

                        @Override
                        public void afterTextChanged(Editable editable) {
                            if (RegisterActivity.isEmailValid(editable.toString())) {
                                changeOutlineToSuccess(layoutEmail);
                            } else {
                                changeOutlineToError(layoutEmail, getString(R.string.invalid_email_address));
                            }
                        }
                    });
                }
            } else {
                if (!Objects.requireNonNull(etEmail.getText()).toString().equals("")) {
                    if (!RegisterActivity.isEmailValid(Objects.requireNonNull(etEmail.getText()).toString())) {
                        changeOutlineToError(layoutEmail, getString(R.string.invalid_email_address));
                        errorEmail = true;
                    } else {
                        changeOutlineToSuccess(layoutEmail);
                    }
                } else {
                    layoutEmail.setError(null);
                }
            }
        });

        etPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                if (errorPassword) {
                    etPassword.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        }

                        @Override
                        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        }

                        @Override
                        public void afterTextChanged(Editable editable) {
                            if (editable.toString().length() > 0) {
                                changeOutlineToSuccess(layoutPassword);
                            } else {
                                changeOutlineToError(layoutPassword, getString(R.string.you_need_to_enter_a_password));
                            }
                        }
                    });
                }
            } else {
                if (!Objects.requireNonNull(etPassword.getText()).toString().equals("")) {
                    if (Objects.requireNonNull(etPassword.getText()).toString().length() == 0) {
                        changeOutlineToError(layoutPassword, getString(R.string.you_need_to_enter_a_password));
                        errorPassword = true;
                    } else {
                        changeOutlineToSuccess(layoutPassword);
                    }
                } else {
                    layoutPassword.setError(null);
                }
            }
        });

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        mAuth = FirebaseAuth.getInstance();

        btnGoogle.setOnClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            signIn();
        });

        btnLogin.setOnClickListener(v -> {
            String email = Objects.requireNonNull(etEmail.getText()).toString();
            String password = Objects.requireNonNull(etPassword.getText()).toString();
            if (password.length() == 0) {
                changeOutlineToError(layoutPassword, getString(R.string.you_need_to_enter_a_password));
                errorPassword = true;
            }
            if (!RegisterActivity.isEmailValid(email)) {
                changeOutlineToError(layoutEmail, getString(R.string.invalid_email_address));
                errorEmail = true;
            }
            if (RegisterActivity.isEmailValid(email) && password.length() > 0) {
                progressBar.setVisibility(View.VISIBLE);
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                signIn(email, password);
            }
        });

        btnRegister.setOnClickListener(v -> {

            Intent i = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(i);
        });

        // Initialize Facebook Login button
        mCallbackManager = CallbackManager.Factory.create();
        btnFacebook = findViewById(R.id.btnFacebook);
        btnFacebook.setOnClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            LoginManager.getInstance().logInWithReadPermissions(LoginActivity.this, Arrays.asList("email", "public_profile"));
            LoginManager.getInstance().registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
                @Override
                public void onSuccess(LoginResult loginResult) {
                    Log.d(TAG, "facebook:onSuccess:" + loginResult);
                    handleFacebookAccessToken(loginResult.getAccessToken());
                }

                @Override
                public void onCancel() {
                    Log.d(TAG, "facebook:onCancel");
                    updateUI(null);
                    progressBar.setVisibility(View.GONE);
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                }

                @Override
                public void onError(FacebookException error) {
                    Log.d(TAG, "facebook:onError", error);
                    updateUI(null);
                    progressBar.setVisibility(View.GONE);
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                }
            });
        });

        etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                //Clear focus here from edittext
                etPassword.clearFocus();
            }
            return false;
        });
    }

    public static void changeOutlineToError(TextInputLayout textInputLayout, String errorText) {
        textInputLayout.setErrorTextColor(ColorStateList.valueOf(Color.parseColor("#af001f")));
        textInputLayout.setErrorIconDrawable(R.drawable.ic_baseline_error_24);
        textInputLayout.setError(errorText);
    }

    public static void changeOutlineToSuccess(TextInputLayout textInputLayout) {
        textInputLayout.setErrorTextColor(ColorStateList.valueOf(Color.parseColor("#a9d271")));
        textInputLayout.setError(" ");
        textInputLayout.setErrorIconDrawable(null);
    }

    private void handleFacebookAccessToken(AccessToken token) {
        Log.d(TAG, "handleFacebookAccessToken:" + token);

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                // Sign in success, update UI with the signed-in user's information
                Log.d(TAG, "signInWithCredential:success");
                FirebaseUser user = mAuth.getCurrentUser();
                updateUI(user);
            } else {
                // If sign in fails, display a message to the user.
                Log.w(TAG, "signInWithCredential:failure", task.getException());
                Toast.makeText(LoginActivity.this, getString(R.string.authentication_failed), Toast.LENGTH_SHORT).show();
                updateUI(null);
                progressBar.setVisibility(View.GONE);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            }
        });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                // Sign in success, update UI with the signed-in user's information
                Log.d(TAG, "signInWithCredential:success");
                FirebaseUser user = mAuth.getCurrentUser();
                Intent i = new Intent(LoginActivity.this, MainActivity.class);
                i.putExtra("uid", Objects.requireNonNull(user).getUid());
                startActivity(i);
                finish();
            } else {
                Log.w(TAG, "signInWithCredential:failure", task.getException());
                Toast.makeText(LoginActivity.this, getString(R.string.authentication_failed), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onBackPressed() {

        new AlertDialog.Builder(LoginActivity.this)
                .setTitle(R.string.quit_back_pressed)
                .setMessage(R.string.quit_message)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(R.string.login_quit_positive_button, (dialog, whichButton) -> {
                    finish();
                    finishAffinity();
                    super.onBackPressed();
                })
                .setNegativeButton(R.string.login_quit_negative_button, null).show();

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Movie returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
//                progressBar.setVisibility(View.GONE);
//                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "firebaseAuthWithGoogle:" + Objects.requireNonNull(account).getId());
                firebaseAuthWithGoogle(account.getIdToken());

            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e);
                updateUI(null);
                progressBar.setVisibility(View.GONE);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            }
        } else {
            mCallbackManager.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void signIn(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                progressBar.setVisibility(View.GONE);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                Log.d(TAG, "signInWithEmail:success");
                FirebaseUser user = mAuth.getCurrentUser();

                Intent i = new Intent(LoginActivity.this, MainActivity.class);
                i.putExtra("uid", Objects.requireNonNull(user).getUid());
                startActivity(i);
                finish();
            } else {
                progressBar.setVisibility(View.GONE);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                if (RegisterActivity.isEmailValid(etEmail.getText())) {
                    changeOutlineToSuccess(layoutEmail);
                } else {
                    changeOutlineToError(layoutEmail, getString(R.string.invalid_email_address));
                }

                if (RegisterActivity.isEmailValid(etEmail.getText())) {
                    changeOutlineToError(layoutPassword, getString(R.string.invalid_email_or_password));
                } else {
                    changeOutlineToError(layoutPassword, getString(R.string.invalid_password));
                }
                Log.w(TAG, "signInWithEmail:failure", task.getException());
                Toast.makeText(LoginActivity.this, R.string.authentication_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }
}