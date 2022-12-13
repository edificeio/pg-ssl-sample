package sample.pgssl.client;

import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.*;

public class PostgresClientPool implements IPostgresClient {
    private static final Logger log = LoggerFactory.getLogger(PostgresClientPool.class);
    private final PgPool pgPool;
    private final JsonObject config;
    private final Vertx vertx;

    PostgresClientPool(final Vertx vertx, final PgPool pgPool, final JsonObject config) {
        this.vertx = vertx;
        this.config = config;
        this.pgPool = pgPool;
    }

    public Future<IPostgresTransaction> transaction() {
        final Future<IPostgresTransaction> future = Future.future();
        this.pgPool.begin(r -> {
            if (r.succeeded()) {
                future.complete(new PostgresClient.PostgresTransaction(r.result()));
            } else {
                future.fail(r.cause());
            }
        });
        return future;
    }

    public Future<RowSet<Row>> preparedQuery(final String query, final Tuple tuple) {
        final Future<RowSet<Row>> future = Future.future();
        this.pgPool.preparedQuery(query).execute(tuple, future.completer());
        return future;
    }

}
