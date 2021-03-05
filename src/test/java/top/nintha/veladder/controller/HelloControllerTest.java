package top.nintha.veladder.controller;

import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import top.nintha.veladder.AppLauncher;
import top.nintha.veladder.entity.MockUser;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@ExtendWith(VertxExtension.class)
class HelloControllerTest {
    private final int port = ThreadLocalRandom.current().nextInt(12000, 22000);

    @BeforeEach
    @DisplayName("Deploy a verticle")
    void prepare(Vertx vertx, VertxTestContext testContext) {
        vertx.exceptionHandler(t -> log.error("[VERTX]", t));
        vertx.deployVerticle(new AppLauncher(port), testContext.succeedingThenComplete());
    }

    @Test
    void helloWorld(Vertx vertx, VertxTestContext ctx) {
        WebClient.create(vertx)
                .get(port, "127.0.0.1", "/hello/world")
                .send()
                .onComplete(ctx.succeeding(buffer -> ctx.verify(() -> {
                    Assertions.assertEquals("hello world", buffer.bodyAsString().toLowerCase());
                    ctx.completeNow();
                })));
    }

    @Test
    void helloVoid(Vertx vertx, VertxTestContext ctx) {
        WebClient.create(vertx)
                .get(port, "127.0.0.1", "/hello/void")
                .send()
                .onComplete(ctx.succeeding(buffer -> ctx.verify(() -> {
                    Assertions.assertNull(buffer.body());
                    ctx.completeNow();
                })));
    }

    @Test
    void echoText(Vertx vertx, VertxTestContext ctx) {
        String text = UUID.randomUUID().toString();
        WebClient.create(vertx)
                .get(port, "127.0.0.1", "/echo/text")
                .addQueryParam("text", text)
                .send()
                .onComplete(ctx.succeeding(buffer -> ctx.verify(() -> {
                    Assertions.assertEquals(text, buffer.bodyAsString());
                    ctx.completeNow();
                })));
    }

