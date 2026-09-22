package com.mailbox.instant_messaging.database;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.serviceproxy.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;

public class DatabaseServiceImpl implements DatabaseService {

  private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseServiceImpl.class);
  private final HashMap<SqlQuery, String> sqlQueries;
  private final JDBCClient dbClient;

  public DatabaseServiceImpl(JDBCClient dbClient, HashMap<SqlQuery, String> sqlQueries, Handler<AsyncResult<DatabaseService>> readyHandler) {
    this.dbClient = dbClient;
    this.sqlQueries = sqlQueries;
    dbClient.getConnection(ar -> {
      if (ar.failed()) {
        LOGGER.error("Could not open a database connection", ar.cause());
        readyHandler.handle(Future.failedFuture(ar.cause()));
        return;
      }

      SQLConnection connection = ar.result();

      Future.<Void>future(promise -> connection.execute(sqlQueries.get(SqlQuery.CREATE_USERS_TABLE), promise))
        .compose(v -> Future.<Void>future(promise -> connection.execute(sqlQueries.get(SqlQuery.CREATE_MESSAGES_TABLE), promise)))
        .onComplete(res -> {

          connection.close();

          if (res.succeeded()) {
            readyHandler.handle(Future.succeededFuture(this));
          } else {
            LOGGER.error("Database preparation error | Code: {}", ErrorCodes.DB_ERROR, res.cause());
            readyHandler.handle(Future.failedFuture(new ServiceException(ErrorCodes.DB_ERROR.getCode(), res.cause().getMessage())));
          }
        });
    });
  }

  @Override
  public DatabaseService fetchLastMessages(Handler<AsyncResult<JsonArray>> resultHandler) {
    dbClient.query(sqlQueries.get(SqlQuery.GET_20_LAST_MESSAGES), res -> {
      if (res.succeeded()) {
        List<JsonObject> messages = res.result().getRows();
        messages.forEach(row -> row.getMap().replaceAll((k, v) -> v instanceof java.time.LocalDateTime ? v.toString() : v));
        resultHandler.handle(Future.succeededFuture(new JsonArray(messages)));
      } else {
        LOGGER.error("Database query error | Code: {}", ErrorCodes.DB_ERROR, res.cause());
        resultHandler.handle(Future.failedFuture(new ServiceException(ErrorCodes.DB_ERROR.getCode(), res.cause().getMessage())));
      }
    });
    return this;
  }

  @Override
  public DatabaseService fetchMessage(String id, Handler<AsyncResult<JsonObject>> resultHandler) {
    if (id == null || id.trim().isEmpty()) {
      LOGGER.warn("Action rejected: missing ID | Code: {}", ErrorCodes.BAD_ACTION);
      resultHandler.handle(Future.failedFuture(new ServiceException(ErrorCodes.BAD_ACTION.getCode(), "Message ID is required")));
      return this;
    }
    dbClient.queryWithParams(sqlQueries.get(SqlQuery.GET_MESSAGE), new JsonArray().add(id), fetch -> {
      if (fetch.succeeded()) {
        JsonObject response = new JsonObject();
        ResultSet resultSet = fetch.result();
        if (resultSet.getNumRows() == 0) {
          response.put("found", false);
        } else {
          response.put("found", true);
          JsonObject row = resultSet.getRows().get(0);
          row.getMap().replaceAll((key, value) -> value instanceof java.time.LocalDateTime ? value.toString() : value);
          response.put("message", row);
        }
        resultHandler.handle(Future.succeededFuture(response));
      } else {
        LOGGER.error("Database query error | Code: {}", ErrorCodes.DB_ERROR, fetch.cause());
        resultHandler.handle(Future.failedFuture(new ServiceException(ErrorCodes.DB_ERROR.getCode(), fetch.cause().getMessage())));
      }
    });
    return this;
  }

  @Override
  public DatabaseService createMessage(String author, String content, Handler<AsyncResult<Integer>> resultHandler) {
    if (author == null || content == null || author.trim().isEmpty() || content.trim().isEmpty()) {
      LOGGER.warn("Action rejected: missing parameters | Code: {}", ErrorCodes.BAD_ACTION);
      resultHandler.handle(Future.failedFuture(new ServiceException(ErrorCodes.BAD_ACTION.getCode(), "Author and content are mandatory")));
      return this;
    }
    dbClient.updateWithParams(sqlQueries.get(SqlQuery.CREATE_MESSAGE), new JsonArray().add(author).add(content), res -> {
      if (res.succeeded()) {
        Integer generatedId = res.result().getKeys().getInteger(0);
        resultHandler.handle(Future.succeededFuture(generatedId));
      } else {
        LOGGER.error("Database query error | Code: {}", ErrorCodes.DB_ERROR, res.cause());
        resultHandler.handle(Future.failedFuture(new ServiceException(ErrorCodes.DB_ERROR.getCode(), res.cause().getMessage())));
      }
    });
    return this;
  }

  @Override
  public DatabaseService deleteMessage(String id, Handler<AsyncResult<Void>> resultHandler) {
    if (id == null || id.trim().isEmpty()) {
      LOGGER.warn("Action rejected: missing ID | Code: {}", ErrorCodes.BAD_ACTION);
      resultHandler.handle(Future.failedFuture(new ServiceException(ErrorCodes.BAD_ACTION.getCode(), "Message ID is required")));
      return this;
    }
    dbClient.updateWithParams(sqlQueries.get(SqlQuery.DELETE_MESSAGE), new JsonArray().add(Integer.valueOf(id)), res -> {
      if (res.succeeded()) {
        resultHandler.handle(Future.succeededFuture());
      } else {
        LOGGER.error("Database query error | Code: {}", ErrorCodes.DB_ERROR, res.cause());
        resultHandler.handle(Future.failedFuture(new ServiceException(ErrorCodes.DB_ERROR.getCode(), res.cause().getMessage())));
      }
    });
    return this;
  }

  @Override
  public DatabaseService fetchUser(String username, Handler<AsyncResult<JsonObject>> resultHandler) {
    if (username == null || username.trim().isEmpty()) {
      LOGGER.warn("Action rejected: missing username | Code: {}", ErrorCodes.BAD_ACTION);
      resultHandler.handle(Future.failedFuture(new ServiceException(ErrorCodes.BAD_ACTION.getCode(), "Username is required")));
      return this;
    }
    dbClient.queryWithParams(sqlQueries.get(SqlQuery.GET_USER), new JsonArray().add(username), fetch -> {
      if (fetch.succeeded()) {
        JsonObject response = new JsonObject();
        ResultSet resultSet = fetch.result();
        if (resultSet.getNumRows() == 0) {
          response.put("found", false);
        } else {
          response.put("found", true);
          JsonObject row = resultSet.getRows().get(0);
          response.put("user", row);
        }
        resultHandler.handle(Future.succeededFuture(response));
      } else {
        LOGGER.error("Database query error | Code: {}", ErrorCodes.DB_ERROR, fetch.cause());
        resultHandler.handle(Future.failedFuture(new ServiceException(ErrorCodes.DB_ERROR.getCode(), fetch.cause().getMessage())));
      }
    });
    return this;
  }

  @Override
  public DatabaseService createUser(String username, String password, Handler<AsyncResult<Void>> resultHandler) {
    if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
      LOGGER.warn("Action rejected: missing credentials | Code: {}", ErrorCodes.BAD_ACTION);
      resultHandler.handle(Future.failedFuture(new ServiceException(ErrorCodes.BAD_ACTION.getCode(), "Username and password are required")));
      return this;
    }
    dbClient.updateWithParams(sqlQueries.get(SqlQuery.CREATE_USER), new JsonArray().add(username).add(password), res -> {
      if (res.succeeded()) {
        resultHandler.handle(Future.succeededFuture());
      } else {
        LOGGER.error("Database query error | Code: {}", ErrorCodes.DB_ERROR, res.cause());
        resultHandler.handle(Future.failedFuture(new ServiceException(ErrorCodes.DB_ERROR.getCode(), res.cause().getMessage())));
      }
    });
    return this;
  }
}
