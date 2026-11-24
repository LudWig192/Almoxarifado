package com.example.almoxarifado;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class CadastroItemActivity extends AlmoxActivity {
    boolean editando;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        gate(AppData.Perfil.ALMOXARIFE);
        if (isFinishing()) return;
        setContentView(R.layout.activity_cadastro_item);
        toolbar(R.id.toolbar);
        AutoCompleteTextView u = findViewById(R.id.actUnidade);
        String[] units = {"UN", "CX", "PAR", "KG", "M", "L"};
        u.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, units));
        String code = getIntent().getStringExtra(CODIGO);
        AppData.Item old = code == null ? null : AppData.RepositoryProvider.ITENS.buscar(code);
        if (old != null) {
            editando = true;
            ((com.google.android.material.appbar.MaterialToolbar) findViewById(R.id.toolbar)).setTitle("Editar item");
            ((TextInputEditText) findViewById(R.id.etCodigo)).setText(old.codigoBarras);
            findViewById(R.id.etCodigo).setEnabled(false);
            ((TextInputEditText) findViewById(R.id.etDescricao)).setText(old.descricao);
            u.setText(old.unidade, false);
            ((TextInputEditText) findViewById(R.id.etLocal)).setText(old.local);
            ((TextInputEditText) findViewById(R.id.etSaldo)).setText(String.valueOf(old.saldo));
            findViewById(R.id.etSaldo).setEnabled(false);
            ((TextInputEditText) findViewById(R.id.etMinimo)).setText(String.valueOf(old.minimo));
        } else if (code != null) ((TextInputEditText) findViewById(R.id.etCodigo)).setText(code);
        ((TextInputLayout) findViewById(R.id.tilCodigo)).setEndIconOnClickListener(v -> {
            Intent i = new Intent(this, ScannerActivity.class);
            i.putExtra(MODO, ESCOLHER_ITEM);
            startActivityForResult(i, 62);
        });
        findViewById(R.id.btnSalvar).setOnClickListener(v -> save());
    }

    String s(int id) {
        return String.valueOf(((TextInputEditText) findViewById(id)).getText()).trim();
    }

    void error(int id, String m) {
        ((TextInputLayout) findViewById(id)).setError(m);
    }

    void save() {
        String c = s(R.id.etCodigo), d = s(R.id.etDescricao), u = ((AutoCompleteTextView) findViewById(R.id.actUnidade)).getText().toString(), l = s(R.id.etLocal);
        if (c.isEmpty() || !c.matches("\\d+")) {
            error(R.id.tilCodigo, "Informe um código válido.");
            return;
        }
        if (d.isEmpty()) {
            error(R.id.tilDescricao, "Informe a descrição.");
            return;
        }
        if (u.isEmpty()) {
            error(R.id.tilUnidade, "Escolha a unidade.");
            return;
        }
        if (l.isEmpty()) {
            error(R.id.tilLocal, "Informe a localização.");
            return;
        }
        int saldo, min;
        try {
            saldo = Integer.parseInt(s(R.id.etSaldo));
            min = Integer.parseInt(s(R.id.etMinimo));
            if (saldo < 0 || min < 0) throw new Exception();
        } catch (Exception ex) {
            error(R.id.tilSaldo, "Informe um número válido.");
            return;
        }
        AppData.Item item = new AppData.Item(c, d, u, l, saldo, min);
        AppData.Resultado<AppData.Item> r = AppData.RepositoryProvider.ITENS.salvar(item, editando);
        if (!r.sucesso()) {
            error(R.id.tilCodigo, r.erro);
            return;
        }
        snack(editando ? "Item atualizado." : "Item cadastrado.");
        finish();
    }

    @Override
    protected void onActivityResult(int r, int c, Intent d) {
        super.onActivityResult(r, c, d);
        if (c == RESULT_OK && d != null)
            ((TextInputEditText) findViewById(R.id.etCodigo)).setText(d.getStringExtra(CODIGO));
    }
}
