# Guia para Preparar um Serviço Spring Boot para o Kubernetes

Este documento detalha os passos necessários para adaptar uma aplicação Spring Boot para ser executada de forma segura, resiliente e configurável em um ambiente Kubernetes.

---

### Passo 1: Externalizar a Configuração da Aplicação

**O Problema:** Configurações como URLs de banco de dados, senhas e chaves secretas não devem ser fixas no código (`hardcoded`). Isso é inseguro e inflexível, pois cada mudança exigiria a reconstrução da imagem Docker.

**A Solução:** Modificar o `application.properties` para ler esses valores a partir de variáveis de ambiente. O Kubernetes será responsável por injetar essas variáveis nos contêineres.

**Ação:** Substitua os valores estáticos por placeholders que leem variáveis de ambiente, usando a sintaxe `${NOME_DA_VARIAVEL:valor_default}`.

**Arquivo Modificado:** `src/main/resources/application.properties`

```properties
# Antes
spring.datasource.url=jdbc:postgresql://localhost:5432/distrischool_users
spring.datasource.username=postgres
spring.datasource.password=1234
jwt.secret=6ABM8AgHqlaaQ/WDtQqJTQ6wO99YRXNRUhjJjVfbH+w=
jwt.expiration=86400000

# Depois
spring.datasource.url=jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:distrischool_users}
spring.datasource.username=${DB_USER:postgres}
spring.datasource.password=${DB_PASSWORD:1234}
jwt.secret=${JWT_SECRET:6ABM8AgHqlaaQ/WDtQqJTQ6wO99YRXNRUhjJjVfbH+w=}
jwt.expiration=${JWT_EXPIRATION:86400000}
```

---

### Passo 2: Adicionar Endpoints de Verificação de Saúde (Health Probes)

**O Problema:** O Kubernetes precisa saber se a sua aplicação está funcionando corretamente para poder gerenciá-la (reiniciar se travar, não enviar tráfego se não estiver pronta).

**A Solução:** Adicionar o **Spring Boot Actuator**, que expõe automaticamente endpoints como `/actuator/health` para monitoramento.

**Ação:** Adicione a dependência do Actuator ao seu arquivo de build.

**Arquivo Modificado:** `build.gradle`

```groovy
// Adicione esta linha dentro do bloco 'dependencies'
implementation 'org.springframework.boot:spring-boot-starter-actuator'
```

---

### Passo 3: Adaptar o Manifesto de Deployment do Kubernetes

**O Problema:** O `deployment.yaml` básico apenas executa a imagem, mas não aproveita os recursos de gerenciamento do Kubernetes.

**A Solução:** Aprimorar o manifesto para:
1.  Usar os endpoints do Actuator com `livenessProbe` e `readinessProbe`.
2.  Definir `requests` e `limits` de recursos (CPU/Memória) para garantir a estabilidade do cluster.
3.  Instruir o deployment a carregar as variáveis de ambiente a partir de `ConfigMaps` e `Secrets`.

**Ação:** Atualize o arquivo `k8s/deployment.yaml` com as seções `livenessProbe`, `readinessProbe`, `resources` e `envFrom`.

**Arquivo Modificado:** `k8s/deployment.yaml`

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ds6-user-service
spec:
  replicas: 1
  selector:
    matchLabels:
      app: ds6-user-service
  template:
    metadata:
      labels:
        app: ds6-user-service
    spec:
      containers:
      - name: app
        image: imagem-placeholder # Esta imagem será substituída pela sua pipeline de CI/CD
        ports:
        - containerPort: 8080
        # Health Probes: Garante que o tráfego só seja enviado para pods saudáveis.
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
        # Gerenciamento de Recursos: Garante a estabilidade do cluster.
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        # Externalização de Configuração: Carrega configurações do ambiente K8s.
        envFrom:
        - configMapRef:
            # O nome do ConfigMap que você criará
            name: user-service-config
        - secretRef:
            # O nome do Secret que você criará
            name: user-service-secret
```

---

### Passo 4: Criar os Manifestos de Configuração e Segredos

**O Problema:** O deployment agora depende de um `ConfigMap` e de um `Secret` para obter suas configurações. Precisamos criar esses objetos no Kubernetes.

**A Solução:** Criar arquivos YAML separados para o `ConfigMap` (dados não-sensíveis) e para o `Secret` (dados sensíveis).

**Ação:** Crie os dois arquivos a seguir no diretório `k8s/`.

**1. Novo Arquivo:** `k8s/configmap.yaml` (para dados não-sensíveis)

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: user-service-config
data:
  DB_HOST: "postgres-service" # Nome do serviço do seu banco de dados no K8s
  DB_PORT: "5432"
  DB_NAME: "distrischool_users"
  JWT_EXPIRATION: "86400000"
```

**2. Novo Arquivo:** `k8s/secret.yaml` (para dados sensíveis)

**IMPORTANTE:** Os valores em um `Secret` devem ser codificados em **Base64**.

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: user-service-secret
type: Opaque
data:
  # Para gerar os valores, use o comando: echo -n 'seu-valor' | base64
  DB_USER: "cG9zdGdyZXM=" # 'postgres' em Base64
  DB_PASSWORD: "c3VhX3NlbmhhX2FxdWk=" # 'sua_senha_aqui' em Base64
  JWT_SECRET: "NkFCTTNBZ0hxTGFhUS9XRHQzUWpUUTZ3Tzk5WVJYTlJVaGpKalZmaEgrdz0=" # '6ABM8...' em Base64
```

Com estes quatro passos, qualquer serviço Spring Boot se torna robusto e alinhado com as melhores práticas para deploy em Kubernetes.
