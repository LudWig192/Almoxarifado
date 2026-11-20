package com.example.almoxarifado.App;

import static android.content.Context.MODE_PRIVATE;

import android.content.Intent;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;

import com.example.almoxarifado.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AlmoxActivity {
    TextInputLayout email, senha;
    TextInputEditText e, s;
    com.google.android.material.button.MaterialButton entrar;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_login);
        if (user == null) {
            String uid = getSharedPreferences("almox_sessao", MODE_PRIVATE).getString("uid", null);
            if (uid != null) user = AppData.RepositoryProvider.AUTH.restaurar(uid);
        }
        if (user != null) {
            route(user);
            if (!isFinishing() && AppData.RepositoryProvider.AUTH.sessao() == null) user = null;
            else return;
        }
        email = findVi
        email = findViewById(R.id.tilEmail);
        senha = findViewById(R.id.tilSenha);
        e = findViewById(R.id.etEmail);
        s = findViewById(R.id.etSenha);
        entrar = findViewById(R.id.btnEntrar);
        e.addTextChangedListener(new SimpleWatcher(() -> email.setError(null)));
        s.addTextChangedListener(new SimpleWatcher(() -> senha.setError(null)));
        entrar.setOnClickListener(v -> login());
        findViewById(R.id.btnCriarConta).setOnClickListener(v -> go(CadastroContaActivity.class));
        e.setOnEditorActionListener((v, a, event) -> {
            s.requestFocus();
            return true;
        });
        s.setOnEditorActionListener((v, a, event) -> {
            if (a == EditorInfo.IME_ACTION_DONE) login();
            return false;
        });
    }

    void login() {
        String em = e.getText() == null ? "" : e.getText().toString().trim(), pw = s.getText() == null ? "" : s.getText().toString();
        if (em.isEmpty()) {
            email.setError("Informe o e-mail.");
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(em).matches()) {
            email.setError("E-mail inválido.");
            return;
        }
        if (pw.isEmpty()) {
            senha.setError("Informe a senha.");
            return;
        }
        if (pw.length() < 6) {
            senha.setError("A senha deve ter pelo menos 6 caracteres.");
            return;
        }
        entrar.setEnabled(false);
        AppData.Resultado<AppData.Usuario> r = AppData.RepositoryProvider.AUTH.entrar(em, pw);
        entrar.setEnabled(true);
        if (!r.sucesso()) {
            snack(r.erro);
            return;
        }
        route(r.valor);
    }

    void route(AppData.Usuario u) {
        if (u.perfil == null) {
            AppData.RepositoryProvider.AUTH.sair();
            getSharedPreferences("almox_sessao", MODE_PRIVATE).edit().clear().apply();
            snack("Seu perfil de acesso ainda não foi liberado. Fale com o administrador.");
            return;
        }
        getSharedPreferences("almox_sessao", MODE_PRIVATE).edit().putString("uid", u.uid).apply();
        Intent i = new Intent(this, u.perfil == AppData.Perfil.ALMOXARIFE ? HomeAlmoxarifeActivity.class : HomeSolicitanteActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
    }
}

class SimpleWatcher implements android.text.TextWatcher {
    final Runnable r;

    SimpleWatcher(Runnable x) {
        r = x;
    }

    public void beforeTextChanged(CharSequence s, int st, int c, int a) {
    }

    public void onTextChanged(CharSequence s, int st, int before, int count) {
        r.run();
    }

    public void afterTextChanged(android.text.Editable e) {
    }
}