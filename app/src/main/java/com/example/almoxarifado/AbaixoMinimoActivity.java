package com.example.almoxarifado;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.LinearProgressIndicator;

public class AbaixoMinimoActivity extends AlmoxActivity {
    RecyclerView rv;
    LinearLayoutManager lm;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        gate(AppData.Perfil.ALMOXARIFE);
        if (isFinishing()) return;
        setContentView(R.layout.activity_abaixo_minimo);
        toolbar(R.id.toolbar);
        rv = findViewById(R.id.rvAbaixoMinimo);
        lm = new LinearLayoutManager(this);
        rv.setLayoutManager(lm);
        vm.itens.observe(this, x -> render(x));
    }

    void render(java.util.List<AppData.Item> all) {
        java.util.ArrayList<AppData.Item> a = new java.util.ArrayList<>();
        for (AppData.Item i : all) if (i.saldo < i.minimo) a.add(i);
        a.sort((x, y) -> {
            if (x.saldo == 0 && y.saldo != 0) return -1;
            if (y.saldo == 0 && x.saldo != 0) return 1;
            return Double.compare((double) x.saldo / x.minimo, (double) y.saldo / y.minimo);
        });
        rv.setAdapter(new RecyclerView.Adapter<H>() {
            public H onCreateViewHolder(ViewGroup p, int t) {
                return new H(LayoutInflater.from(p.getContext()).inflate(R.layout.item_abaixo_minimo, p, false));
            }

            public void onBindViewHolder(H h, int n) {
                AppData.Item i = a.get(n);
                h.name.setText(i.descricao);
                h.local.setText(location(i));
                h.status.setText(i.saldo == 0 ? "Zerado" : "Abaixo do mínimo");
                StatusChipHelper.aplicar(h.status, i.status());
                h.progress.setProgress(i.minimo == 0 ? 100 : Math.min(100, i.saldo * 100 / i.minimo));
                h.progress.setIndicatorColor(getColor(i.saldo == 0 ? R.color.erro : R.color.alerta_texto));
                h.prop.setText(i.saldo + " / " + i.minimo);
                h.button.setOnClickListener(v -> goCode(RegistrarEntradaActivity.class, i.codigoBarras));
            }

            public int getItemCount() {
                return a.size();
            }
        });
        boolean empty = a.isEmpty();
        findViewById(R.id.rvAbaixoMinimo).setVisibility(empty ? View.GONE : View.VISIBLE);
        findViewById(R.id.layoutVazio).setVisibility(empty ? View.VISIBLE : View.GONE);
        findViewById(R.id.cardResumo).setVisibility(empty ? View.GONE : View.VISIBLE);
        ((TextView) findViewById(R.id.tvResumoTitulo)).setText(a.size() == 1 ? "1 item precisa de reposição" : a.size() + " itens precisam de reposição");
    }

    static class H extends RecyclerView.ViewHolder {
        TextView name, local, status, prop;
        LinearProgressIndicator progress;
        View button;

        H(View v) {
            super(v);
            name = v.findViewById(R.id.tvNome);
            local = v.findViewById(R.id.tvLocal);
            status = v.findViewById(R.id.tvStatus);
            prop = v.findViewById(R.id.tvProporcao);
            progress = v.findViewById(R.id.progressSaldo);
            button = v.findViewById(R.id.btnRegistrarEntrada);
        }
    }
}
