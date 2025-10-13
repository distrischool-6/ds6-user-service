# Guia Plug and Play para Deploy de Microsserviços no Kubernetes

Este guia fornece um passo a passo completo para configurar um novo microsserviço Spring Boot para ser implantado em nosso cluster Kubernetes através do GitHub Actions.

O objetivo é que o processo seja "plug and play": crie alguns arquivos de manifesto, configure os segredos e o serviço será implantado automaticamente.

---

### Pré-requisitos

Antes de começar, seu microsserviço deve:
1.  Ser um projeto Spring Boot.
2.  Ter a dependência do **Spring Boot Actuator** no `build.gradle` para health checks.
    ```groovy
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    ```
3.  Ter seu `application.properties` configurado para ler variáveis de ambiente (ex: `${DB_USER}`).
4.  Ter um `Dockerfile` na raiz do projeto para construir a imagem.

---

### Passo 1: Criar os Manifestos do Kubernetes

Para cada novo serviço, você precisará de 3 arquivos de manifesto dentro de uma pasta `k8s/` no repositório.

**Instrução:** Copie os templates abaixo, crie os arquivos e **substitua todas as ocorrências de `[NOME-DO-SERVICO]`** pelo nome real do seu serviço (ex: `order-service`).

#### 1. `k8s/deployment.yaml`
*Define como rodar sua aplicação no cluster.*

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: [NOME-DO-SERVICO]
spec:
  replicas: 2 # Quantas instâncias você quer rodando
  selector:
    matchLabels:
      app: [NOME-DO-SERVICO]
  template:
    metadata:
      labels:
        app: [NOME-DO-SERVICO]
    spec:
      containers:
      - name: [NOME-DO-SERVICO]-app
        # IMPORTANTE: A imagem será construída e nomeada pela sua pipeline de CI.
        # Use um nome padrão como gabriel/[NOME-DO-SERVICO]:latest
        image: gabriel/[NOME-DO-SERVICO]:latest
        ports:
        - containerPort: 8080
        # Health Probes (padrão para Spring Boot Actuator)
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 15
          periodSeconds: 20
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 5
          periodSeconds: 10
        # Recursos (ajuste se necessário)
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        # Carrega as configurações dos manifestos abaixo
        envFrom:
        - configMapRef:
            name: [NOME-DO-SERVICO]-config
        - secretRef:
            name: [NOME-DO-SERVICO]-secret
```

#### 2. `k8s/service.yaml`
*Expõe sua aplicação para outros serviços dentro do cluster.*

```yaml
apiVersion: v1
kind: Service
metadata:
  name: [NOME-DO-SERVICO]
spec:
  type: ClusterIP
  selector:
    app: [NOME-DO-SERVICO]
  ports:
    - protocol: TCP
      port: 80 # Porta que outros serviços usarão
      targetPort: 8080 # Porta interna do contêiner
```

#### 3. `k8s/configmap.yaml`
*Guarda as configurações **não-secretas** da sua aplicação.*

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: [NOME-DO-SERVICO]-config
data:
  # Adicione aqui outras variáveis de ambiente não-sensíveis
  DB_HOST: "postgres-service" # Assumindo que o serviço do DB se chama assim
  DB_PORT: "5432"
  DB_NAME: "distrischool_NOME_DO_SEU_DB" # Troque pelo nome do DB
  JWT_EXPIRATION: "86400000"
```

---

### Passo 2: Configurar os Segredos no GitHub

**NUNCA** salve senhas ou chaves no repositório. Nós as gerenciamos de forma centralizada na Organização do GitHub.

1.  Vá para a sua **Organização GitHub** -> `Settings` -> `Secrets and variables` -> `Actions`.
2.  Crie os segredos necessários para o seu novo serviço. Se ele usa o mesmo banco de dados, você pode reutilizar os segredos existentes. Se forem novos, crie-os:
    *   `DB_USER`: O usuário do banco de dados.
    *   `DB_PASSWORD`: A senha do banco de dados.
    *   `JWT_SECRET`: A chave para assinar os tokens JWT.
3.  **Dê acesso ao repositório:** Na política de acesso de cada segredo (`Repository access`), certifique-se de que o novo repositório do microsserviço está na lista de repositórios autorizados.

---

### Passo 3: Criar o Pipeline de Deploy (GitHub Actions)

Este pipeline automatiza todo o processo de deploy.

1.  Crie a pasta `.github/workflows/` no seu repositório.
2.  Dentro dela, crie o arquivo `deploy.yml`.
3.  Copie o conteúdo abaixo e **substitua as duas ocorrências de `[NOME-DO-SERVICO]`**.

#### `.github/workflows/deploy.yml`

```yaml
name: Deploy [NOME-DO-SERVICO] to VPS

on:
  push:
    branches:
      - main

jobs:
  deploy:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v3

      - name: Deploy to VPS
        uses: appleboy/ssh-action@master
        with:
          host: ${{ secrets.VPS_HOST }}
          username: ${{ secrets.VPS_USER }}
          key: ${{ secrets.VPS_SSH_PRIVATE_KEY }}
          script: |
            # Navega até a pasta do projeto no servidor
            cd /home/github/meu-projeto
            git pull origin main

            # Constrói a imagem Docker localmente no servidor
            # Certifique-se que o Dockerfile está na raiz do projeto
            docker build -t gabriel/[NOME-DO-SERVICO]:latest .

            # Cria/Atualiza o Secret do Kubernetes com os valores do GitHub
            kubectl create secret generic [NOME-DO-SERVICO]-secret \
              --from-literal=DB_USER='${{ secrets.DB_USER }}' \
              --from-literal=DB_PASSWORD='${{ secrets.DB_PASSWORD }}' \
              --from-literal=JWT_SECRET='${{ secrets.JWT_SECRET }}' \
              --dry-run=client -o yaml | kubectl apply -f -

            # Aplica os outros manifestos do Kubernetes
            kubectl apply -f k8s/configmap.yaml
            kubectl apply -f k8s/deployment.yaml
            kubectl apply -f k8s/service.yaml

            echo "Deploy do [NOME-DO-SERVICO] finalizado com sucesso!"
```

---

### Checklist Final

Se você seguiu todos os passos, seu novo microsserviço está pronto.

- [ ] Substituiu todas as ocorrências de `[NOME-DO-SERVICO]` nos arquivos `deployment.yaml`, `service.yaml`, `configmap.yaml` e `deploy.yml`.
- [ ] Adicionou os segredos necessários na Organização GitHub e deu permissão de acesso ao repositório.
- [ ] Fez o commit e push dos novos arquivos para a branch `main`.

Ao fazer o push, o pipeline do GitHub Actions será acionado e seu serviço será automaticamente implantado no cluster. Você pode acompanhar o progresso na aba "Actions" do seu repositório.