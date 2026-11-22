# ==========================================
# Estágio 1: Build da aplicação (Maven + JDK 24)
# ==========================================
FROM maven:3.9.9-eclipse-temurin-24-alpine AS builder
WORKDIR /app

COPY pom.xml ./
# Usa cache persistente do BuildKit para não baixar dependências repetidamente
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -B

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests -B

# Extrai o JAR em camadas renomeando o artefato principal para app.jar
RUN java -Djarmode=tools -jar target/*.jar extract --layers --application-filename app.jar --destination target/extracted

# ==========================================
# Estágio 2: Imagem final enxuta com CDS (JRE 24)
# ==========================================
FROM eclipse-temurin:24-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

# Copia cada camada separadamente (camadas superiores mudam raramente e ficam em cache na nuvem)
COPY --from=builder --chown=spring:spring /app/target/extracted/dependencies/ ./
COPY --from=builder --chown=spring:spring /app/target/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=spring:spring /app/target/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=spring:spring /app/target/extracted/application/ ./

USER spring:spring

# Gera o arquivo CDS (Class Data Sharing) durante o build para acelerar o startup e reduzir RAM na nuvem
RUN java -XX:ArchiveClassesAtExit=app-cds.jsa -Dspring.context.exit=onRefresh -jar app.jar || true

EXPOSE 8081

# Flags otimizadas para baixo consumo de RAM, startup rápido (CDS) e Virtual Threads
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+TieredCompilation", \
  "-XX:TieredStopAtLevel=1", \
  "-XX:SharedArchiveFile=app-cds.jsa", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
