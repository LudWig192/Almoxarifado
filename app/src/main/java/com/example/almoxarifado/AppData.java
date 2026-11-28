package com.example.almoxarifado;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Camada de dados do app, agora ligada ao Firebase (Auth + Firestore).
 * As operações de rede são assíncronas: quem chama recebe o resultado por Callback.
 */
public final class AppData {
    private AppData() {
    }

    // [REGRA-NEGOCIO] Status nunca é gravado: saldo == 0 ZERADO; 0 < saldo < minimo ABAIXO_MINIMO; saldo >= minimo EM_ESTOQUE.
    public static StatusEstoque status(Item i) {
        return i.status();
    }

    public static String normalizar(String s) {
        return Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
    }

    public enum Perfil {ALMOXARIFE, SOLICITANTE}

    public enum StatusEstoque {ZERADO, ABAIXO_MINIMO, EM_ESTOQUE}

    public enum StatusRequisicao {PENDENTE, ATENDIDA}

    public enum TipoMovimento {ENTRADA, SAIDA}

    // ------------------------------------------------------------------
    // Contratos
    // ------------------------------------------------------------------

    public interface Callback<T> {
        void ok(T valor);

        void erro(String mensagem);
    }

    public interface AuthRepository {
        void entrar(String email, String senha, Callback<Usuario> cb);

        void cadastrar(String nome, String email, String senha, Callback<Usuario> cb);

        // Chamar ao abrir o app. ok(null) significa "ninguém logado".
        void restaurar(Callback<Usuario> cb);

        void sair();

        Usuario sessao();

        AppData.Resultado<Usuario> entrar(String em, String pw);
    }

    public interface ItemRepository {
        LiveData<List<Item>> observar();

        // Lê do cache mantido pelo listener (não faz rede).
        Item buscar(String codigo);

        void salvar(Item i, boolean edicao, Callback<Void> cb);

        void entrada(String codigo, int qtd, Usuario u, String obs, Callback<Void> cb);
    }

    public interface RequisicaoRepository {
        // Todas as requisições (uso do almoxarife).
        LiveData<List<Requisicao>> observar();

        // Somente as do solicitante (uso do solicitante).
        LiveData<List<Requisicao>> observarDoSolicitante(String uid);

        void criar(String codigo, int qtd, String destino, String obs, Usuario u, Callback<Void> cb);

        void atender(String id, Usuario u, Callback<Void> cb);
    }

    public interface MovimentoRepository {
        LiveData<List<Movimento>> observar();

        // Últimos movimentos de um item (use limite 3 na ficha).
        LiveData<List<Movimento>> observarPorItem(String codigo, int limite);
    }

    // ------------------------------------------------------------------
    // Modelos
    // ------------------------------------------------------------------

    public static class Item {
        public String codigoBarras, descricao, unidade, local;
        public int saldo, minimo;

        public Item(String c, String d, String u, String l, int s, int m) {
            codigoBarras = c;
            descricao = d;
            unidade = u;
            local = l;
            saldo = s;
            minimo = m;
        }

        public StatusEstoque status() {
            return saldo == 0 ? StatusEstoque.ZERADO : saldo < minimo ? StatusEstoque.ABAIXO_MINIMO : StatusEstoque.EM_ESTOQUE;
        }
    }

    public static class Usuario {
        public String uid, nome, email;
        public Perfil perfil;

        public Usuario(String i, String n, String e, Perfil p) {
            uid = i;
            nome = n;
            email = e;
            perfil = p;
        }
    }

    public static class Requisicao {
        public String id, itemId, itemDescricao, destino, observacao, solicitanteUid, solicitanteNome, atendidoPorUid;
        public int quantidade;
        public long data, atendidoEm;
        public StatusRequisicao status = StatusRequisicao.PENDENTE;
    }

    public static class Movimento {
        public String id, itemId, uid, usuarioNome, destino;
        public TipoMovimento tipo;
        public int quantidade;
        public long data;
    }

    // ------------------------------------------------------------------
    // Provedor
    // ------------------------------------------------------------------

    public static final class RepositoryProvider {
        public static final AuthRepository AUTH = new FirebaseAuthRepository();
        public static final ItemRepository ITENS = new FirestoreItemRepository();
        public static final RequisicaoRepository REQUISICOES = new FirestoreRequisicaoRepository();
        public static final MovimentoRepository MOVIMENTOS = new FirestoreMovimentoRepository();
    }

