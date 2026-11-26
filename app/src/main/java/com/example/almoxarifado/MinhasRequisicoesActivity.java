package com.example.almoxarifado;

import android.os.Bundle;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MinhasRequisicoesActivity extends AlmoxActivity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        gate(AppData.Perfil.SOLICITANTE);
        if (isFinishing()) return;
        setContentView(R.layout.activity_minhas_requisicoes);
        toolbar(R.id.toolbar);
        RecyclerView rv = findViewById(R.id.rvRequisicoes);
        rv.setLayoutManager(new LinearLayoutManager(this));
        RequisicaoAdapter a = new RequisicaoAdapter(r -> {
        }, true);
        rv.setAdapter(a);
        vm.requisicoes.observe(this, rs -> {
            java.util.ArrayList<AppData.Requisicao> mine = new java.util.ArrayList<>();
            for (AppData.Requisicao r : rs) if (r.solicitanteUid.equals(user.uid)) mine.add(r);
            java.util.Collections.reverse(mine);
            a.set(mine);
            rv.setVisibility(mine.isEmpty() ? View.GONE : View.VISIBLE);
            findViewById(R.id.layoutVazio).setVisibility(mine.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }
}
