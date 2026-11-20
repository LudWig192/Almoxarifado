package com.example.almoxarifado;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Dados falsos compartilhados pelas telas; substituir por implementações Firestore via RepositoryProvider.
 */
public final class AppData {
    private AppData() {
    }

    // TODO [REGRA-NEGOCIO] Status nunca deve ser gravado: saldo == 0 ZERADO; 0 < saldo < minimo ABAIXO_MINIMO; saldo >= minimo EM_ESTOQUE.
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

    public interface AuthRepository {
        Resultado<Usuario> entrar(String email, String senha);

        Resultado<Usuario> cadastrar(String nome, String email, String senha);

        void sair();

        Usuario sessao();
    }

    public interface ItemRepository {
        LiveData<List<Item>> observar();

        Item buscar(String codigo);

        Resultado<Item> salvar(Item i, boolean edicao);

        Resultado<Item> entrada(String codigo, int qtd, Usuario u, String obs);
    }

    public interface RequisicaoRepository {
        LiveData<List<Requisicao>> observar();

        Resultado<Requisicao> criar(String codigo, int qtd, String destino, String obs, Usuario u);

        Resultado<Requisicao> atender(String id, Usuario u);
    }

    public interface MovimentoRepository {
        LiveData<List<Movimento>> observar();

        List<Movimento> porItem(String codigo);
    }

    public static class Item {
        public String codigoBarras, descricao, unidade, local;
        public int saldo, minimo;