    // ------------------------------------------------------------------
    // Utilitários de Firestore
    // ------------------------------------------------------------------

    private static FirebaseFirestore db() {
        return FirebaseFirestore.getInstance();
    }

    private static long num(DocumentSnapshot d, String campo) {
        Long v = d.getLong(campo);
        return v == null ? 0 : v;
    }

    private static long tempo(DocumentSnapshot d, String campo) {
        Timestamp t = d.getTimestamp(campo, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE);
        return t == null ? 0 : t.toDate().getTime();
    }

    // Erro de regra de negócio dentro de transação: guarda a mensagem e aborta sem repetir.
    private static FirebaseFirestoreException negocio(String[] erro, String msg) {
        erro[0] = msg;
        return new FirebaseFirestoreException(msg, FirebaseFirestoreException.Code.FAILED_PRECONDITION);
    }

    private static void falha(Exception e, String[] erroNegocio, Callback<?> cb) {
        if (erroNegocio[0] != null) cb.erro(erroNegocio[0]);
        else cb.erro("Erro de conexão. Tente novamente.");
    }

    private static Map<String, Object> mapaMovimento(String itemId, TipoMovimento tipo, long qtd, String uid, String nome, String destino) {
        Map<String, Object> m = new HashMap<>();
        m.put("itemId", itemId);
        m.put("tipo", tipo.name());
        m.put("quantidade", qtd);
        m.put("uid", uid);
        m.put("usuarioNome", nome);
        m.put("destino", destino);
        m.put("data", FieldValue.serverTimestamp());
        return m;
    }

    private static Item itemDe(DocumentSnapshot d) {
        return new Item(d.getId(), d.getString("descricao"), d.getString("unidade"), d.getString("local"),
                (int) num(d, "saldo"), (int) num(d, "minimo"));
    }

    private static Requisicao requisicaoDe(DocumentSnapshot d) {
        Requisicao r = new Requisicao();
        r.id = d.getId();
        r.itemId = d.getString("itemId");
        r.itemDescricao = d.getString("itemDescricao");
        r.destino = d.getString("destino");
        r.observacao = d.getString("observacao");
        r.solicitanteUid = d.getString("solicitanteUid");
        r.solicitanteNome = d.getString("solicitanteNome");
        r.atendidoPorUid = d.getString("atendidoPorUid");
        r.quantidade = (int) num(d, "quantidade");
        r.data = tempo(d, "data");
        r.atendidoEm = tempo(d, "atendidoEm");
        try {
            String s = d.getString("status");
            if (s != null) r.status = StatusRequisicao.valueOf(s);
        } catch (IllegalArgumentException ignorado) {
            r.status = StatusRequisicao.PENDENTE;
        }
        return r;
    }

    private static Movimento movimentoDe(DocumentSnapshot d) {
        Movimento m = new Movimento();
        m.id = d.getId();
        m.itemId = d.getString("itemId");
        m.uid = d.getString("uid");
        m.usuarioNome = d.getString("usuarioNome");
        m.destino = d.getString("destino");
        m.quantidade = (int) num(d, "quantidade");
        m.data = tempo(d, "data");
        try {
            String t = d.getString("tipo");
            if (t != null) m.tipo = TipoMovimento.valueOf(t);
        } catch (IllegalArgumentException ignorado) {
            m.tipo = null;
        }
        return m;
    }

    private interface Mapeador<T> {
        T de(DocumentSnapshot d);
    }

    /**
     * LiveData ligada a uma consulta do Firestore. O listener só existe enquanto houver
     * alguém observando (liga em onActive, desliga em onInactive), então não vaza.
     */
    private static final class ConsultaLiveData<T> extends LiveData<List<T>> {
        private final Query query;
        private final Mapeador<T> mapeador;
        private final Comparator<T> ordem;
        private final int limite;
        private ListenerRegistration registro;

        ConsultaLiveData(Query q, Mapeador<T> m, Comparator<T> o, int limite) {
            this.query = q;
            this.mapeador = m;
            this.ordem = o;
            this.limite = limite;
        }

