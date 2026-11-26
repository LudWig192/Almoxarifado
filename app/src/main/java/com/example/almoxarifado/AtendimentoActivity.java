package com.example.almoxarifado;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class AtendimentoActivity extends AlmoxActivity {
    RequisicaoAdapter ad;
    android.os.Handler handler = new android.os.Handler();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        gate(AppData.Perfil.ALMOXARIFE);
        if (isFinishing()) return;
        setContentView(R.layout.activity_atendimento);
        toolbar(R.id.toolbar);
        RecyclerView rv = findViewById(R.id.rvRequisicoes);
        rv.setLayoutManager(new LinearLayoutManager(this));
        ad = new RequisicaoAdapter(r -> {
            AppData.Resultado<AppData.Requisicao> x = AppData.RepositoryProvider.REQUISICOES.atender(r.id, user);
            if (x.sucesso()) snack("Requisição atendida.");
            else if (x.erro.startsWith("Saldo insuficiente|")) {
                String[] p = x.erro.split("\\|", 2);
                ((TextView) findViewById(R.id.tvErroTitulo)).setText(p[0]);
                ((TextView) findViewById(R.id.tvErroMensagem)).setText(p[1]);
                View c = findViewById(R.id.cardErro);
                c.setVisibility(View.VISIBLE);
                handler.removeCallbacksAndMessages(null);
                handler.postDelayed(() -> c.setVisibility(View.GONE), 5000);
            } else snack(x.erro);
        }, false);
        rv.setAdapter(ad);
        vm.requisicoes.observe(this, rs -> {
            java.util.ArrayList<AppData.Requisicao> p = new java.util.ArrayList<>();
            for (AppData.Requisicao r : rs)
                if (r.status == AppData.StatusRequisicao.PENDENTE) p.add(r);
            ad.set(p);
            ((TextView) findViewById(R.id.tvContador)).setText(String.valueOf(p.size()));
            findViewById(R.id.rvRequisicoes).setVisibility(p.isEmpty() ? View.GONE : View.VISIBLE);
            findViewById(R.id.layoutVazio).setVisibility(p.isEmpty() ? View.VISIBLE : View.GONE);
        });
        findViewById(R.id.cardErro).setOnClickListener(v -> v.setVisibility(View.GONE));
    }
}
