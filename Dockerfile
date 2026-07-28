# Estágio 1: Build (Compilação)
FROM eclipse-temurin:21-jdk-alpine AS builder

# Define o diretório de trabalho dentro do container
WORKDIR /app

# Copia os arquivos de configuração do Gradle primeiro (para aproveitar o cache do Docker)
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Corrige problemas de quebra de linha do Windows (CRLF para LF)
RUN sed -i 's/\r$//' gradlew

# Dá permissão de execução para o wrapper do Gradle
RUN chmod +x ./gradlew

# Baixa as dependências sem compilar o código (isso cria uma camada de cache)
RUN ./gradlew dependencies --no-daemon

# Agora copia o código fonte do projeto
COPY src src

# Compila o projeto gerando o arquivo .jar executável
RUN ./gradlew build -x test --no-daemon

# Estágio 2: Execução (Imagem final mais leve)
FROM eclipse-temurin:21-jre-alpine

# Diretório de trabalho na imagem final
WORKDIR /app

# Copia apenas o arquivo .jar executável compilado do estágio anterior
COPY --from=builder /app/build/libs/fitoherb-backend-v2-0.0.1-SNAPSHOT.jar app.jar

# Expõe a porta que o Spring Boot utiliza
EXPOSE 8080

# Comando que será executado quando o container iniciar
ENTRYPOINT ["java", "-jar", "app.jar"]
