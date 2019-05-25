package top.nintha.veladder.controller;

import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import top.nintha.veladder.annotations.RequestMapping;
import top.nintha.veladder.annotations.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController(name = "rest one")
public class HelloController {

    @RequestMapping("hello/world")
    public String helloWorld() {
        return "Hello world";
    }

    @RequestMapping("echo")
    public Map<String, Object> echo(String message, Long token, int code, RoutingContext ctx) {
        log.info("uri={}",  ctx.request().absoluteURI());

        log.info("message={}, token={}, code={}", message, token, code);
        HashMap<String, Object> map = new HashMap<>();
        map.put("message", message);
        map.put("token", token);
        map.put("code", code);
        return map;
    }
}