        @Override
        protected void onActive() {
            registro = query.addSnapshotListener((snap, e) -> {
                if (e != null || snap == null) return;
                ArrayList<T> lista = new ArrayList<>();
                for (DocumentSnapshot d : snap.getDocuments()) lista.add(mapeador.de(d));
                if (ordem != null) lista.sort(ordem);
                if (limite > 0 && lista.size() > limite) lista = new ArrayList<>(lista.subList(0, limite));
                setValue(lista);
            });
        }

        @Override
        protected void onInactive() {
            if (registro != null) {
                registro.remove();
                registro = null;
            }
        }
    }

    // ------------------------------------------------------------------
    // Auth  [FIREBASE-AUTH] [FIRESTORE-WRITE] [PERFIL]
    // ------------------------------------------------------------------

    public static final class FirebaseAuthRepository implements AuthRepository {
        private final FirebaseAuth auth = FirebaseAuth.getInstance();
        private Usuario atual;

        @Override
        public void entrar(String email, String senha, Callback<Usuario> cb) {
            auth.signInWithEmailAndPassword(email.trim(), senha)
                    .addOnSuccessListener(r -> carregarUsuario(r.getUser(), cb))
                    .addOnFailureListener(e -> cb.erro(traduzir(e)));
        }

        @Override
        public void cadastrar(String nome, String email, String senha, Callback<Usuario> cb) {
            String emailLimpo = email.trim().toLowerCase(Locale.ROOT);
            auth.createUserWithEmailAndPassword(emailLimpo, senha)
                    .addOnSuccessListener(r -> {
                        String uid = r.getUser().getUid();
                        Map<String, Object> dados = new HashMap<>();
                        dados.put("nome", nome);
                        dados.put("email", emailLimpo);
                        // sem "perfil", de propósito
                        db().collection("usuarios").document(uid).set(dados)
                                .addOnSuccessListener(v -> {
                                    auth.signOut(); // sem perfil, não fica logado
                                    atual = null;
                                    cb.ok(new Usuario(uid, nome, emailLimpo, null));
                                })
                                .addOnFailureListener(e -> cb.erro("Erro ao salvar usuário. Tente novamente."));
                    })
                    .addOnFailureListener(e -> cb.erro(traduzir(e)));
        }

        @Override
        public void restaurar(Callback<Usuario> cb) {
            FirebaseUser u = auth.getCurrentUser();
            if (u == null) {
                cb.ok(null);
                return;
            }
            carregarUsuario(u, cb);
        }

        // Lê usuarios/{uid}; sem perfil válido, desloga e avisa.
        private void carregarUsuario(FirebaseUser u, Callback<Usuario> cb) {
            db().collection("usuarios").document(u.getUid()).get()
                    .addOnSuccessListener(doc -> {
                        Perfil perfil = null;
                        try {
                            String p = doc.getString("perfil");
                            if (p != null) perfil = Perfil.valueOf(p);
                        } catch (IllegalArgumentException ignorado) {
                            // valor inválido conta como "sem perfil"
                        }
                        if (perfil == null) {
                            auth.signOut();
                            atual = null;
                            cb.erro("Seu usuário ainda não tem perfil. Procure o responsável.");
                            return;
                        }
                        atual = new Usuario(u.getUid(), doc.getString("nome"), doc.getString("email"), perfil);
                        cb.ok(atual);
                    })
                    .addOnFailureListener(e -> {
                        auth.signOut();
                        atual = null;
                        cb.erro("Erro ao ler o perfil. Tente novamente.");
                    });
        }

        @Override
        public void sair() {
            auth.signOut();
            atual = null;
        }

        @Override
        public Usuario sessao() {
            return atual;
        }

        private String traduzir(Exception e) {
            if (e instanceof FirebaseAuthUserCollisionException) return "Este e-mail já está cadastrado.";
            if (e instanceof FirebaseAuthWeakPasswordException) return "A senha deve ter pelo menos 6 caracteres.";
            if (e instanceof FirebaseAuthInvalidCredentialsException
                    || e instanceof FirebaseAuthInvalidUserException) return "E-mail ou senha incorretos.";
            return "Erro de conexão. Tente novamente.";
        }
    }

    // ------------------------------------------------------------------
    // Itens  [FIRESTORE-LISTENER] [FIRESTORE-READ] [FIRESTORE-WRITE] [FIRESTORE-TRANSACAO]
    // ------------------------------------------------------------------

