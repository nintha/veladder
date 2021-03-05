package top.nintha.veladder.controller;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import top.nintha.veladder.AppLauncher;

import java.util.concurrent.ThreadLocalRandom;

@ExtendWith(VertxExtension.class)
class HelloControllerTest {
    private final int port = ThreadLocalRandom.current().nextInt(12000, 22000);

    @BeforeEach
    @DisplayName("Deploy a verticle")
    void prepare(Vertx vertx, VertxTestContext testContext) {
        vertx.deployVerticle(new AppLauncher(port), testContext.succeedingThenComplete());
    }

    @Test
    void helloWorld(Vertx vertx, VertxTestContext ctx) {
        final WebClient client = WebClient.create(vertx);
        client
                .get(port, "127.0.0.1", "/hello/world")
                .send()
                .onComplete(ctx.succeeding(buffer -> ctx.verify(() -> {
                    Assertions.assertEquals("hello world", buffer.bodyAsString().toLowerCase());
                    ctx.completeNow();
                })));
    }

    @Test
    void helloVoid(Vertx vertx, VertxTestContext ctx) {
        final WebClient client = WebClient.create(vertx);
        client
                .get(port, "127.0.0.1", "/hello/void")
                .send()
                .onComplete(ctx.succeeding(buffer -> ctx.verify(() -> {
                    Assertions.assertNull(buffer.body());
                    ctx.completeNow();
                })));
    }

    @Test
    void echo() {
    }

    @Test
    void helloArray() {
    }

    @Test
    void queryArray() {
    }

    @Test
    void queryBean() {
    }

    @Test
    void postRequestBody() {
    }

    @Test
    void helloPathVariable() {
    }
}