    @Test
    void echoObjectWithQuery(Vertx vertx, VertxTestContext ctx) {
        String text = UUID.randomUUID().toString();
        Long number = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE);
        int code = ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);
        WebClient.create(vertx)
                .get(port, "127.0.0.1", "/echo/object")
                .addQueryParam("text", text)
                .addQueryParam("number", number.toString())
                .addQueryParam("code", String.valueOf(code))
                .send()
                .onComplete(ctx.succeeding(buffer -> ctx.verify(() -> {
                    JsonObject entries = buffer.bodyAsJsonObject();
                    Assertions.assertEquals(text, entries.getString("text"));
                    Assertions.assertEquals(number, entries.getLong("number"));
                    Assertions.assertEquals(code, entries.getInteger("code"));
                    ctx.completeNow();
                })));
    }

    @Test
    void echoObjectWithForm(Vertx vertx, VertxTestContext ctx) {
        String text = UUID.randomUUID().toString();
        Long number = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE);
        int code = ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);

        MultiMap form = MultiMap.caseInsensitiveMultiMap();
        form.add("text", text);
        form.add("number", String.valueOf(number));
        form.add("code", String.valueOf(code));

        WebClient.create(vertx)
                .post(port, "127.0.0.1", "/echo/object")
                .sendForm(form)
                .onComplete(ctx.succeeding(buffer -> ctx.verify(() -> {
                    JsonObject entries = buffer.bodyAsJsonObject();
                    Assertions.assertEquals(text, entries.getString("text"));
                    Assertions.assertEquals(number, entries.getLong("number"));
                    Assertions.assertEquals(code, entries.getInteger("code"));
                    ctx.completeNow();
                })));
    }

    @Test
    void helloArray(Vertx vertx, VertxTestContext ctx) {
        MultiMap form = MultiMap.caseInsensitiveMultiMap();
        form.add("ids", "0");
        form.add("ids", "1");
        form.add("names", "name0");
        form.add("names", "name1");

        WebClient.create(vertx)
                .post(port, "127.0.0.1", "/hello/array")
                .sendForm(form)
                .onComplete(ctx.succeeding(buffer -> ctx.verify(() -> {
                    var entries = buffer.bodyAsJsonObject();
                    Assertions.assertEquals(new JsonArray().add(0).add(1), entries.getJsonArray("ids"));
                    Assertions.assertEquals(new JsonArray().add("name0").add("name1"), entries.getJsonArray("names"));
                    ctx.completeNow();
                })));
    }

    @Test
    void helloList(Vertx vertx, VertxTestContext ctx) {
        MultiMap form = MultiMap.caseInsensitiveMultiMap();
        form.add("ids", "0");
        form.add("ids", "1");
        form.add("names", "name0");
        form.add("names", "name1");
        form.add("rawList", "raw");

        WebClient.create(vertx)
                .post(port, "127.0.0.1", "/hello/list")
                .sendForm(form)
                .onComplete(ctx.succeeding(buffer -> ctx.verify(() -> {
                    var entries = buffer.bodyAsJsonObject();
                    Assertions.assertEquals(new JsonArray().add(0).add(1), entries.getJsonArray("ids"));
                    Assertions.assertEquals(new JsonArray().add("name0").add("name1"), entries.getJsonArray("names"));
                    Assertions.assertEquals(new JsonArray().add("raw"), entries.getJsonArray("rawList"));
                    ctx.completeNow();
                })));
    }

    @Test
    void queryBean(Vertx vertx, VertxTestContext ctx) {
        long id = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE);
        String name = "name" + UUID.randomUUID().toString();
        String tag0 = "tag0" + UUID.randomUUID().toString();
        String tag1 = "tag1" + UUID.randomUUID().toString();
        WebClient.create(vertx)
                .get(port, "127.0.0.1", "/query/bean")
                .addQueryParam("id", Long.toString(id))
                .addQueryParam("name", name)
                .addQueryParam("tags", tag0)
                .addQueryParam("tags", tag1)
                .send()
                .onComplete(ctx.succeeding(buffer -> ctx.verify(() -> {
                    MockUser user = buffer.bodyAsJson(MockUser.class);
                    Assertions.assertEquals(id, user.getId());
                    Assertions.assertEquals(name, user.getName());
                    Assertions.assertEquals(tag0, user.getTags().get(0));
                    Assertions.assertEquals(tag1, user.getTags().get(1));
                    ctx.completeNow();
                })));
    }

    @Test
    void postRequestBody(Vertx vertx, VertxTestContext ctx) {
        long id = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE);
        String name = "name" + UUID.randomUUID().toString();
        String tag0 = "tag0" + UUID.randomUUID().toString();
        String tag1 = "tag1" + UUID.randomUUID().toString();

        MockUser mu = new MockUser();
        mu.setId(id);
        mu.setName(name);
        mu.setTags(List.of(tag0, tag1));

        WebClient.create(vertx)
                .post(port, "127.0.0.1", "/post/body")
                .sendJson(mu)
                .onComplete(ctx.succeeding(buffer -> ctx.verify(() -> {
                    MockUser user = buffer.bodyAsJson(MockUser.class);
                    Assertions.assertEquals(id, user.getId());
                    Assertions.assertEquals(name, user.getName());
                    Assertions.assertEquals(tag0, user.getTags().get(0));
                    Assertions.assertEquals(tag1, user.getTags().get(1));
                    ctx.completeNow();
                })));
    }

    @Test
    void helloPathVariable(Vertx vertx, VertxTestContext ctx) {
        final String token = UUID.randomUUID().toString();
        final Long id = 1234L;
        String uri  = String.format("/hello/path/variable/%s/%s", token, id);

        WebClient.create(vertx)
                .post(port, "127.0.0.1", uri)
                .send()
                .onComplete(ctx.succeeding(buffer -> ctx.verify(() -> {
                    Assertions.assertEquals(token + id, buffer.bodyAsString());
                    ctx.completeNow();
                })));

    }

}