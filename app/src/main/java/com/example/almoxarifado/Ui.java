package com.example.almoxarifado;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

class StatusChipHelper {
    static void aplicar(TextView t, AppData.StatusEstoque s) {
        int bg = s == AppData.StatusEstoque.EM_ESTOQUE ? R.color.sucesso : s == AppData.StatusEstoque.ZERADO ? R.color.erro_fundo : R.color.alerta_fundo;
        int fg = s == AppData.StatusEstoque.EM_ESTOQUE ? R.color.branco : s == AppData.StatusEstoque.ZERADO ? R.color.erro : R.color.alerta_texto;
        t.setBackgroundTintList(android.content.res.ColorStateList.valueOf(t.getResources().getColor(bg, t.getContext().getTheme())));
        t.setTextColor(t.getResources().getColor(fg, t.getContext().getTheme()));
        t.setText(s == AppData.StatusEstoque.EM_ESTOQUE ? "Em estoque" : s == AppData.StatusEstoque.ZERADO ? "Zerado" : "Abaixo do mínimo");
    }
}

abstract class AlmoxActivity extends AppCompatActivity {
    static final String CODIGO = "CODIGO", MODO = "MODO", ESCOLHER_ITEM = "ESCOLHER_ITEM", ABRIR_FICHA = "ABRIR_FICHA";
    AppData.AlmoxViewModel vm;
    AppData.Usuario user;

    static String location(AppData.Item i) {
        return "PRATELEIRA " + i.local + " · " + i.unidade;
    }

    static String when(long t, String fmt) {
        return new SimpleDateFormat(fmt, new Locale("pt", "BR")).format(new Date(t));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        vm = new ViewModelProvider(this).get(AppData.AlmoxViewModel.class);
        user = AppData.RepositoryProvider.AUTH.sessao();
    }

    void toolbar(int id) {
        MaterialToolbar t = findViewById(id);
        if (t != null) t.setNavigationOnClickListener(v -> finish());
    }

