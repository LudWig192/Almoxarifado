package com.example.almoxarifado;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class ListaItensActivity extends AlmoxActivity {
    ItemAdapter adapter;
    String filtro = "TODOS";
    boolean escolher;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_lista_itens);
        toolbar(R.id.toolbar);
        escolher = ESCOLHER_ITEM.equals(getIntent().getStringExtra(MODO));
        RecyclerView rv = findViewById(R.id.rvItens);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ItemAdapter(i -> {
            if (escolher) {
                setResult(RESULT_OK, new Intent().putExtra(CODIGO, i.codigoBarras));
                finish();
            } else goCode(FichaItemActivity.class, i.codigoBarras);
        });
        rv.setAdapter(adapter);
        if (user != null && user.perfil == AppData.Perfil.SOLICITANTE) {
            findViewById(R.id.fabNovoItem).setVisibility(View.GONE);
            findViewById(R.id.btnCadastrarVazio).setVisibility(View.GONE);
        }
        View.OnClickListener create = v -> go(CadastroItemActivity.class);
        findViewById(R.id.fabNovoItem).setOnClickListener(create);
        findViewById(R.id.btnCadastrarVazio).setOnClickListener(create);
        ((TextInputLayout) findViewById(R.id.tilBusca)).setEndIconOnClickListener(v -> scan());
        ((ChipGroup) findViewById(R.id.chipGroupFiltro)).setOnCheckedStateChangeListener((g, ids) -> {
            int id = ids.isEmpty() ? R.id.chipTodos : ids.get(0);
            filtro = id == R.id.chipZerados ? "ZERADOS" : id == R.id.chipAbaixoMinimo ? "ABAIXO" : "TODOS";
            render();
        });
        ((TextInputEditText) findViewById(R.id.etBusca)).addTextChangedListener(new SimpleWatcher(this::render));
        vm.itens.observe(this, x -> render());
    }

    void scan() {
        Intent i = new Intent(this, ScannerActivity.class);
        i.putExtra(MODO, ESCOLHER_ITEM);
        startActivityForResult(i, 99);
    }

    void render() {
        List<AppData.Item> all = AppData.RepositoryProvider.ITENS.observar().getValue();
        if (all == null) all = new ArrayList<>();
        String q = AppData.normalizar(String.valueOf(((TextInputEditText) findViewById(R.id.etBusca)).getText()));
        ArrayList<AppData.Item> out = new ArrayList<>();
        for (AppData.Item i : all) {
            boolean match = AppData.normalizar(i.descricao).contains(q) || i.codigoBarras.contains(q);
            if (filtro.equals("ZERADOS") && i.saldo != 0) match = false;
            if (filtro.equals("ABAIXO") && i.saldo >= i.minimo) match = false;
            if (match) out.add(i);
        }
        adapter.set(out);
        boolean empty = out.isEmpty();
        findViewById(R.id.rvItens).setVisibility(empty ? View.GONE : View.VISIBLE);
        findViewById(R.id.layoutVazio).setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) {
            TextView tv = (TextView) ((android.view.ViewGroup) findViewById(R.id.layoutVazio)).getChildAt(1);
            tv.setText(all.isEmpty() ? "Nenhum item cadastrado" : "Nenhum item encontrado");
            findViewById(R.id.btnCadastrarVazio).setVisibility(all.isEmpty() && user != null && user.perfil == AppData.Perfil.ALMOXARIFE ? View.VISIBLE : View.GONE);
        }
    }
}
