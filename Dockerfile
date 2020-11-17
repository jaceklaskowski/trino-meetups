FROM prestosql/presto:346
WORKDIR /usr/lib/presto/default/etc/catalog
COPY postgres.properties postgres.properties
