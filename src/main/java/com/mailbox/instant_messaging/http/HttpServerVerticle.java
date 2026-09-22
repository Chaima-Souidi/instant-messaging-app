package com.mailbox.instant_messaging.http;

import com.github.rjeschke.txtmark.Processor;
import com.mailbox.instant_messaging.database.DatabaseService;
import com.mailbox.instant_messaging.database.ErrorCodes;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.templ.freemarker.FreeMarkerTemplateEngine;
import io.vertx.serviceproxy.ServiceException;
import org.mindrot.jbcrypt.BCrypt;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.mailbox.instant_messaging.database.DatabaseVerticle.CONFIG_APP_DB_QUEUE;

public class HttpServerVerticle extends AbstractVerticle {

  private static final String CONFIG_HTTP_SERVER_PORT = "http.server.port";
  private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerVerticle.class);
  private FreeMarkerTemplateEngine templateEngine;
  private DatabaseService dbService;
  private JWTAuth jwtProvider;


  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    templateEngine = FreeMarkerTemplateEngine.create(vertx);

    String appDbQueue = config().getString(CONFIG_APP_DB_QUEUE, "appdb.queue");

    dbService = DatabaseService.createProxy(vertx, appDbQueue);

    String jwtSecret = config().getString("jwt.secret");

    if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
      throw new IllegalStateException("FATAL ERROR: JWT secret is missing from configuration!");
    }

    JWTAuthOptions authConfig = new JWTAuthOptions().addPubSecKey(new PubSecKeyOptions().setAlgorithm("HS256").setBuffer(jwtSecret));

    jwtProvider = JWTAuth.create(vertx, authConfig);

    HttpServer server = vertx.createHttpServer();
    Router router = Router.router(vertx);

    SockJSBridgeOptions options = new SockJSBridgeOptions().addInboundPermitted(new PermittedOptions().setAddress(appDbQueue)).addOutboundPermitted(new PermittedOptions().setAddress("chat.to.client"));

    SockJSHandler sockJSHandler = SockJSHandler.create(vertx);

    router.post().handler(BodyHandler.create());

    router.route("/eventbus/*").subRouter(sockJSHandler.bridge(options));
    router.route("/assets/*").handler(StaticHandler.create("webroot"));
    router.route().handler(this::i18nMiddleware);
    router.get("/").handler(this::redirectHandler);
    router.get("/home/:username").handler(this::authMiddleware).handler(this::homeHandler);
    router.get("/message/:message").handler(this::messageRenderingHandler);
    router.get("/login").handler(this::pageLoginHandler);
    router.post("/login").handler(this::loginHandler);
    router.get("/register").handler(this::pageRegisterHandler);
    router.post("/register").handler(this::registerHandler);
    router.post("/create").handler(this::authMiddleware).handler(this::messageCreateHandler);
    router.post("/delete").handler(this::authMiddleware).handler(this::messageDeletionHandler);
    router.get("/lang/:language").handler(this::changeLanguageHandler);

    int portNumber = config().getInteger(CONFIG_HTTP_SERVER_PORT, 8080);

    server.requestHandler(router).listen(portNumber, ar -> {
      if (ar.succeeded()) {
        LOGGER.info("HTTP server running on port " + portNumber);
        startPromise.complete();
      } else {
        LOGGER.error("Could not start a HTTP server", ar.cause());
        startPromise.fail(ar.cause());
      }
    });
  }

  private void messageDeletionHandler(RoutingContext context) {
    String loggedInUser = context.get("authenticated_user");
    String messageId = context.request().getFormAttribute("id");

    dbService.fetchMessage(messageId, fetchReply -> {
      if (fetchReply.succeeded()) {
        JsonObject body = fetchReply.result();

        if (body.getBoolean("found")) {
          String author = body.getJsonObject("message").getString("AUTHOR");

          if (loggedInUser.equals(author)) {
            dbService.deleteMessage(messageId, deleteReply -> {
              if (deleteReply.succeeded()) {
                context.response().setStatusCode(200).end();
              } else {
                handleServiceError(context, deleteReply.cause());
              }
            });
          } else {
            LOGGER.warn("SECURITY ALERT: User '" + loggedInUser + "' attempted to delete message ID " + messageId + " belonging to '" + author + "'.");
            context.response().setStatusCode(403).end("Forbidden: You can only delete your own messages");
          }
        } else {
          context.response().setStatusCode(404).end("Message not found");
        }
      } else {
        handleServiceError(context, fetchReply.cause());
      }
    });
  }

  private void authMiddleware(RoutingContext context) {
    Cookie cookie = context.request().getCookie("jwt_token");

    if (cookie != null) {
      jwtProvider.authenticate(new JsonObject().put("token", cookie.getValue()), res -> {
        if (res.succeeded()) {
          String username = res.result().principal().getString("username");
          context.put("authenticated_user", username);
          context.next();
        } else {
          context.response().setStatusCode(401).end("Not allowed");
        }
      });
    } else {
      context.response().setStatusCode(401).end("Missing token");
    }
  }

  private void i18nMiddleware(RoutingContext context) {
    String lang = "en";
    Cookie langCookie = context.request().getCookie("lang");
    if (langCookie != null) {
      String requestedLang = langCookie.getValue();
      if ("fr".equals(requestedLang) || "en".equals(requestedLang) || "es".equals(requestedLang)) {
        lang = requestedLang;
      } else {
        LOGGER.warn("SECURITY ALERT: Invalid language requested : '" + requestedLang + "'");
      }
    }

    vertx.fileSystem().readFile("locales/" + lang + ".json", readResult -> {
      if (readResult.succeeded()) {
        context.put("i18n", readResult.result().toJsonObject());
      } else {
        context.put("i18n", new JsonObject());
      }

      context.next();
    });
  }

  private void changeLanguageHandler(RoutingContext context) {
    String selectedLang = context.request().getParam("language");

    Cookie langCookie = Cookie.cookie("lang", selectedLang);
    langCookie.setPath("/");
    langCookie.setMaxAge(31536000);

    String previousPage = context.request().getHeader("Referer");
    if (previousPage == null || previousPage.isEmpty()) {
      previousPage = "/login";
    }

    context.addCookie(langCookie).redirect(previousPage);
  }

  private void homeHandler(RoutingContext context) {
    String loggedInUser = context.request().getParam("username");
    dbService.fetchLastMessages(reply -> {
      if (reply.succeeded()) {
        context.put("title", "Home");
        context.put("messages", reply.result().getList());
        context.put("username", loggedInUser);

        templateEngine.render(context.data(), "templates/home.ftl", ar -> {
          if (ar.succeeded()) {
            context.response().putHeader("Content-Type", "text/html");
            context.response().end(ar.result());
          } else {
            context.fail(ar.cause());
          }
        });
      } else {
        handleServiceError(context, reply.cause());
      }
    });
  }

  private void messageRenderingHandler(RoutingContext context) {
    String idParam = context.request().getParam("message");
    dbService.fetchMessage(idParam, reply -> {
      if (reply.succeeded()) {
        JsonObject body = reply.result();

        if (body.getBoolean("found")) {
          JsonObject messageObj = body.getJsonObject("message");

          Integer id = messageObj.getInteger("ID");
          String author = messageObj.getString("AUTHOR");
          String rawContent = messageObj.getString("CONTENT");
          String created_at = messageObj.getString("CREATED_AT");

          context.put("title", "Message");
          context.put("id", id);
          context.put("author", author);
          context.put("rawContent", rawContent);
          context.put("content", Processor.process(rawContent));
          context.put("timestamp", created_at);
          templateEngine.render(context.data(), "templates/message.ftl", ar -> {
            if (ar.succeeded()) {
              context.response().putHeader("Content-Type", "text/html");
              context.response().end(ar.result());
            } else {
              context.fail(ar.cause());
            }
          });
        } else {
          context.response().setStatusCode(404).end("Message not found");
        }
      } else {
        handleServiceError(context, reply.cause());
      }
    });
  }

  private void pageLoginHandler(RoutingContext context) {
    context.put("title", "Log In");

    templateEngine.render(context.data(), "templates/login.ftl", ar -> {
      if (ar.succeeded()) {
        context.response().putHeader("Content-Type", "text/html").end(ar.result());
      } else {
        context.fail(ar.cause());
      }
    });
  }

  private void loginHandler(RoutingContext context) {
    String username = context.request().getFormAttribute("username");
    String typedPassword = context.request().getFormAttribute("password");
    dbService.fetchUser(username, reply -> {
      if (reply.succeeded()) {
        JsonObject body = reply.result();

        if (body.getBoolean("found")) {
          JsonObject userRow = body.getJsonObject("user");

          String passwordDb = userRow.getString("PASSWORD");

          if (BCrypt.checkpw(typedPassword, passwordDb)) {
            String token = jwtProvider.generateToken(new JsonObject().put("username", username));
            Cookie cookie = Cookie.cookie("jwt_token", token);

            cookie.setHttpOnly(true);
            cookie.setMaxAge(3600);
            cookie.setPath("/");

            String location = "/home/" + username;

            context.addCookie(cookie).redirect(location);
          } else {
            LOGGER.warn("Failed authentication attempt for username: '" + username + "' (Incorrect password).");
            context.redirect("/login?error=true");
          }
        } else {
          LOGGER.warn("Failed authentication attempt for unknown username: '" + username + "'.");
          context.redirect("/login?error=true");
        }

      } else {
        handleServiceError(context, reply.cause());
      }
    });
  }

  private void redirectHandler(RoutingContext context) {
    Cookie cookie = context.request().getCookie("jwt_token");

    if (cookie != null) {
      jwtProvider.authenticate(new JsonObject().put("token", cookie.getValue()), res -> {
        if (res.succeeded()) {
          String username = res.result().principal().getString("username");
          context.redirect("/home/" + username);
        } else {
          context.redirect("/login");
        }
      });
    } else {
      context.redirect("/login");
    }
  }

  private void registerHandler(RoutingContext context) {
    String username = context.request().getFormAttribute("username");
    String password = context.request().getFormAttribute("password");
    String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

    dbService.createUser(username, hashedPassword, reply -> {
      if (reply.succeeded()) {
        String token = jwtProvider.generateToken(new JsonObject().put("username", username));
        Cookie cookie = Cookie.cookie("jwt_token", token);

        cookie.setHttpOnly(true);
        cookie.setMaxAge(3600);
        cookie.setPath("/");

        String location = "/home/" + username;

        context.addCookie(cookie).redirect("/home/" + username);
      } else {
        handleServiceError(context, reply.cause());
      }
    });
  }

  private void pageRegisterHandler(RoutingContext context) {
    context.put("title", "Register");

    templateEngine.render(context.data(), "templates/register.ftl", ar -> {
      if (ar.succeeded()) {
        context.response().putHeader("Content-Type", "text/html").end(ar.result());
      } else {
        context.fail(ar.cause());
      }
    });
  }

  private void messageCreateHandler(RoutingContext context) {
    String author = context.get("authenticated_user");
    String rawContent = context.request().getFormAttribute("content");
    String htmlContent = Processor.process(rawContent);
    PolicyFactory policy = Sanitizers.FORMATTING.and(Sanitizers.LINKS);
    String safeContent = policy.sanitize(htmlContent);

    dbService.createMessage(author, safeContent, reply -> {
      if (reply.succeeded()) {
        Integer newId = reply.result();
        JsonObject newMessage = new JsonObject().put("id", newId).put("author", author).put("content", safeContent);
        vertx.eventBus().publish("chat.to.client", newMessage);
        context.response().setStatusCode(200).end();
      } else {
        LOGGER.error("Database error while creating message for user '" + author + "': " + reply.cause().getMessage(), reply.cause());
        handleServiceError(context, reply.cause());
      }
    });
  }


  private void handleServiceError(RoutingContext context, Throwable cause) {
    if (cause instanceof ServiceException exc) {
      if (exc.failureCode() == ErrorCodes.BAD_ACTION.getCode()) {
        context.response().setStatusCode(400).end("Bad Request: " + exc.getMessage());
      } else if (exc.failureCode() == ErrorCodes.DB_ERROR.getCode()) {
        context.response().setStatusCode(500).end("Internal Server Error: Database issue.");
      } else {
        context.response().setStatusCode(500).end("Server Error: " + exc.getMessage());
      }
    } else {
      context.response().setStatusCode(500).end("Unexpected system error.");
    }
  }
}
