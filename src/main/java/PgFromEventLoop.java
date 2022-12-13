import io.vertx.core.AbstractVerticle;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.sqlclient.Tuple;
import sample.pgssl.IPostgresClient;

/*
 * This Java source file was generated by the Gradle 'init' task.
 */
public class PgFromEventLoop  extends AbstractVerticle {
    public static Logger log = LoggerFactory.getLogger(PgFromEventLoop.class);
    public static final String ADDRESS = "pgssl.eventloop";
    @Override
    public void start() throws Exception {
        super.start();
        final IPostgresClient client = IPostgresClient.create(vertx, config(), false, true);
        vertx.eventBus().consumer(ADDRESS, query -> {
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