        Item(String c, String d, String u, String l, int s, int m) {
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

        Usuario(String i, String n, String e, Perfil p) {
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

    public static class Resultado<T> {
        public final T valor;
        public final String erro;

        private Resultado(T v, String e) {
            valor = v;
            erro = e;
        }

        public static <T> Resultado<T> ok(T v) {
            return new Resultado<>(v, null);
        }

        public static <T> Resultado<T> falha(String e) {
            return new Resultado<>(null, e);
        }

        public boolean sucesso() {
            return erro == null;
        }
    }

    public static final class RepositoryProvider {
        public static final FakeDatabase DB = new FakeDatabase();
        public static final FakeAuthRepository AUTH = new FakeAuthRepository(DB);
        public static final FakeItemRepository ITENS = new FakeItemRepository(DB);
        public static final FakeRequisicaoRepository REQUISICOES = new FakeRequisicaoRepository(DB);
        public static final FakeMovimentoRepository MOVIMENTOS = new FakeMovimentoRepository(DB);
    }

    public static final class FakeDatabase {
        final MutableLiveData<List<Item>> itens = new MutableLiveData<>();
        final MutableLiveData<List<Requisicao>> reqs = new MutableLiveData<>();
        final MutableLiveData<List<Movimento>> movs = new MutableLiveData<>();
        final LinkedHashMap<String, Item> itemMap = new LinkedHashMap<>();
        final ArrayList<Requisicao> requisicoes = new ArrayList<>();
        final ArrayList<Movimento> movimentos = new ArrayList<>();

        FakeDatabase() {
            seed();
            publish();
        }

        synchronized void seed() {
            String[][] a = {{"7890000123456", "Parafuso sextavado M8 x 30", "CX", "B-04", "148", "40"}, {"7890000123463", "Porca sextavada M8", "CX", "B-05", "212", "50"}, {"7890000123470", "Luva de raspa cano curto", "PAR", "D-01", "6", "20"}, {"7890000123487", "Arruela lisa 8 mm", "CX", "B-06", "96", "30"}, {"7890000123494", "Rolamento 6204 ZZ", "UN", "C-02", "0", "10"}, {"7890000123500", "Disco de corte 180 mm", "UN", "E-03", "12", "30"}};
            for (String[] x : a)
                itemMap.put(x[0], new Item(x[0], x[1], x[2], x[3], Integer.parseInt(x[4]), Integer.parseInt(x[5])));
            String[] codes = {a[0][0], a[1][0], a[2][0], a[4][0], a[5][0]}, dest = {"Linha de montagem 1", "Manutenção", "Linha de montagem 2", "Expedição", "Linha de montagem 3"};
            for (int n = 0; n < 5; n++) {
                Item it = itemMap.get(codes[n]);
                Requisicao r = new Requisicao();
                r.id = "req-semente-" + n;
                r.itemId = it.codigoBarras;
                r.itemDescricao = it.descricao;
                r.quantidade = new int[]{200, 12, 4, 2, 20}[n];
                r.destino = dest[n];
                r.solicitanteUid = "solicitante";
                r.solicitanteNome = "Carlos";
                r.data = System.currentTimeMillis() - ((5 - n) * 3600000L);
                requisicoes.add(r);
            }
            movimento(codes[0], TipoMovimento.ENTRADA, 200, "Marcos", null);
            movimento(codes[0], TipoMovimento.SAIDA, 100, "Marcos", "Linha de montagem 1");
            movimento(codes[0], TipoMovimento.SAIDA, 52, "Marcos", "Linha de montagem 2");
        }

        synchronized void publish() {
            ArrayList<Item> i = new ArrayList<>(itemMap.values());
            i.sort(Comparator.comparing(x -> x.descricao.toLowerCase(new Locale("pt", "BR"))));
            itens.postValue(i);
            ArrayList<Requisicao> r = new ArrayList<>(requisicoes);
            r.sort(Comparator.comparingLong(x -> x.data));
            reqs.postValue(r);
            ArrayList<Movimento> m = new ArrayList<>(movimentos);
            m.sort((x, y) -> Long.compare(y.data, x.data));
            movs.postValue(m);
        }

        synchronized void movimento(String codigo, TipoMovimento tipo, int q, String nome, String destino) {
            Movimento m = new Movimento();
            m.id = UUID.randomUUID().toString();
            m.itemId = codigo;
            m.tipo = tipo;
            m.quantidade = q;
            m.uid = "fake";
            m.usuarioNome = nome;
            m.destino = destino;
            m.data = System.currentTimeMillis();
            movimentos.add(m);
        }
    }

    public static final class FakeAuthRepository implements AuthRepository {
        final FakeDatabase db;
        final Map<String, Usuario> users = new HashMap<>();
        final Map<String, String> passwords = new HashMap<>();
        Usuario current;

        FakeAuthRepository(FakeDatabase d) {
            db = d;
            Usuario a = new Usuario("almoxarife", "Marcos", "almoxarife@techinova.com.br", Perfil.ALMOXARIFE), s = new Usuario("solicitante", "Carlos", "solicitante@techinova.com.br", Perfil.SOLICITANTE);
            users.put(a.email, a);
            users.put(s.email, s);
            passwords.put(a.email, "123456");
            passwords.put(s.email, "123456");
        }

        // TODO [FIREBASE-AUTH] Trocar por signInWithEmailAndPassword/createUserWithEmailAndPassword/signOut; a sessão falsa atual vive apenas em memória.
        public Resultado<Usuario> entrar(String e, String p) {
            Usuario u = users.get(e.toLowerCase(Locale.ROOT));
            if (u == null || !Objects.equals(passwords.get(e.toLowerCase(Locale.ROOT)), p))
                return Resultado.falha("E-mail ou senha incorretos.");
            current = u;
            return Resultado.ok(u);
        }

        // TODO [FIRESTORE-WRITE] Criar usuarios/{uid} com campos nome,email e sem perfil após Auth criar a conta.
        public Resultado<Usuario> cadastrar(String n, String e, String p) {
            e = e.toLowerCase(Locale.ROOT);
            if (users.containsKey(e)) return Resultado.falha("Este e-mail já está cadastrado.");
            Usuario u = new Usuario(UUID.randomUUID().toString(), n, e, null);
            users.put(e, u);
            passwords.put(e, p);
            return Resultado.ok(u);
        }

        // TODO [PERFIL] Buscar usuarios/{uid}.perfil e nome após autenticação; perfil nunca é escolhido no aplicativo.
        public Usuario restaurar(String uid) {
            for (Usuario u : users.values())
                if (u.uid.equals(uid)) {
                    current = u;
                    return u;
                }
            return null;
        }

        public void sair() {
            current = null;
        }

        public Usuario sessao() {
            return current;
        }
    }

    public static final class FakeItemRepository implements ItemRepository {
        final FakeDatabase d;

        FakeItemRepository(FakeDatabase d) {
            this.d = d;
        }

        public LiveData<List<Item>> observar() {
            return d.itens;
        } // TODO [FIRESTORE-LISTENER] Observar coleção itens, ordenar por descricao e publicar mudanças em tempo real nas telas.

        public Item buscar(String c) {
            return d.itemMap.get(c);
        } // TODO [FIRESTORE-READ] Ler itens/{codigoBarras}; código de barras é o ID único do documento.

        public Resultado<Item> salvar(Item i, boolean edit) {
            if (!edit && d.itemMap.containsKey(i.codigoBarras))
                return Resultado.falha("Já existe um item com este código.");
            Item old = d.itemMap.get(i.codigoBarras);
            if (edit && old != null) i.saldo = old.saldo;
            d.itemMap.put(i.codigoBarras, i);
            if (!edit && i.saldo > 0)
                d.movimento(i.codigoBarras, TipoMovimento.ENTRADA, i.saldo, "Marcos", "Saldo inicial");
            d.publish();
            return Resultado.ok(i);
        } // TODO [FIRESTORE-WRITE] Gravar itens/{codigoBarras} com codigoBarras,descricao,unidade,local,saldo,minimo; criação com saldo deve gerar movimento de entrada.

        // TODO [REGRA-SEGURANCA] Rules devem impedir update de itens.saldo fora das transações de entrada/atendimento e impedir alteração direta do saldo no cadastro/edição.
        public Resultado<Item> entrada(String c, int q, Usuario u, String obs) {
            Item i = buscar(c);
            if (i == null) return Resultado.falha("Item não encontrado.");
            if (q < 1) return Resultado.falha("Informe uma quantidade maior que zero.");
            i.saldo += q;
            d.movimento(c, TipoMovimento.ENTRADA, q, u.nome, obs);
            d.publish();
            return Resultado.ok(i);
        } // TODO [FIRESTORE-TRANSACAO] Ler itens/{codigo}.saldo, somar quantidade e criar movimentos/{id} tipo ENTRADA na mesma transação.
    }

    public static final class FakeRequisicaoRepository implements RequisicaoRepository {
        final FakeDatabase d;

        FakeRequisicaoRepository(FakeDatabase d) {
            this.d = d;
        } // TODO [FIRESTORE-LISTENER] Observar requisicoes com filtros por status ou solicitanteUid e ordenar por data.

        public LiveData<List<Requisicao>> observar() {
            return d.reqs;
        } // TODO [FIRESTORE-WRITE] Criar requisicoes/{id} com itemId,itemDescricao,quantidade,destino,observacao,solicitanteUid,solicitanteNome,status,data.

        public Resultado<Requisicao> criar(String c, int q, String dest, String obs, Usuario u) {
            Item i = d.itemMap.get(c);
            if (i == null) return Resultado.falha("Selecione um item.");
            Requisicao r = new Requisicao();
            r.id = UUID.randomUUID().toString();
            r.itemId = c;
            r.itemDescricao = i.descricao;
            r.quantidade = q;
            r.destino = dest;
            r.observacao = obs;
            r.solicitanteUid = u.uid;
            r.solicitanteNome = u.nome;
            r.data = System.currentTimeMillis();
            d.requisicoes.add(r);
            d.publish();
            return Resultado.ok(r);
        } // TODO [REGRA-NEGOCIO] Não bloquear pedidos acima do saldo; validar saldo apenas no atendimento. Destinos definidos por lista controlada; código do item é único por documento itens/{codigoBarras}.

        public synchronized Resultado<Requisicao> atender(String id, Usuario u) {
            Requisicao r = null;
            for (Requisicao x : d.requisicoes) if (x.id.equals(id)) r = x;
            if (r == null || r.status != StatusRequisicao.PENDENTE)
                return Resultado.falha("Esta requisição já foi atendida.");
            Item i = d.itemMap.get(r.itemId);
            if (i == null || i.saldo < r.quantidade)
                return Resultado.falha("Saldo insuficiente|" + r.itemDescricao + ": pedido de " + r.quantidade + ", saldo de " + (i == null ? 0 : i.saldo) + ". Registre uma entrada antes de atender.");
            i.saldo -= r.quantidade;
            r.status = StatusRequisicao.ATENDIDA;
            r.atendidoPorUid = u.uid;
            r.atendidoEm = System.currentTimeMillis();
            d.movimento(i.codigoBarras, TipoMovimento.SAIDA, r.quantidade, u.nome, r.destino);
            d.publish();
            return Resultado.ok(r);
        } // TODO [FIRESTORE-TRANSACAO] Atender requisição: em UMA transação, ler itens/{itemId}.saldo; recusar se saldo < quantidade; subtrair saldo; criar movimentos/{id} tipo SAIDA; atualizar requisicoes/{id}.status,atendidoPorUid,atendidoEm.
        // TODO [REGRA-SEGURANCA] Firestore Rules devem limitar criação/leitura de requisicoes pelo UID do solicitante; somente ALMOXARIFE muda status/saldo; solicitante não lê movimentos; saldo jamais negativo e perfil não pode ser forjado.
    }

    public static final class FakeMovimentoRepository implements MovimentoRepository {
        final FakeDatabase d;

        FakeMovimentoRepository(FakeDatabase d) {
            this.d = d;
        } // TODO [FIRESTORE-LISTENER] Observar movimentos; filtrar itemId, ordenar data decrescente e limitar a 3 na ficha.

        public LiveData<List<Movimento>> observar() {
            return d.movs;
        }

        public List<Movimento> porItem(String c) {
            ArrayList<Movimento> a = new ArrayList<>();
            for (Movimento m : d.movimentos) if (m.itemId.equals(c)) a.add(m);
            a.sort((x, y) -> Long.compare(y.data, x.data));
            return a;
        }
    }

    public static class AlmoxViewModel extends ViewModel {
        public LiveData<List<Item>> itens = RepositoryProvider.ITENS.observar();
        public LiveData<List<Requisicao>> requisicoes = RepositoryProvider.REQUISICOES.observar();
        public LiveData<List<Movimento>> movimentos = RepositoryProvider.MOVIMENTOS.observar();
        public String codigoSelecionado, destino, observacao, filtro, busca;
        public int quantidade = 1;
    }
}