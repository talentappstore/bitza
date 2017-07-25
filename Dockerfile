FROM openjdk:8

COPY target/bitza-0.0.1-SNAPSHOT.jar app.jar

CMD ['java', '$JAVA_OPTS', 'app.jar']
