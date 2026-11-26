package com.example.almoxarifado;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.appbar.MaterialToolbar;

public class HomeAlmoxarifeActivity extends AlmoxActivity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        gate(AppData.Perfil.ALMOXARIFE);
        if (isFinishing()) return;
        setContentView(R.layout.activity_home_almoxarife);
        MaterialToolbar t = findViewById(R.id.toolbar);
        t.setOnMenuItemClickListener(m -> {
            if (m.getItemId() == R.id.action_sair) {
                new androidx.appcompat.app.AlertDialog.Builder(this).setMessage("Deseja sair?").setNegativeButton("Cancelar", null).setPositiveButton("Sair", (d, w) -> logout()).show();
                return true;
            }
            return false;
        });
        findViewById(R.id.btnLerCodigo).setOnClickListener(v -> scan(ABRIR_FICHA));
        findViewById(R.id.cardItens).setOnClickListener(v -> go(ListaItensActivity.class));
        findViewById(R.id.cardEntrada).setOnClickListener(v -> go(RegistrarEntradaActivity.class));
        findViewById(R.id.cardPendentes).setOnClickListener(v -> go(AtendimentoActivity.class));
        findViewById(R.id.cardAbaixoMinimo).setOnClickListener(v -> go(AbaixoMinimoActivity.class));
        findViewById(R.id.cardUltimaRequisicao).setOnClickListener(v -> go(AtendimentoActivity.class));
        ((TextView) findViewById(R.id.tvPerfil)).setText("Almoxarife");
        ((TextView) findViewById(R.id.tvSaudacao)).setText("Olá, " + user.nome.split(" ")[0]);
        vm.requisicoes.observe(this, rs -> {
            long p = rs.stream().filter(x -> x.status == AppData.StatusRequisicao.PENDENTE).count();
            badge(R.id.tvBadgePendentes, p);
            if (!rs.isEmpty()) {
                AppData.Requisicao r = rs.get(rs.size() - 1);
                AppData.Item i = AppData.RepositoryProvider.ITENS.buscar(r.itemId);
                findViewById(R.id.cardUltimaRequisicao).setVisibility(View.VISIBLE);
                ((TextView) findViewById(R.id.tvUltimaItem)).setText(r.itemDescricao);
                ((TextView) findViewById(R.id.tvUltimaDetalhe)).setText(r.destino + " · " + r.quantidade + " " + (i == null ? "" : i.unidade));
                ((TextView) findViewById(R.id.tvUltimaStatus)).setText(r.status == AppData.StatusRequisicao.PENDENTE ? "Pendente" : "Atendida");
            } else findViewById(R.id.cardUltimaRequisicao).setVisibility(View.GONE);
        });
        vm.itens.observe(this, is -> badge(R.id.tvBadgeAbaixoMinimo, is.stream().filter(i -> i.saldo < i.minimo).count()));
    }

    void badge(int id, long n) {
        TextView x = findViewById(id);
        x.setText(String.valueOf(n));
        x.setVisibility(n == 0 ? View.GONE : View.VISIBLE);
    }

    void scan(String mode) {
        Intent i = new Intent(this, ScannerActivity.class);
        i.putExtra(MODO, mode);
        startActivity(i);
    }

    void logout() {
        AppData.RepositoryProvider.AUTH.sair();
        getSharedPreferences("almox_sessao", MODE_PRIVATE).edit().clear().apply();
        Intent i = new Intent(this, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
    }
}
