FROM repo.koall.io:8080/base
ENV PATH $PATH:/opt/jdk/bin
VOLUME /opt/jdk
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
WORKDIR /srv/
ADD target/*.jar /srv/
CMD java -Dfile.encoding=UTF-8 -jar ko-mail.jar
