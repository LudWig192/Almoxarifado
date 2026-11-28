package com.example.almoxarifado;

import android.os.Bundle;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class CadastroContaActivity extends AlmoxActivity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_cadastro_conta);
        toolbar(R.id.toolbar);
        findViewById(R.id.btnCriar).setOnClickListener(v -> save());
    }

    void save() {
        String n = text(R.id.etNome), e = text(R.id.etEmail), p = text(R.id.etSenha), c = text(R.id.etConfirmar);
        if (n.isEmpty()) {
            err(R.id.tilNome, "Informe o nome.");
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(e).matches()) {
            err(R.id.tilEmail, "E-mail inválido.");
            return;
        }
        if (p.length() < 6) {
            err(R.id.tilSenha, "A senha deve ter pelo menos 6 caracteres.");
            return;
        }
        if (!p.equals(c)) {
            err(R.id.tilConfirmar, "As senhas não conferem.");
            return;
        }
        AppData.Resultado<AppData.Usuario> r = AppData.RepositoryProvider.AUTH.cadastrar(n, e, p);
        if (!r.sucesso()) {
            err(R.id.tilEmail, r.erro);
            return;
        }
        androidx.appcompat.app.AlertDialog d = new androidx.appcompat.app.AlertDialog.Builder(this).setMessage("Conta criada. Aguarde a liberação do perfil pelo administrador.").setPositiveButton("Voltar ao login", (x, w) -> finish()).create();
        d.show();
    }

    String text(int id) {
        return String.valueOf(((TextInputEditText) findViewById(id)).getText()).trim();
    }

    void err(int id, String s) {
        ((TextInputLayout) findViewById(id)).setError(s);
    }
}