    public static final class FirestoreItemRepository implements ItemRepository {
        // Ordenado por descrição (sem acento e sem diferenciar maiúsculas), no cliente.
        private final ConsultaLiveData<Item> itens = new ConsultaLiveData<>(
                db().collection("itens"),
                AppData::itemDe,
                Comparator.comparing(x -> normalizar(x.descricao)),
                0);

        @Override
        public LiveData<List<Item>> observar() {
            return itens;
        }

        @Override
        public Item buscar(String codigo) {
            List<Item> lista = itens.getValue();
            if (lista == null || codigo == null) return null;
            for (Item i : lista) if (codigo.equals(i.codigoBarras)) return i;
            return null;
        }

        @Override
        public void salvar(Item i, boolean edicao, Callback<Void> cb) {
            DocumentReference ref = db().collection("itens").document(i.codigoBarras);

            if (edicao) {
                // Edição nunca mexe no saldo.
                ref.update("descricao", i.descricao, "unidade", i.unidade, "local", i.local, "minimo", (long) i.minimo)
                        .addOnSuccessListener(v -> cb.ok(null))
                        .addOnFailureListener(e -> cb.erro("Erro ao salvar o item. Tente novamente."));
                return;
            }

            String[] erro = new String[1];
            db().runTransaction(tx -> {
                        if (tx.get(ref).exists())
                            throw negocio(erro, "Já existe um item com este código.");

                        Map<String, Object> m = new HashMap<>();
                        m.put("codigoBarras", i.codigoBarras);
                        m.put("descricao", i.descricao);
                        m.put("unidade", i.unidade);
                        m.put("local", i.local);
                        m.put("saldo", (long) i.saldo);
                        m.put("minimo", (long) i.minimo);
                        tx.set(ref, m);

                        if (i.saldo > 0) {
                            AuthRepository auth = RepositoryProvider.AUTH;
                            Usuario u = auth.sessao();
                            tx.set(db().collection("movimentos").document(),
                                    mapaMovimento(i.codigoBarras, TipoMovimento.ENTRADA, i.saldo,
                                            u == null ? null : u.uid, u == null ? null : u.nome, "Saldo inicial"));
                        }
                        return null;
                    }).addOnSuccessListener(v -> cb.ok(null))
                    .addOnFailureListener(e -> falha(e, erro, cb));
        }

        @Override
        public void entrada(String codigo, int qtd, Usuario u, String obs, Callback<Void> cb) {
            if (qtd < 1) {
                cb.erro("Informe uma quantidade maior que zero.");
                return;
            }
            DocumentReference ref = db().collection("itens").document(codigo);
            String[] erro = new String[1];
            db().runTransaction(tx -> {
                        DocumentSnapshot item = tx.get(ref);
                        if (!item.exists()) throw negocio(erro, "Item não encontrado.");
                        tx.update(ref, "saldo", num(item, "saldo") + qtd);
                        tx.set(db().collection("movimentos").document(),
                                mapaMovimento(codigo, TipoMovimento.ENTRADA, qtd, u.uid, u.nome, obs));
                        return null;
                    }).addOnSuccessListener(v -> cb.ok(null))
                    .addOnFailureListener(e -> falha(e, erro, cb));
        }
    }

    // ------------------------------------------------------------------
    // Requisições
    // ------------------------------------------------------------------

    public static final class FirestoreRequisicaoRepository implements RequisicaoRepository {
        private static final Comparator<Requisicao> POR_DATA = Comparator.comparingLong(x -> x.data);

        private final ConsultaLiveData<Requisicao> todas = new ConsultaLiveData<>(
                db().collection("requisicoes"), AppData::requisicaoDe, POR_DATA, 0);

        @Override
        public LiveData<List<Requisicao>> observar() {
            return todas;
        }

        @Override
        public LiveData<List<Requisicao>> observarDoSolicitante(String uid) {
            return new ConsultaLiveData<>(
                    db().collection("requisicoes").whereEqualTo("solicitanteUid", uid),
                    AppData::requisicaoDe, POR_DATA, 0);
        }

