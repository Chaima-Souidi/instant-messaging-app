package com.mailbox.instant_messaging;

import com.mailbox.instant_messaging.database.DatabaseService;
import com.mailbox.instant_messaging.database.DatabaseVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
public class DatabaseServiceTest {

  private DatabaseService service;

  @BeforeEach
  void prepare(Vertx vertx, VertxTestContext testContext) {
    JsonObject conf = new JsonObject()
      .put(DatabaseVerticle.CONFIG_APP_DB_JDBC_URL, "jdbc:hsqldb:mem:testdb;shutdown=true")
      .put(DatabaseVerticle.CONFIG_APP_DB_JDBC_MAX_POOL_SIZE, 4);

    vertx.deployVerticle(new DatabaseVerticle(), new DeploymentOptions().setConfig(conf), testContext.succeeding(id -> {
      service = DatabaseService.createProxy(vertx, DatabaseVerticle.CONFIG_APP_DB_QUEUE);
      testContext.completeNow();
    }));
  }

  @AfterEach
  public void finish(Vertx vertx, VertxTestContext testContext) {
    vertx.close(testContext.succeedingThenComplete());
  }

  @Test
  void user_operations(Vertx vertx, VertxTestContext testContext) throws Throwable {
    service.createUser("Alice", "password", testContext.succeeding(userCreated -> {
      service.fetchUser("Alice", testContext.succeeding(fetchedUser -> {
        assertTrue(fetchedUser.getBoolean("found"));
        assertTrue(fetchedUser.containsKey("user"));

        JsonObject userObj = fetchedUser.getJsonObject("user");

        assertEquals(3, userObj.size());

        assertNotNull(userObj.getInteger("ID"));
        assertEquals("Alice", userObj.getString("USERNAME"));
        assertEquals("password", userObj.getString("PASSWORD"));

        testContext.completeNow();
      }));
    }));
    testContext.awaitCompletion(5000, TimeUnit.MILLISECONDS);
  }

  @Test
  void message_operations(Vertx vertx, VertxTestContext testContext) throws Throwable {
    service.createUser("Alice", "password", testContext.succeeding(userCreated -> {
      service.createMessage("Alice", "message", testContext.succeeding(messageCreated -> {
        service.fetchMessage("0", testContext.succeeding(fetchedMessage -> {

          assertTrue(fetchedMessage.getBoolean("found"));
          assertTrue(fetchedMessage.containsKey("message"));

          JsonObject messageObj = fetchedMessage.getJsonObject("message");

          assertEquals(5, messageObj.size());

          assertEquals("Alice", messageObj.getString("AUTHOR"));
          assertEquals("message", messageObj.getString("CONTENT"));
          assertNotNull(messageObj.getString("CREATED_AT"));

          service.deleteMessage("0", testContext.succeeding(messageDeleted -> {
            service.fetchMessage("0", testContext.succeeding(deletedCheck -> {
              assertFalse(deletedCheck.getBoolean("found"));
              assertFalse(deletedCheck.containsKey("message"));
              testContext.completeNow();
            }));
          }));
        }));
      }));
    }));
    testContext.awaitCompletion(5000, TimeUnit.MILLISECONDS);
  }

}
