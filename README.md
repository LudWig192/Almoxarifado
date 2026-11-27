# Almoxarifado

Aplicativo Android (Java) para controle de estoque de um almoxarifado, com sincronização em tempo real usando **Firebase Authentication** e **Cloud Firestore**.

## Integrantes

| Nome |
|---|
| Livia Figueiredo Da Silva |
| Lucas Vinicius Tinin De Lima |
| Sabrina Brito De Abreu Santos |
| Manoela Cruz Ramalho |

## Problema

Em um almoxarife, o controle de estoque feito de forma manual gera erros de saldo, pedidos sem registro e falta de rastreio de quem retirou o material. O solicitante não sabe se o item está disponível, e o almoxarife não tem um histórico confiável das entradas e saídas.

O aplicativo resolve isso com dois perfis:

- **Almoxarife:** cadastra itens, registra entradas de estoque, atende requisições e consulta o histórico de movimentos.
- **Solicitante:** pede materiais e acompanha o andamento dos próprios pedidos.

O saldo só muda por meio de **entradas** e **atendimentos**, ambos gravados em transação junto com um registro de movimento. Todas as telas se atualizam sozinhas quando outro usuário altera os dados.

## Telas

> Substitua os caminhos pelos prints reais, na pasta `docs/prints/`.

| Tela | Print |
|---|---|
| Login | `<img width="174" height="442" alt="image" src="https://github.com/user-attachments/assets/0b2e4e3b-bc7d-4677-b67c-b9550728992d" />
` |
|  Home do Almoxarife | `<img width="169" height="432" alt="image" src="https://github.com/user-attachments/assets/c662d931-a083-42c4-a8e7-ac26e7dd4376" />
` |
| Lista de itens | `<img width="168" height="435" alt="image" src="https://github.com/user-attachments/assets/1869e299-8461-4424-b28a-11a2aaba157f" />
` |
| Leitor de codigo de barras | `<img width="168" height="433" alt="image" src="https://github.com/user-attachments/assets/74279445-cd69-4f81-9a80-8ba1a0af41d6" />
` |
| Ficha do item | `<img width="163" height="430" alt="image" src="https://github.com/user-attachments/assets/e75270a2-efa8-49c6-b13a-cbd25b3fff99" />
` |
| Atendimento de requisições | `<img width="166" height="426" alt="image" src="https://github.com/user-attachments/assets/c37afd8f-b489-4b23-9bd5-d63c697733c5" />
` |
| Abaixo do minimo | `<img width="169" height="429" alt="image" src="https://github.com/user-attachments/assets/4fff03da-ced3-428c-8794-6e957f503720" />
` |

## Tecnologias

- Android (Java), pacote `com.example.almoxarifado`
- Firebase Authentication (e-mail e senha)
- Cloud Firestore (listeners em tempo real e transações)
- ViewModel e LiveData

## Como configurar o Firebase

1. Acesse https://console.firebase.google.com e clique em **Criar um projeto**.
2. Registre um app Android com o nome do pacote igual ao `applicationId` do projeto (`com.example.almoxarifado`).
3. Baixe o **`google-services.json`** e coloque dentro da pasta `app/`. **Esse arquivo não está no repositório**; cada pessoa usa o do próprio projeto Firebase.
4. Em **Authentication → Método de login**, ative **E-mail/senha**.
5. Em **Firestore Database**, crie o banco (região `southamerica-east1`, por exemplo). Use o modo de teste enquanto desenvolve.
6. Abra o projeto no Android Studio e aguarde o sync do Gradle.
7. Crie as contas de teste (próxima seção).
8. Publique as regras de segurança (seção **Regras do Firestore**).

### Dependências do Gradle

No `build.gradle.kts` da raiz:

```kotlin
id("com.google.gms.google-services") version "4.4.2" apply false
```

No `app/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
}
```

## Contas de teste

O cadastro pelo aplicativo **não define perfil**: quem se cadastra fica sem acesso até receber um perfil manualmente. Por isso as contas abaixo são criadas pelo console.

| Perfil | E-mail | 
|---|---|
| Almoxarife | `almoxarife.teste@gmail.com` |
| Solicitante | `solicitante.teste@gmail.com` | 

Para criar cada conta:

1. **Authentication → Usuários → Adicionar usuário**, com e-mail e senha (mínimo de 6 caracteres). Copie o **UID**.
2. **Firestore Database**, coleção `usuarios`, documento com ID igual ao **UID**, com três campos de texto:

| Campo | Valor |
|---|---|
| `nome` | nome do usuário |
| `email` | o mesmo e-mail |
| `perfil` | `ALMOXARIFE` ou `SOLICITANTE` (em maiúsculas) |

## Modelo de dados (Firestore)

