FROM eclipse-temurin:25-jre-alpine
EXPOSE 8080 8081

RUN apk add --no-cache curl exiftool su-exec
RUN mkdir /vempain_admin
RUN adduser -D -h /vempain_admin/vempain -u 6666 -H vempain

ADD service/build/libs/vempain-admin-backend-*.jar /app.jar
COPY container-entrypoint.sh /usr/local/bin/container-entrypoint.sh
RUN chmod 755 /usr/local/bin/container-entrypoint.sh

ENTRYPOINT ["/usr/local/bin/container-entrypoint.sh"]
CMD ["-jar","/app.jar"]
