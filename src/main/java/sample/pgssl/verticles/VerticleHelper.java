package sample.pgssl.verticles;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.sqlclient.Tuple;
import sample.pgssl.client.IPostgresClient;

public class VerticleHelper {

    public static void createConsumer(final Vertx vertx, final String address, final IPostgresClient client, final Logger log){
        vertx.eventBus().consumer(address, query -> {
            client.preparedQuery("INSERT INTO test.data(name) VALUES($1)", Tuple.of("test")).onComplete(result -> {
                if(result.succeeded()){
                    query.reply("ok");
                }else{
                    log.error("Query failed: ", result.cause());
                    query.fail(500, result.cause().getMessage());
                }
            });
        });
    }
}
