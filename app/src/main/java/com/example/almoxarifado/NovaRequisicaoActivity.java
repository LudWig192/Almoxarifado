package com.example.almoxarifado;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class NovaRequisicaoActivity extends AlmoxActivity {
    TextView itemName, itemLocal, saldoInfo;
    TextInputLayout qtyLayout, destLayout;
    TextInputEditText qty, obs;
    AutoCompleteTextView dest;
    String codigo;
    boolean loading;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (user == null || user.perfil != AppData.Perfil.SOLICITANTE) {
            gate(AppData.Perfil.SOLICITANTE);
            return;
        }
        setContentView(R.layout.activity_nova_requisicao);
        toolbar(R.id.toolbar);
        codigo = getIntent().getStringExtra(CODIGO);
        itemName = findViewById(R.id.tvItemNome);
        itemLocal = findViewById(R.id.tvItemLocal);
        saldoInfo = findViewById(R.id.tvSaldoDisponivel);
        qtyLayout = findViewById(R.id.tilQuantidade);
        destLayout = findViewById(R.id.tilDestino);
        qty = findViewById(R.id.etQuantidade);
        obs = findViewById(R.id.etObservacao);
        dest = findViewById(R.id.actDestino);
        String[] ds = {"Linha de montagem 1", "Linha de montagem 2", "Linha de montagem 3", "Manutenção", "Expedição"};
        dest.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, ds));
        findViewById(R.id.btnMais).setOnClickListener(v -> qty.setText(String.valueOf(parseQty() + 1)));
        findViewById(R.id.btnMenos).setOnClickListener(v -> qty.setText(String.valueOf(Math.max(1, parseQty() - 1))));
        findViewById(R.id.btnTrocarItem).setOnClickListener(v -> new android.app.AlertDialog.Builder(this).setItems(new String[]{"Ler código de barras", "Escolher da lista"}, (d, w) -> {
            Intent i = new Intent(this, w == 0 ? ScannerActivity.class : ListaItensActivity.class);
            i.putExtra(MODO, ESCOLHER_ITEM);
            startActivityForResult(i, 77);
        }).show());
        findViewById(R.id.tvItemNome).setOnClickListener(v -> {
            Intent i = new Intent(this, ListaItensActivity.class);
            i.putExtra(MODO, ESCOLHER_ITEM);
            startActivityForResult(i, 77);
        });
        findViewById(R.id.btnEnviarRequisicao).setOnClickListener(v -> send());
        if (codigo == null) {
            if (b != null) codigo = b.getString("codigo");
            if (codigo == null) {
                Intent i = new Intent(this, ListaItensActivity.class);
                i.putExtra(MODO, ESCOLHER_ITEM);
                startActivityForResult(i, 77);
            }
        } else render();
        if (b != null) {
            qty.setText(b.getString("qtd", "1"));
            dest.setText(b.getString("dest", ""));
            obs.setText(b.getString("obs", ""));
        }
    }

    int parseQty() {
        try {
            return Integer.parseInt(qty.getText().toString());
        } catch (Exception e) {
            return 1;
        }
    }

    void render() {
        AppData.Item i = AppData.RepositoryProvider.ITENS.buscar(codigo);
        if (i == null) {
            itemName.setText("Nenhum item selecionado");
            itemLocal.setText("");
            saldoInfo.setText("");
            return;
        }
        itemName.setText(i.descricao);
        itemLocal.setText(location(i));
        saldoInfo.setText("Saldo disponível: " + i.saldo + " " + i.unidade.toLowerCase());
    }

    void send() {
        qtyLayout.setError(null);
        destLayout.setError(null);
        int n;
        try {
            n = Integer.parseInt(qty.getText().toString().trim());
            if (n <= 0) throw new Exception();
        } catch (Exception ex) {
            qtyLayout.setError("Informe uma quantidade maior que zero.");
            return;
        }
        if (codigo == null || AppData.RepositoryProvider.ITENS.buscar(codigo) == null) {
            snack("Selecione um item.");
            return;
        }
        if (dest.getText().toString().trim().isEmpty()) {
            destLayout.setError("Escolha o destino.");
            return;
        }
        View v = findViewById(R.id.btnEnviarRequisicao);
        v.setEnabled(false);
        AppData.Resultado<AppData.Requisicao> r = AppData.RepositoryProvider.REQUISICOES.criar(codigo, n, dest.getText().toString(), obs.getText().toString(), user);
        v.setEnabled(true);
        if (!r.sucesso()) {
            snack(r.erro);
            return;
        }
        snack("Requisição enviada.");
        finish();
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
        b.putString("dest", dest.getText().toString());
        b.putString("obs", obs.getText().toString());
    }
}