    // TODO [REGRA-SEGURANCA] A checagem de perfil da UI só controla navegação; as Firestore Rules devem validar usuarios/{uid}.perfil em cada leitura e gravação protegida.
    void gate(AppData.Perfil p) {
        if (user == null || user.perfil != p) {
            Toast.makeText(this, "Você não tem acesso a esta tela.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    void snack(String s) {
        Snackbar.make(findViewById(android.R.id.content), s, Snackbar.LENGTH_LONG).show();
    }

    void go(Class<?> c) {
        startActivity(new Intent(this, c));
    }

    void goCode(Class<?> c, String code) {
        Intent i = new Intent(this, c);
        i.putExtra(CODIGO, code);
        startActivity(i);
    }
}

class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.H> {
    final List<AppData.Item> data = new ArrayList<>();
    final Click click;
    ItemAdapter(Click c) {
        click = c;
    }

    void set(List<AppData.Item> x) {
        data.clear();
        data.addAll(x);
        notifyDataSetChanged();
    }

    public H onCreateViewHolder(ViewGroup p, int v) {
        return new H(LayoutInflater.from(p.getContext()).inflate(R.layout.item_item, p, false));
    }

    public void onBindViewHolder(H h, int n) {
        AppData.Item i = data.get(n);
        h.name.setText(i.descricao);
        h.local.setText(AlmoxActivity.location(i));
        h.saldo.setText(String.valueOf(i.saldo));
        h.saldo.setTextColor(h.saldo.getResources().getColor(i.status() == AppData.StatusEstoque.EM_ESTOQUE ? R.color.sucesso : i.status() == AppData.StatusEstoque.ZERADO ? R.color.erro : R.color.alerta_texto, h.saldo.getContext().getTheme()));
        StatusChipHelper.aplicar(h.status, i.status());
        if (i.status() == AppData.StatusEstoque.ABAIXO_MINIMO) h.status.setText("Mín. " + i.minimo);
        h.itemView.setOnClickListener(v -> click.item(i));
    }

    public int getItemCount() {
        return data.size();
    }

    interface Click {
        void item(AppData.Item i);
    }

    static class H extends RecyclerView.ViewHolder {
        TextView name, local, saldo, status;

        H(View v) {
            super(v);
            name = v.findViewById(R.id.tvNome);
            local = v.findViewById(R.id.tvLocal);
            saldo = v.findViewById(R.id.tvSaldo);
            status = v.findViewById(R.id.tvStatus);
        }
    }
}

class MovimentoAdapter extends RecyclerView.Adapter<MovimentoAdapter.H> {
    final List<AppData.Movimento> data = new ArrayList<>();

    void set(List<AppData.Movimento> x) {
        data.clear();
        data.addAll(x);
        notifyDataSetChanged();
    }

    public H onCreateViewHolder(ViewGroup p, int x) {
        return new H(LayoutInflater.from(p.getContext()).inflate(R.layout.item_movimento, p, false));
    }

    public void onBindViewHolder(H h, int n) {
        AppData.Movimento m = data.get(n);
        h.tipo.setText(m.tipo == AppData.TipoMovimento.ENTRADA ? "Entrada" : "Saída · " + m.destino);
        h.data.setText(AlmoxActivity.when(m.data, "dd/MM") + " · " + m.usuarioNome);
        h.qtd.setText((m.tipo == AppData.TipoMovimento.ENTRADA ? "+" : "−") + m.quantidade);
        h.qtd.setTextColor(h.qtd.getResources().getColor(m.tipo == AppData.TipoMovimento.ENTRADA ? R.color.sucesso : R.color.erro, h.qtd.getContext().getTheme()));
    }

    public int getItemCount() {
        return data.size();
    }

    static class H extends RecyclerView.ViewHolder {
        TextView tipo, data, qtd;

        H(View v) {
            super(v);
            tipo = v.findViewById(R.id.tvTipo);
            data = v.findViewById(R.id.tvDataUsuario);
            qtd = v.findViewById(R.id.tvQuantidade);
        }
    }
}

class RequisicaoAdapter extends RecyclerView.Adapter<RequisicaoAdapter.H> {
    final List<AppData.Requisicao> data = new ArrayList<>();
    final Click click;
    final boolean solicitante;
    RequisicaoAdapter(Click c, boolean s) {
        click = c;
        solicitante = s;
    }

    void set(List<AppData.Requisicao> x) {
        data.clear();
        data.addAll(x);
        notifyDataSetChanged();
    }

    public H onCreateViewHolder(ViewGroup p, int x) {
        return new H(LayoutInflater.from(p.getContext()).inflate(R.layout.item_requisicao, p, false));
    }

    public void onBindViewHolder(H h, int n) {
        AppData.Requisicao r = data.get(n);
        AppData.Item i = AppData.RepositoryProvider.ITENS.buscar(r.itemId);
        h.item.setText(r.itemDescricao);
        h.detail.setText(r.destino + " · " + r.solicitanteNome + " · " + AlmoxActivity.when(r.data, "HH:mm"));
        h.status.setText(r.status == AppData.StatusRequisicao.PENDENTE ? "Pendente" : "Atendida");
        h.status.setBackgroundTintList(android.content.res.ColorStateList.valueOf(h.item.getResources().getColor(r.status == AppData.StatusRequisicao.PENDENTE ? R.color.pendente_fundo : R.color.sucesso, h.item.getContext().getTheme())));
        h.status.setTextColor(h.item.getResources().getColor(r.status == AppData.StatusRequisicao.PENDENTE ? R.color.pendente_texto : R.color.branco, h.item.getContext().getTheme()));
        h.box.setVisibility(solicitante ? View.GONE : View.VISIBLE);
        h.button.setVisibility(solicitante ? View.GONE : View.VISIBLE);
        h.pedido.setText(String.valueOf(r.quantidade));
        h.saldo.setText(String.valueOf(i == null ? 0 : i.saldo));
        if (i != null && i.saldo < r.quantidade) {
            h.box.setBackgroundTintList(android.content.res.ColorStateList.valueOf(h.item.getResources().getColor(R.color.erro_fundo, h.item.getContext().getTheme())));
            h.pedido.setTextColor(Color.RED);
            h.saldo.setTextColor(Color.RED);
        }
        h.button.setOnClickListener(v -> {
            h.button.setEnabled(false);
            click.atender(r);
            h.button.setEnabled(true);
        });
    }

    public int getItemCount() {
        return data.size();
    }

    interface Click {
        void atender(AppData.Requisicao r);
    }

    static class H extends RecyclerView.ViewHolder {
        TextView item, detail, status, pedido, saldo;
        View box;
        MaterialButton button;

        H(View v) {
            super(v);
            item = v.findViewById(R.id.tvItem);
            detail = v.findViewById(R.id.tvDetalhe);
            status = v.findViewById(R.id.tvStatus);
            pedido = v.findViewById(R.id.tvPedido);
            saldo = v.findViewById(R.id.tvSaldo);
            box = v.findViewById(R.id.boxQuantidades);
            button = v.findViewById(R.id.btnAtender);
        }
    }
}