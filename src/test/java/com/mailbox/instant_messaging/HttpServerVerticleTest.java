package com.mailbox.instant_messaging;

import com.mailbox.instant_messaging.database.DatabaseService;
import com.mailbox.instant_messaging.database.DatabaseVerticle;
import com.mailbox.instant_messaging.http.HttpServerVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
public class HttpServerVerticleTest {

  private WebClient client;
  private JWTAuth jwtProvider;
  private DatabaseService service;

  @AfterEach
  public void finish(Vertx vertx, VertxTestContext testContext) {
    vertx.close(testContext.succeedingThenComplete());
  }

  @BeforeEach
  void prepare(Vertx vertx, VertxTestContext testContext) {
    JsonObject conf = new JsonObject()
      .put(DatabaseVerticle.CONFIG_APP_DB_JDBC_URL, "jdbc:hsqldb:mem:testdb;shutdown=true")
      .put(DatabaseVerticle.CONFIG_APP_DB_JDBC_MAX_POOL_SIZE, 4)
      .put("jwt.secret", "RSPLzF97/DeVghP+GKgJImxSCDjujp7c0zdQgli8gRk=");

    JWTAuthOptions authConfig = new JWTAuthOptions()
      .addPubSecKey(new PubSecKeyOptions()
        .setAlgorithm("HS256")
        .setBuffer("RSPLzF97/DeVghP+GKgJImxSCDjujp7c0zdQgli8gRk="));
    jwtProvider = JWTAuth.create(vertx, authConfig);

    client = WebClient.create(vertx, new WebClientOptions().setDefaultHost("localhost").setDefaultPort(8080));

    vertx.deployVerticle(new DatabaseVerticle(), new DeploymentOptions().setConfig(conf), testContext.succeeding(id1 -> {

      service = DatabaseService.createProxy(vertx, "appdb.queue");

      vertx.deployVerticle(new HttpServerVerticle(), new DeploymentOptions().setConfig(conf), testContext.succeeding(id2 -> {
        testContext.completeNow();
      }));
    }));
  }

  @Test
  void cannot_delete_others_message(Vertx vertx, VertxTestContext testContext) {
    service.createUser("Alice", "password", testContext.succeeding(aliceCreated -> {
      service.createMessage("Alice", "The secret message", testContext.succeeding(messageId -> {
        service.createUser("Bob", "password", testContext.succeeding(bobCreated -> {

          String bobToken = jwtProvider.generateToken(new JsonObject().put("username", "Bob"));

          client.post("/delete")
            .putHeader("Cookie", "jwt_token=" + bobToken)
            .sendForm(io.vertx.core.MultiMap.caseInsensitiveMultiMap().add("id", String.valueOf(messageId)),
              testContext.succeeding(response -> {

                testContext.verify(() -> {
                  assertEquals(403, response.statusCode(), "The server should block the request with a 403 Forbidden");
                  testContext.completeNow();
                });

              }));

        }));
      }));
    }));
  }

  @Test
  void should_escape_xss_in_html_and_js(Vertx vertx, VertxTestContext testContext) {
    String maliciousUser = "\"-alert('XSS')-\"";
    String maliciousMessage = "<script>alert('Hack')</script>";

    service.createUser(maliciousUser, "password", testContext.succeeding(userCreated -> {
      service.createMessage(maliciousUser, maliciousMessage, testContext.succeeding(msgId -> {

        String token = jwtProvider.generateToken(new JsonObject().put("username", maliciousUser));

        String encodedUser = URLEncoder.encode(maliciousUser, StandardCharsets.UTF_8);

        client.get("/home/" + encodedUser)
          .putHeader("Cookie", "jwt_token=" + token)
          .send(testContext.succeeding(response -> {

            testContext.verify(() -> {
              assertEquals(200, response.statusCode(), "The page should load correctly");

              String htmlBody = response.bodyAsString();

              assertTrue(htmlBody.contains("&lt;script&gt;alert(&#39;Hack&#39;)&lt;/script&gt;"), "The HTML message was not escaped!");
              assertFalse(htmlBody.contains(maliciousMessage), "The HTML XSS vulnerability is open!");

              assertTrue(htmlBody.contains("username: \"\\\"-alert(\\'XSS\\')-\\\"\""), "The username in JS was not escaped!");

              testContext.completeNow();
            });

          }));
      }));
    }));
  }

}
