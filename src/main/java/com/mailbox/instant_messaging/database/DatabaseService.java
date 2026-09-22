package com.mailbox.instant_messaging.database;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;

import java.util.HashMap;

@ProxyGen
public interface DatabaseService {

  static DatabaseService create(JDBCClient dbClient, HashMap<SqlQuery, String> sqlQueries, Handler<AsyncResult<DatabaseService>> readyHandler) {
    return new DatabaseServiceImpl(dbClient, sqlQueries, readyHandler);
  }

  static DatabaseService createProxy(Vertx vertx, String address) {
    return new DatabaseServiceVertxEBProxy(vertx, address);
  }

  @Fluent
  DatabaseService fetchLastMessages(Handler<AsyncResult<JsonArray>> resultHandler);

  @Fluent
  DatabaseService fetchMessage(String id, Handler<AsyncResult<JsonObject>> resultHandler);

  @Fluent
  DatabaseService createMessage(String author, String content, Handler<AsyncResult<Integer>> resultHandler);

  @Fluent
  DatabaseService deleteMessage(String id, Handler<AsyncResult<Void>> resultHandler);

  @Fluent
  DatabaseService fetchUser(String username, Handler<AsyncResult<JsonObject>> resultHandler);

  @Fluent
  DatabaseService createUser(String username, String password, Handler<AsyncResult<Void>> resultHandler);
}
