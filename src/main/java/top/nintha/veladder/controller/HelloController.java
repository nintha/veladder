package top.nintha.veladder.controller;

import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import top.nintha.veladder.annotations.RequestBody;
import top.nintha.veladder.annotations.RequestMapping;
import top.nintha.veladder.annotations.RestController;
import top.nintha.veladder.entity.BeanReq;

import java.util.*;

@Slf4j
@RestController
public class HelloController {

    @RequestMapping("hello/world")
    public String helloWorld() {
        return "Hello world";
    }

    @RequestMapping("echo")
    public Map<String, Object> echo(String message, Long token, int code, RoutingContext ctx) {
        log.info("uri={}", ctx.request().absoluteURI());

        log.info("message={}, token={}, code={}", message, token, code);
        HashMap<String, Object> map = new HashMap<>();
        map.put("message", message);
        map.put("token", token);
        map.put("code", code);
        return map;
    }

    @RequestMapping(value = "hello/array")
    public List<Map.Entry<String, String>> helloArray(long[] ids, String[] names, RoutingContext ctx) {
        log.info("ids={}", Arrays.toString(ids));
        log.info("names={}", Arrays.toString(names));
        return ctx.request().params().entries();
    }

    @RequestMapping("query/list")
    public List<Map.Entry<String, String>> queryArray(List<Long> ids, TreeSet<String> names, LinkedList rawList, RoutingContext ctx) {
        log.info("ids={}", ids);
        log.info("names={}", names);
        log.info("rawList={}", rawList);
        return ctx.request().params().entries();
    }

    @RequestMapping("query/bean")
    public BeanReq queryBean(BeanReq req) {
        log.info("req={}", req);
        return req;
    }

    @RequestMapping(value = "post/body", method = HttpMethod.POST)
    public BeanReq postRequestBody(@RequestBody BeanReq req) {
        log.info("req={}", req);
        return req;
    }
}
