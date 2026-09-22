package com.mailbox.instant_messaging;

import com.mailbox.instant_messaging.database.DatabaseVerticle;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.ext.jdbc.JDBCClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainVerticle extends AbstractVerticle {


  private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);
  private JDBCClient dbClient;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    Future<String> dbVerticleDeployment = vertx.deployVerticle(new DatabaseVerticle(), new DeploymentOptions().setConfig(config()));

    dbVerticleDeployment.compose(id -> vertx.deployVerticle("com.mailbox.instant_messaging.http.HttpServerVerticle",
        new DeploymentOptions().setConfig(config()).setInstances(2)))
      .onComplete(ar -> {
        if (ar.succeeded()) {
          LOGGER.info("Deployment successful: All Verticles are online!");
          startPromise.complete();
        } else {
          LOGGER.error("Deployment failed", ar.cause());
          startPromise.fail(ar.cause());
        }
      });
  }

}