        // [REGRA-NEGOCIO] Não bloqueia pedido acima do saldo; isso só é validado no atendimento.
        @Override
        public void criar(String codigo, int qtd, String destino, String obs, Usuario u, Callback<Void> cb) {
            db().collection("itens").document(codigo).get()
                    .addOnSuccessListener(item -> {
                        if (!item.exists()) {
                            cb.erro("Selecione um item.");
                            return;
                        }
                        Map<String, Object> r = new HashMap<>();
                        r.put("itemId", codigo);
                        r.put("itemDescricao", item.getString("descricao"));
                        r.put("quantidade", (long) qtd);
                        r.put("destino", destino);
                        r.put("observacao", obs);
                        r.put("solicitanteUid", u.uid);
                        r.put("solicitanteNome", u.nome);
                        r.put("status", StatusRequisicao.PENDENTE.name());
                        r.put("data", FieldValue.serverTimestamp());
                        db().collection("requisicoes").add(r)
                                .addOnSuccessListener(ref -> cb.ok(null))
                                .addOnFailureListener(e -> cb.erro("Erro ao enviar o pedido. Tente novamente."));
                    })
                    .addOnFailureListener(e -> cb.erro("Erro de conexão. Tente novamente."));
        }

        // Atendimento: tudo em UMA transação (valida saldo, baixa saldo, cria SAIDA, atualiza requisição).
        @Override
        public void atender(String id, Usuario u, Callback<Void> cb) {
            DocumentReference reqRef = db().collection("requisicoes").document(id);
            String[] erro = new String[1];
            db().runTransaction(tx -> {
                        DocumentSnapshot r = tx.get(reqRef);
                        if (!r.exists() || !StatusRequisicao.PENDENTE.name().equals(r.getString("status")))
                            throw negocio(erro, "Esta requisição já foi atendida.");

                        String itemId = r.getString("itemId");
                        long qtd = num(r, "quantidade");
                        DocumentReference itemRef = db().collection("itens").document(itemId);
                        DocumentSnapshot item = tx.get(itemRef);
                        long saldo = item.exists() ? num(item, "saldo") : 0;

                        if (saldo < qtd)
                            throw negocio(erro, "Saldo insuficiente|" + r.getString("itemDescricao")
                                    + ": pedido de " + qtd + ", saldo de " + saldo
                                    + ". Registre uma entrada antes de atender.");

                        tx.update(itemRef, "saldo", saldo - qtd);
                        tx.set(db().collection("movimentos").document(),
                                mapaMovimento(itemId, TipoMovimento.SAIDA, qtd, u.uid, u.nome, r.getString("destino")));
                        tx.update(reqRef,
                                "status", StatusRequisicao.ATENDIDA.name(),
                                "atendidoPorUid", u.uid,
                                "atendidoEm", FieldValue.serverTimestamp());
                        return null;
                    }).addOnSuccessListener(v -> cb.ok(null))
                    .addOnFailureListener(e -> falha(e, erro, cb));
        }
    }

    // ------------------------------------------------------------------
    // Movimentos  [FIRESTORE-LISTENER]
    // ------------------------------------------------------------------

    public static final class FirestoreMovimentoRepository implements MovimentoRepository {
        private static final Comparator<Movimento> RECENTES_PRIMEIRO = (x, y) -> Long.compare(y.data, x.data);

        private final ConsultaLiveData<Movimento> todos = new ConsultaLiveData<>(
                db().collection("movimentos"), AppData::movimentoDe, RECENTES_PRIMEIRO, 0);

        @Override
        public LiveData<List<Movimento>> observar() {
            return todos;
        }

        // Filtra por item e ordena/limita no cliente (assim não precisa criar índice no Firestore).
        @Override
        public LiveData<List<Movimento>> observarPorItem(String codigo, int limite) {
            return new ConsultaLiveData<>(
                    db().collection("movimentos").whereEqualTo("itemId", codigo),
                    AppData::movimentoDe, RECENTES_PRIMEIRO, limite);
        }
    }

    // ------------------------------------------------------------------
    // ViewModel
    // ------------------------------------------------------------------

    public static class AlmoxViewModel extends ViewModel {
        public LiveData<List<Item>> itens = RepositoryProvider.ITENS.observar();
        public LiveData<List<Requisicao>> requisicoes = RepositoryProvider.REQUISICOES.observar();
        public LiveData<List<Movimento>> movimentos = RepositoryProvider.MOVIMENTOS.observar();
        public String codigoSelecionado, destino, observacao, filtro, busca;
        public int quantidade = 1;
    }
}
