package com.example.almoxarifado;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.google.android.material.appbar.MaterialToolbar;

public class HomeSolicitanteActivity extends AlmoxActivity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        gate(AppData.Perfil.SOLICITANTE);
        if (isFinishing()) return;
        setContentView(R.layout.activity_home_solicitante);
        MaterialToolbar t = findViewById(R.id.toolbar);
        t.setOnMenuItemClickListener(m -> {
            if (m.getItemId() == R.id.action_sair) {
                new androidx.appcompat.app.AlertDialog.Builder(this).setMessage("Deseja sair?").setNegativeButton("Cancelar", null).setPositiveButton("Sair", (d, w) -> {
                    AppData.RepositoryProvider.AUTH.sair();
                    getSharedPreferences("almox_sessao", MODE_PRIVATE).edit().clear().apply();
                    Intent i = new Intent(this, LoginActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                }).show();
                return true;
            }
            return false;
        });
        ((TextView) findViewById(R.id.tvSaudacao)).setText("Olá, " + user.nome.split(" ")[0]);
        findViewById(R.id.btnLerCodigo).setOnClickListener(v -> {
            Intent i = new Intent(this, ScannerActivity.class);
            i.putExtra(MODO, ABRIR_FICHA);
            startActivity(i);
        });
        findViewById(R.id.cardItens).setOnClickListener(v -> go(ListaItensActivity.class));
        findViewById(R.id.cardNova).setOnClickListener(v -> go(NovaRequisicaoActivity.class));
        findViewById(R.id.cardMinhas).setOnClickListener(v -> go(MinhasRequisicoesActivity.class));
        vm.requisicoes.observe(this, rs -> {
            long c = rs.stream().filter(r -> r.solicitanteUid.equals(user.uid) && r.status == AppData.StatusRequisicao.PENDENTE).count();
            ((TextView) findViewById(R.id.tvBadge)).setText(String.valueOf(c));
        });
    }
}
