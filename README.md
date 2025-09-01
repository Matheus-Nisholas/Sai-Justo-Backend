# Calculadora de Rescisão - Backend

---

## ✨ Funcionalidades Principais

* **Autenticação Segura:** Sistema de registro e login com senhas criptografadas (BCrypt) e autenticação baseada em JSON Web Tokens (JWT).
* **Cálculo de Rescisão Detalhado:** Lógica de negócio robusta para calcular verbas rescisórias com base em diferentes tipos de demissão (Sem Justa Causa, Pedido de Demissão, etc.), considerando regras de FGTS, multas e descontos.
* **Histórico de Cálculos:** Todos os cálculos são salvos e associados ao usuário autenticado.
* **Exportação para PDF:** Geração de um recibo de rescisão detalhado em formato PDF.
* **API Documentada:** Documentação da API gerada automaticamente com Swagger (OpenAPI), facilitando o teste e a integração.

---

## 🚀 Tecnologias Utilizadas

* **Java 17**
* **Spring Boot 3**
* **Spring Security:** Para autenticação e autorização.
* **Spring Data JPA (Hibernate):** Para persistência de dados.
* **PostgreSQL:** Banco de dados relacional.
* **Flyway:** Para versionamento e migração do schema do banco de dados.
* **JWT (JSON Web Tokens):** Para a API stateless.
* **Lombok:** Para reduzir código boilerplate.
* **OpenPDF:** Para a geração de documentos PDF.
* **Swagger/OpenAPI:** Para documentação da API.

---

## 🏁 Como Executar (Ambiente de Desenvolvimento)

### Pré-requisitos

* JDK 17 ou superior.
* PostgreSQL instalado localmente ou via Docker.
* Um cliente de banco de dados (como pgAdmin ou DBeaver).

### Passos

1.  **Clone o repositório:**
    ```bash
    git clone [https://github.com/seu-usuario/calculadora-rescisao.git](https://github.com/seu-usuario/calculadora-rescisao.git)
    cd calculadora-rescisao
    ```

2.  **Configure o Banco de Dados:**
    * Crie um banco de dados no seu PostgreSQL chamado `rescisao_db`.
    * Verifique se as credenciais no arquivo `src/main/resources/application.properties` correspondem às do seu banco de dados local.

3.  **Execute a Aplicação:**
    * Abra o projeto em sua IDE (IntelliJ, VS Code, etc.).
    * Execute a classe principal `CalculadoraRescisaoApplication.java`.

4.  **Acesse a Documentação da API:**
    * Com a aplicação rodando, acesse a seguinte URL no seu navegador para ver e testar os endpoints:
      [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---