| Coleção | Documento | Campos |
|---|---|---|
| `usuarios` | UID do Auth | `nome`, `email`, `perfil` |
| `itens` | código de barras | `codigoBarras`, `descricao`, `unidade`, `local`, `saldo`, `minimo` |
| `requisicoes` | gerado | `itemId`, `itemDescricao`, `quantidade`, `destino`, `observacao`, `solicitanteUid`, `solicitanteNome`, `status`, `data`, `atendidoPorUid`, `atendidoEm` |
| `movimentos` | gerado | `itemId`, `tipo` (`ENTRADA` ou `SAIDA`), `quantidade`, `uid`, `usuarioNome`, `destino`, `data` |

O **status do estoque nunca é gravado**; ele é calculado a partir do saldo:

- saldo igual a 0: **Zerado**
- saldo menor que o mínimo: **Abaixo do mínimo**
- demais casos: **Em estoque**

## Regras do Firestore

Em **Firestore Database → Regras**:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    function logado() { return request.auth != null; }
    function perfil() {
      return get(/databases/$(database)/documents/usuarios/$(request.auth.uid)).data.perfil;
    }
    function almox() { return logado() && perfil() == 'ALMOXARIFE'; }
    function solic() { return logado() && perfil() == 'SOLICITANTE'; }

    match /usuarios/{uid} {
      allow read: if logado() && request.auth.uid == uid;
      allow create: if logado() && request.auth.uid == uid
                    && !('perfil' in request.resource.data);
      allow update, delete: if false;
    }

    match /itens/{codigo} {
      allow read: if almox() || solic();
      allow create, update: if almox() && request.resource.data.saldo >= 0;
      allow delete: if false;
    }

    match /movimentos/{id} {
      allow read: if almox();
      allow create: if almox() && request.resource.data.quantidade > 0
                    && request.resource.data.uid == request.auth.uid;
      allow update, delete: if false;
    }

    match /requisicoes/{id} {
      allow read: if almox() || (solic() && resource.data.solicitanteUid == request.auth.uid);
      allow create: if solic()
                    && request.resource.data.solicitanteUid == request.auth.uid
                    && request.resource.data.status == 'PENDENTE';
      allow update: if almox()
                    && resource.data.status == 'PENDENTE'
                    && request.resource.data.status == 'ATENDIDA';
      allow delete: if false;
    }
  }
}
```

As regras garantem que só o almoxarife altera o saldo, que ele nunca fica negativo, que o solicitante só vê os próprios pedidos e não lê movimentos, e que ninguém altera o próprio perfil. O controle visual por perfil no aplicativo apenas organiza a navegação e **não substitui** estas regras.

## Vídeo da demonstração

Link (não listado): _cole aqui o link do vídeo_

---

# Histórias de usuário e critérios de aceite

Perfis: **Visitante** (sem login), **Almoxarife** e **Solicitante**.

## Acesso e perfis

### US01. Criar conta
Como **visitante**, quero me cadastrar com nome, e-mail e senha para poder solicitar acesso ao sistema.

**Critérios de aceite**
- Dado um e-mail novo e uma senha de pelo menos 6 caracteres, quando eu me cadastro, então a conta é criada no Authentication e o documento `usuarios/{uid}` é gravado com `nome` e `email`, **sem** `perfil`.
- Dado um e-mail já cadastrado, quando tento me cadastrar, então vejo "Este e-mail já está cadastrado."
- Dada uma senha com menos de 6 caracteres, então vejo "A senha deve ter pelo menos 6 caracteres."
- Depois do cadastro, não fico logado, porque ainda não tenho perfil.

### US02. Entrar e manter a sessão
Como **usuário**, quero entrar com e-mail e senha e continuar logado ao reabrir o app.

**Critérios de aceite**
- Dados e-mail e senha corretos e um perfil liberado, quando entro, então abre a tela inicial do meu perfil.
- Dados e-mail ou senha incorretos, então vejo "E-mail ou senha incorretos." e continuo na tela de login.
- Campos vazios, e-mail inválido ou senha curta mostram a mensagem de erro no próprio campo, sem chamar o servidor.
- Se já havia uma sessão ativa, ao abrir o app vou direto à tela inicial, sem digitar a senha.
- O botão de entrar fica desabilitado enquanto a resposta não chega.

### US03. Bloqueio de usuário sem perfil
Como **responsável pelo sistema**, quero que quem não tem perfil liberado não use o app.

**Critérios de aceite**
- Dado um usuário autenticado sem o campo `perfil` (ou com valor inválido), quando tenta entrar, então é deslogado e vê "Seu usuário ainda não tem perfil. Procure o responsável."
- O perfil só pode ser definido pelo console do Firebase; o app não oferece escolha de perfil.
- As regras do Firestore impedem qualquer usuário de alterar `usuarios/{uid}.perfil`.

### US04. Proteção das telas por perfil
Como **responsável pelo sistema**, quero que cada perfil acesse apenas as telas do seu papel.

**Critérios de aceite**
- Quando um solicitante tenta abrir uma tela de almoxarife (ou o contrário), vê "Você não tem acesso a esta tela." e a tela fecha.
- As mesmas restrições valem nas regras do Firestore, mesmo que a tela seja contornada.

## Itens e estoque (Almoxarife)

### US05. Ver a lista de itens em tempo real
Como **almoxarife**, quero ver os itens ordenados por descrição, com saldo e situação, para saber o que está faltando.

**Critérios de aceite**
- A lista mostra descrição, local, unidade, saldo e o status (Em estoque, Zerado ou Abaixo do mínimo).
- A ordenação é por descrição, sem diferenciar maiúsculas e acentos.
- Quando outro usuário altera um item, a lista atualiza sozinha, sem botão de recarregar.
- O status é calculado pelo saldo e pelo mínimo e nunca é gravado no banco.

### US06. Cadastrar item
Como **almoxarife**, quero cadastrar um item com código de barras, descrição, unidade, local, saldo inicial e mínimo.

**Critérios de aceite**
- O código de barras é o ID único do documento em `itens`.
- Se já existir um item com o mesmo código, vejo "Já existe um item com este código." e nada é gravado.
- Se o saldo inicial for maior que zero, é criado automaticamente um movimento de **ENTRADA** com o texto "Saldo inicial", na mesma operação.
- A gravação do item e do movimento é atômica (ou grava os dois, ou nenhum).

### US07. Editar item
Como **almoxarife**, quero corrigir descrição, unidade, local e mínimo de um item.

**Critérios de aceite**
- A edição altera somente `descricao`, `unidade`, `local` e `minimo`.
- O saldo **não** muda por edição; só muda por entrada ou atendimento.

### US08. Registrar entrada de estoque
Como **almoxarife**, quero registrar a chegada de material para atualizar o saldo.

**Critérios de aceite**
- Dada uma quantidade maior que zero, o saldo do item aumenta e um movimento **ENTRADA** é criado na mesma transação, com quantidade, usuário e data do servidor.
- Quantidade menor que 1 mostra "Informe uma quantidade maior que zero." e nada é alterado.
- Se o item não existir, vejo "Item não encontrado."

### US09. Consultar a ficha do item
Como **almoxarife**, quero ver os últimos movimentos de um item para entender o histórico.

**Critérios de aceite**
- A ficha mostra os **3 movimentos mais recentes**, do mais novo para o mais antigo.
- Cada movimento mostra tipo (entrada ou saída com destino), data, quem fez e a quantidade com sinal (+ ou −).
- A ficha atualiza sozinha quando surge um novo movimento.

## Requisições

### US10. Solicitar material
Como **solicitante**, quero pedir um item informando quantidade e destino para receber o material.

**Critérios de aceite**
- Escolho o item em uma lista, informo quantidade, destino (de uma lista controlada) e observação opcional.
- O pedido é criado com `status = PENDENTE`, meu UID e meu nome, e a data do servidor.
- O pedido **não é bloqueado** quando a quantidade é maior que o saldo; essa validação só ocorre no atendimento.

### US11. Acompanhar meus pedidos
Como **solicitante**, quero ver apenas os meus pedidos e o andamento deles.

**Critérios de aceite**
- A lista mostra somente requisições com o meu UID.
- Cada pedido exibe o status (Pendente ou Atendida), destino e horário.
- Quando o almoxarife atende, o status muda na minha tela sem eu recarregar.
- Não vejo o saldo nem os botões de atendimento.

### US12. Atender requisição
Como **almoxarife**, quero atender um pedido para entregar o material e baixar o estoque.

**Critérios de aceite**
- Em **uma única transação**: valido o saldo, baixo o saldo do item, crio um movimento **SAIDA** (com o destino) e marco a requisição como ATENDIDA, com o atendente e a data.
- Se o saldo for suficiente, o item e a requisição refletem a baixa imediatamente em todas as telas.
- Se a requisição já tiver sido atendida (inclusive por outro almoxarife ao mesmo tempo), vejo "Esta requisição já foi atendida." e o saldo não é baixado duas vezes.
- O botão de atender fica desabilitado enquanto a operação está em andamento.

### US13. Saldo insuficiente no atendimento
Como **almoxarife**, quero ser avisado quando não há saldo, para registrar uma entrada antes.

**Critérios de aceite**
- Quando o saldo é menor que a quantidade pedida, o atendimento é recusado e nada é alterado.
- A mensagem informa o item, a quantidade pedida e o saldo atual, e orienta a registrar uma entrada antes.
- Na lista, pedido e saldo aparecem destacados em vermelho quando o saldo não cobre o pedido.
- O saldo nunca fica negativo.

## Regras de segurança

### US14. Regras de acesso a dados
Como **responsável pelo sistema**, quero que as regras do Firestore protejam os dados mesmo fora do app.

**Critérios de aceite**
- O solicitante só cria pedidos com o próprio UID, só lê os próprios e não lê `movimentos`.
- Somente o almoxarife atende requisições e altera o saldo.
- O saldo de `itens` não pode ficar negativo.
- Ninguém altera `usuarios/{uid}.perfil` pelo app.
- Requisições e movimentos não podem ser apagados.
