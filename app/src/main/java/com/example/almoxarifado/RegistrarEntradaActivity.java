package com.example.almoxarifado;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class RegistrarEntradaActivity extends AlmoxActivity {
    String codigo;
    TextView selected;
    TextInputEditText qty, obs;
    TextInputLayout qtyTil;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        gate(AppData.Perfil.ALMOXARIFE);
        if (isFinishing()) return;
        setContentView(R.layout.activity_registrar_entrada);
        toolbar(R.id.toolbar);
        codigo = getIntent().getStringExtra(CODIGO);
        selected = findViewById(R.id.tvItemSelecionado);
        qty = findViewById(R.id.etQuantidade);
        obs = findViewById(R.id.etObservacao);
        qtyTil = findViewById(R.id.tilQuantidade);
        findViewById(R.id.btnEscolherItem).setOnClickListener(v -> choose());
        findViewById(R.id.btnMenos).setOnClickListener(v -> qty.setText(String.valueOf(Math.max(1, number() - 1))));
        findViewById(R.id.btnMais).setOnClickListener(v -> qty.setText(String.valueOf(number() + 1)));
        findViewById(R.id.btnRegistrar).setOnClickListener(v -> save());
        if (b != null) {
            codigo = b.getString("codigo", codigo);
            qty.setText(b.getString("qtd", "1"));
            obs.setText(b.getString("obs", ""));
        }
        render();
        if (codigo == null) choose();
    }

    int number() {
        try {
            return Integer.parseInt(qty.getText().toString());
        } catch (Exception e) {
            return 1;
        }
    }

    void choose() {
        new android.app.AlertDialog.Builder(this).setItems(new String[]{"Ler código de barras", "Escolher da lista"}, (d, n) -> {
            Intent i = new Intent(this, n == 0 ? ScannerActivity.class : ListaItensActivity.class);
            i.putExtra(MODO, ESCOLHER_ITEM);
            startActivityForResult(i, 51);
        }).show();
    }

    void render() {
        AppData.Item i = AppData.RepositoryProvider.ITENS.buscar(codigo);
        selected.setText(i == null ? "Nenhum item selecionado" : i.descricao + " · " + location(i) + " · Saldo atual: " + i.saldo);
    }

    void save() {
        int n;
        try {
            n = Integer.parseInt(qty.getText().toString().trim());
            if (n < 1) throw new Exception();
        } catch (Exception e) {
            qtyTil.setError("Informe uma quantidade maior que zero.");
            return;
        }
        if (codigo == null) {
            snack("Selecione um item.");
            return;
        }
        findViewById(R.id.btnRegistrar).setEnabled(false);
        AppData.Resultado<AppData.Item> r = AppData.RepositoryProvider.ITENS.entrada(codigo, n, user, obs.getText().toString());
        findViewById(R.id.btnRegistrar).setEnabled(true);
        if (r.sucesso()) {
            snack("Entrada registrada.");
            finish();
        } else snack(r.erro);
    }

    @Override
    protected void onActivityResult(int r, int c, Intent d) {
        super.onActivityResult(r, c, d);
        if (c == RESULT_OK && d != null) {
            codigo = d.getStringExtra(CODIGO);
            render();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle b) {
        super.onSaveInstanceState(b);
        b.putString("codigo", codigo);
        b.putString("qtd", qty.getText().toString());
        b.putString("obs", obs.getText().toString());
    }
}
