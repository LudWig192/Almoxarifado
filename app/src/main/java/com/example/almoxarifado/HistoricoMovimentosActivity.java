package com.example.almoxarifado;

import android.os.Bundle;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class HistoricoMovimentosActivity extends AlmoxActivity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        gate(AppData.Perfil.ALMOXARIFE);
        if (isFinishing()) return;
        setContentView(R.layout.activity_historico_movimentos);
        toolbar(R.id.toolbar);
        String code = getIntent().getStringExtra(CODIGO);
        AppData.Item i = AppData.RepositoryProvider.ITENS.buscar(code);
        if (i == null) {
            snack("Item não encontrado.");
            finish();
            return;
        }
        ((com.google.android.material.appbar.MaterialToolbar) findViewById(R.id.toolbar)).setTitle("Histórico: " + i.descricao);
        RecyclerView rv = findViewById(R.id.rvMovimentos);
        rv.setLayoutManager(new LinearLayoutManager(this));
        MovimentoAdapter a = new MovimentoAdapter();
        rv.setAdapter(a);
        vm.movimentos.observe(this, x -> {
            java.util.List<AppData.Movimento> m = AppData.RepositoryProvider.MOVIMENTOS.porItem(code);
            a.set(m);
            rv.setVisibility(m.isEmpty() ? View.GONE : View.VISIBLE);
            findViewById(R.id.layoutVazio).setVisibility(m.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }
}
