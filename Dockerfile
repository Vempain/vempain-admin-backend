FROM eclipse-temurin:25-jre-alpine
EXPOSE 8080 8081

RUN apk add --no-cache curl exiftool
RUN mkdir /vempain_admin
RUN adduser -D -h /vempain_admin/vempain -u 6666 -H vempain

USER vempain

ADD service/build/libs/vempain-admin-backend-*.jar /app.jar

ENTRYPOINT ["java","-jar","/app.jar"]
