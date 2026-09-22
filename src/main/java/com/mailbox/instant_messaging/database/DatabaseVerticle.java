package com.mailbox.instant_messaging.database;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.serviceproxy.ProxyHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;


public class DatabaseVerticle extends AbstractVerticle {

  public static final String CONFIG_APP_DB_JDBC_URL = "appdb.jdbc.url";
  public static final String CONFIG_APP_DB_JDBC_DRIVER_CLASS = "appdb.jdbc.driver_class";
  public static final String CONFIG_APP_DB_JDBC_MAX_POOL_SIZE = "appdb.jdbc.max_pool_size";
  public static final String CONFIG_APP_DB_SQL_QUERIES_RESOURCE_FILE = "appdb.sqlqueries.resource.file";
  public static final String CONFIG_APP_DB_QUEUE = "appdb.queue";
  private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseVerticle.class);

  private HashMap<SqlQuery, String> loadQueries() throws IOException {
    String queriesFile = config().getString(CONFIG_APP_DB_SQL_QUERIES_RESOURCE_FILE);
    InputStream queriesInputStream;
    if (queriesFile != null) {
      queriesInputStream = new FileInputStream(queriesFile);
    } else {
      queriesInputStream = getClass().getResourceAsStream("/db-queries.properties");
    }

    try (queriesInputStream) {
      Properties queriesProps = new Properties();
      queriesProps.load(queriesInputStream);
      queriesInputStream.close();

      HashMap<SqlQuery, String> sqlQueries = new HashMap<>();

      sqlQueries.put(SqlQuery.CREATE_MESSAGES_TABLE, queriesProps.getProperty("create-messages-table"));
      sqlQueries.put(SqlQuery.GET_20_LAST_MESSAGES, queriesProps.getProperty("twenty-last-messages"));
      sqlQueries.put(SqlQuery.GET_MESSAGE, queriesProps.getProperty("get-message"));
      sqlQueries.put(SqlQuery.GET_ALL_MESSAGES, queriesProps.getProperty("get-all-messages"));
      sqlQueries.put(SqlQuery.DELETE_MESSAGE, queriesProps.getProperty("delete-message"));
      sqlQueries.put(SqlQuery.SAVE_MESSAGE, queriesProps.getProperty("save-message"));
      sqlQueries.put(SqlQuery.CREATE_MESSAGE, queriesProps.getProperty("create-message"));

      sqlQueries.put(SqlQuery.CREATE_USERS_TABLE, queriesProps.getProperty("create-users-table"));
      sqlQueries.put(SqlQuery.GET_USER, queriesProps.getProperty("get-user"));
      sqlQueries.put(SqlQuery.CREATE_USER, queriesProps.getProperty("create-user"));

      return sqlQueries;
    }
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    HashMap<SqlQuery, String> sqlQueries = loadQueries();

    JDBCClient dbClient = JDBCClient.createShared(vertx, new JsonObject()
      .put("url", config().getValue(CONFIG_APP_DB_JDBC_URL, "jdbc:hsqldb:file:db/app"))
      .put("driver_class", config().getValue(CONFIG_APP_DB_JDBC_DRIVER_CLASS, "org.hsqldb.jdbcDriver"))
      .put("max_pool_size", config().getValue(CONFIG_APP_DB_JDBC_MAX_POOL_SIZE, 30)));

    DatabaseService.create(dbClient, sqlQueries, ready -> {
      if (ready.succeeded()) {
        LOGGER.info("Database service successfully initialized and registered on event bus.");
        ProxyHelper.registerService(DatabaseService.class, vertx, ready.result(), CONFIG_APP_DB_QUEUE);
        startPromise.complete();
      } else {
        LOGGER.error("Failed to initialize database service: " + ready.cause().getMessage(), ready.cause());
        startPromise.fail(ready.cause());
      }
    });
  }

}
