ARG prestoVersion=latest
FROM prestosql/presto:${prestoVersion}

ARG nodetype
ARG nodeenvname=default

WORKDIR /usr/lib/presto

COPY ${nodetype}/config.properties default/etc/config.properties
RUN echo "node.environment=$nodeenvname" > default/etc/node.properties

COPY \
    postgres.properties \
    kafka.properties \
# The following won't work in minikube
#    meetup.properties \
    default/etc/catalog/
