# This Dockerfile contains the image specification of our database
FROM postgres:14

COPY ./out/postgresdb.key /var/lib/postgresql
COPY ./out/postgresdb.crt /var/lib/postgresql

COPY ./out/myCA.crt /var/lib/postgresql
COPY ./out/myCA.crl /var/lib/postgresql

COPY ./postgresql.conf /etc/postgresql/14/main/postgresql.conf

RUN chown postgres:postgres /var/lib/postgresql/postgresdb.key && chmod 600 /var/lib/postgresql/postgresdb.key
RUN chown postgres:postgres /var/lib/postgresql/postgresdb.crt && chmod 600 /var/lib/postgresql/postgresdb.crt

RUN chown postgres:postgres /var/lib/postgresql/myCA.crt && chmod 600 /var/lib/postgresql/myCA.crt
RUN chown postgres:postgres /var/lib/postgresql/myCA.crl && chmod 600 /var/lib/postgresql/myCA.crl

RUN echo 'hostssl all all all md5' > /var/lib/postgresql/data/pg_hba.conf

ENV POSTGRES_HOST_AUTH_METHOD=trust

ENTRYPOINT ["docker-entrypoint.sh"]

ENV PGDATA=/var/lib/postgresql/data_new

CMD [ "-c", "ssl=on" , "-c", "ssl_cert_file=/var/lib/postgresql/postgresdb.crt", "-c",\
    "ssl_key_file=/var/lib/postgresql/postgresdb.key", "-c",\
    "ssl_ca_file=/var/lib/postgresql/myCA.crt", "-c", "ssl_crl_file=/var/lib/postgresql/myCA.crl"]