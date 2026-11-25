package com.example.almoxarifado;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class FichaItemActivity extends AlmoxActivity {
    String codigo;
    TextView nome, saldo, status;
    MovimentoAdapter ad;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_ficha_item);
        toolbar(R.id.toolbar);
        codigo = getIntent().getStringExtra(CODIGO);
        nome = findViewById(R.id.tvNome);
        saldo = findViewById(R.id.tvSaldo);
        status = findViewById(R.id.tvStatus);
        if (codigo == null || AppData.RepositoryProvider.ITENS.buscar(codigo) == null) {
            snack("Item não encontrado.");
            finish();
            return;
        }
        if (user != null && user.perfil == AppData.Perfil.ALMOXARIFE) {
            findViewById(R.id.btnRegistrarEntrada).setVisibility(View.VISIBLE);
            findViewById(R.id.btnRegistrarEntrada).setOnClickListener(v -> goCode(RegistrarEntradaActivity.class, codigo));
            findViewById(R.id.btnVerTudo).setOnClickListener(v -> goCode(HistoricoMovimentosActivity.class, codigo));
        } else {
            findViewById(R.id.btnPedirItem).setVisibility(View.VISIBLE);
            findViewById(R.id.btnPedirItem).setOnClickListener(v -> goCode(NovaRequisicaoActivity.class, codigo));
            findViewById(R.id.sectionHistorico).setVisibility(View.GONE);
            findViewById(R.id.cardHistorico).setVisibility(View.GONE);
        }
        RecyclerView rv = findViewById(R.id.rvMovimentos);
        rv.setLayoutManager(new LinearLayoutManager(this));
        ad = new MovimentoAdapter();
        rv.setAdapter(ad);
        vm.itens.observe(this, x -> render());
        vm.movimentos.observe(this, x -> ad.set(AppData.RepositoryProvider.MOVIMENTOS.porItem(codigo).subList(0, Math.min(3, AppData.RepositoryProvider.MOVIMENTOS.porItem(codigo).size()))));
    }

    void render() {
        AppData.Item i = AppData.RepositoryProvider.ITENS.buscar(codigo);
        if (i == null) {
            finish();
            return;
        }
        nome.setText(i.descricao);
        ((TextView) findViewById(R.id.tvCodigo)).setText(i.codigoBarras);
        saldo.setText(String.valueOf(i.saldo));
        saldo.setTextColor(getColor(i.status() == AppData.StatusEstoque.EM_ESTOQUE ? R.color.sucesso : i.status() == AppData.StatusEstoque.ZERADO ? R.color.erro : R.color.alerta_texto));
        StatusChipHelper.aplicar(status, i.status());
        ((TextView) findViewById(R.id.tvUnidade)).setText(i.unidade);
        ((TextView) findViewById(R.id.tvLocal)).setText(i.local);
        ((TextView) findViewById(R.id.tvMinimo)).setText(String.valueOf(i.minimo));
    }
}