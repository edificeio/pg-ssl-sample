# This Dockerfile contains the image specification of our database
FROM postgres:14-alpine

COPY ./out/postgresdb.key /var/lib/postgresql
COPY ./out/postgresdb.crt /var/lib/postgresql

COPY ./out/myCA.crt /var/lib/postgresql
COPY ./out/myCA.crl /var/lib/postgresql


RUN chown postgres:postgres /var/lib/postgresql/postgresdb.key && chmod 600 /var/lib/postgresql/postgresdb.key
RUN chown postgres:postgres /var/lib/postgresql/postgresdb.crt && chmod 600 /var/lib/postgresql/postgresdb.crt

RUN chown postgres:postgres /var/lib/postgresql/myCA.crt && chmod 600 /var/lib/postgresql/myCA.crt
RUN chown postgres:postgres /var/lib/postgresql/myCA.crl && chmod 600 /var/lib/postgresql/myCA.crl

ENV POSTGRES_HOST_AUTH_METHOD=trust
COPY custom-entrypoint.sh /usr/local/bin/

RUN chown postgres:postgres /usr/local/bin/custom-entrypoint.sh && chmod u+x /usr/local/bin/custom-entrypoint.sh

ENTRYPOINT ["custom-entrypoint.sh"]

CMD [ "-c", "ssl=on" , "-c", "ssl_cert_file=/var/lib/postgresql/postgresdb.crt", "-c",\
    "ssl_key_file=/var/lib/postgresql/postgresdb.key", "-c",\
    "ssl_ca_file=/var/lib/postgresql/myCA.crt", "-c", "ssl_crl_file=/var/lib/postgresql/myCA.crl", \
    "-c","hba_file=/etc/postgresql/14/main/pg_hba.conf"]