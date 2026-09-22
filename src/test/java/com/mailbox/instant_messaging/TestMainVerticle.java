package com.mailbox.instant_messaging;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class TestMainVerticle {

  @BeforeEach
  void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
    JsonObject testConfig = new JsonObject()
      .put("jwt.secret", "oeoWHv1nEtmW68EL9fQ6K8+bV1kX5+cUbzt9fg8kAVY=");
    DeploymentOptions options = new DeploymentOptions().setConfig(testConfig);
    vertx.deployVerticle(new MainVerticle(), options).onComplete(testContext.succeeding(id -> testContext.completeNow()));
  }

  @Test
  void verticle_deployed(Vertx vertx, VertxTestContext testContext) throws Throwable {
    testContext.completeNow();
  }
}
