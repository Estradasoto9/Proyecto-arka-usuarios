FROM amazoncorretto:17-alpine-jdk AS builder

# Configura zona horaria para Colombia
RUN apk add --no-cache tzdata
RUN cp /usr/share/zoneinfo/America/Bogota /etc/localtime
RUN echo "America/Bogota" > /etc/timezone

# Crea un runtime de Java liviano con jlink
RUN jlink --compress=2 --module-path "$JAVA_HOME/jmods" \
  --add-modules java.base,java.logging,java.xml,jdk.unsupported,java.sql,java.naming,java.desktop,java.management,java.security.jgss,java.instrument,jdk.management,jdk.crypto.cryptoki \
  --no-header-files --no-man-pages --output /jlinked

# Etapa final
FROM amazoncorretto:17-alpine-jdk

# Zona horaria y seguridad
COPY --from=builder /etc/localtime /etc/localtime
RUN echo "America/Bogota" > /etc/timezone && addgroup -S appuser && adduser -S appuser -G appuser
USER appuser

# Configura JAVA_HOME y PATH
ENV JAVA_HOME=/opt/jdk
ENV PATH=$JAVA_HOME/bin:$PATH

# Copia el runtime JLinked
COPY --from=builder /jlinked /opt/jdk/

# Copia el JAR compilado (usamos nombre genérico)
COPY target/*.jar app.jar

# Puerto del servicio de usuarios (ajusta si usas otro)
EXPOSE 8081

# Comando de ejecución
ENTRYPOINT ["java", "-jar", "/app.jar"]
