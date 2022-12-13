package sample.pgssl.client;

import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.pgclient.PgConnection;
import io.vertx.pgclient.PgPool;
import io.vertx.pgclient.SslMode;
import io.vertx.sqlclient.*;

import java.time.LocalDateTime;
import java.util.*;

public class PostgresClient implements IPostgresClient {
    private final Vertx vertx;
    private final JsonObject config;
    private PostgresClientPool pool;


    public PostgresClient(final Vertx vertx, final JsonObject config) {
        this.vertx = vertx;
        this.config = config;
    }

    public static String toInsertQuery(final String table, final JsonObject json) {
        final StringBuilder query = new StringBuilder();
        final List<String> columns = new ArrayList<>();
        final List<String> values = new ArrayList<>();
        for(final String key : json.fieldNames()){
            final int count = values.size()+1;
            columns.add(String.format("\"%s\"",key));
            values.add("$" + count);
        }
        final String columnPart = String.join(",", columns);
        final String valuesPart = String.join(",", values);
        query.append(String.format("INSERT INTO %s (%s) VALUES(%s) RETURNING *",table, columnPart, valuesPart));
        return query.toString();
    }

    public static Tuple toInsertTuple(final JsonObject json) {
        final Tuple tuple = Tuple.tuple();
        for(final String key : json.fieldNames()){
            tuple.addValue(json.getValue(key));
        }
        return tuple;
    }

    public static JsonObject toJson(final Row row) {
        final JsonObject json = new JsonObject();
        for(int i = 0 ; i < row.size(); i++){
            final String key = row.getColumnName(i);
            final Object value = row.getValue(key);
            if (value instanceof UUID) {
                json.put(key, value.toString());
            } else if (value instanceof LocalDateTime) {
                json.put(key, value.toString());
            } else {
                json.put(key, value);
            }
        }
        return json;
    }

    public static JsonObject toJson(final Row row, final RowSet result) {
        final JsonObject json = new JsonObject();
        final List<String> columns = result.columnsNames();
        for (final String key : columns) {
            final Object value = row.getValue(key);
            if (value instanceof UUID) {
                json.put(key, value.toString());
            } else if (value instanceof LocalDateTime) {
                json.put(key, value.toString());
            } else {
                json.put(key, value);
            }
        }
        return json;
    }

    public PostgresClientPool getClientPool() {
        return getClientPool(true);
    }

    public PostgresClientPool getClientPool(boolean reuse) {
        final PoolOptions poolOptions = new PoolOptions().setMaxSize(config.getInteger("pool-size", 10));
        if (reuse) {
            if (pool == null) {
                final PgPool pgPool = PgPool.pool(vertx, IPostgresClient.getConnectOption(config),poolOptions);
                pool = new PostgresClientPool(vertx, pgPool, config);
            }
            return pool;
        } else {
            final PgPool pgPool = PgPool.pool(vertx, IPostgresClient.getConnectOption(config), poolOptions);
            return new PostgresClientPool(vertx, pgPool, config);
        }
    }

    @Override
    public Future<io.vertx.sqlclient.RowSet<io.vertx.sqlclient.Row>> preparedQuery(final String query, final Tuple tuple){
        final Promise<io.vertx.sqlclient.RowSet<io.vertx.sqlclient.Row>> future = Promise.promise();
        LoggerFactory.getLogger(getClass()).info("Connect postgres..."+Vertx.currentContext().deploymentID());
        PgConnection.connect(vertx, IPostgresClient.getConnectOption(config), connection -> {
            if(connection.succeeded()){
                connection.result().preparedQuery(query).execute(tuple,e->{
                    future.handle(e);
                    connection.result().close();
                });
            }else{
                future.fail(connection.cause());
                connection.result().close();
            }
        });
        return future.future();
    }

    @Override
    public Future<IPostgresTransaction> transaction(){
        Promise<IPostgresTransaction> promise = Promise.promise();
        LoggerFactory.getLogger(getClass()).info("Connect postgres..."+Vertx.currentContext().deploymentID());
        PgConnection.connect(vertx, IPostgresClient.getConnectOption(config), connection -> {
            if(connection.succeeded()){
                promise.complete(new PostgresTransaction(connection.result().begin(), e->{
                    connection.result().close();
                }));
            }else{
                promise.fail(connection.cause());
                connection.result().close();
            }
        });
        return promise.future();
    }

    public static class PostgresTransaction implements IPostgresTransaction {
        private static final Logger log = LoggerFactory.getLogger(PostgresClientPool.class);
        private final Transaction pgTransaction;
        private final Handler<AsyncResult<Void>> onFinish;
        private final List<Future> futures = new ArrayList<>();

        PostgresTransaction(final Transaction pgTransaction) {
            this(pgTransaction, e->{});
        }

        PostgresTransaction(final Transaction pgTransaction, final Handler<AsyncResult<Void>> onFinish) {
            this.pgTransaction = pgTransaction;
            this.onFinish = onFinish;
        }

        @Override
        public Future<RowSet<Row>> addPreparedQuery(String query, Tuple tuple) {
            final Future<RowSet<Row>> future = Future.future();
            this.pgTransaction.preparedQuery(query).execute(tuple,future.completer());
            futures.add(future);
            return future;
        }

        @Override
        public Future<Void> notify(final String channel, final String message) {
            final Future<Void> future = Future.future();
            //prepareQuery not works with notify allow only internal safe message
            this.pgTransaction.query(
                    "NOTIFY " + channel + ", '" + message + "'").execute(notified -> {
                        future.handle(notified.mapEmpty());
                        if (notified.failed()) {
                            log.error("Could not notify channel: " + channel);
                        }
                    });
            futures.add(future);
            return future;
        }

        @Override
        public Future<Void> commit() {
            return CompositeFuture.all(futures).compose(r -> {
                final Promise<Void> future = Promise.promise();
                this.pgTransaction.commit(future);
                return future.future();
            }).onComplete(onFinish);
        }

        @Override
        public Future<Void> rollback() {
            final Promise<Void> future = Promise.promise();
            this.pgTransaction.rollback(future);
            return future.future().onComplete(onFinish);
        }
    }
}
