FROM prestosql/presto:346

ARG nodetype
ARG nodeenvname

WORKDIR /usr/lib/presto

COPY ${nodetype}/config.properties default/etc/config.properties
RUN echo "node.environment=$nodeenvname" > default/etc/node.properties

COPY postgres.properties kafka.properties default/etc/catalog/
