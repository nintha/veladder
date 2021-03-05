package top.nintha.veladder.controller;

import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import top.nintha.veladder.annotations.RequestBody;
import top.nintha.veladder.annotations.RequestMapping;
import top.nintha.veladder.annotations.RestController;
import top.nintha.veladder.entity.MockUser;

import java.util.*;

@Slf4j
@RestController
public class HelloController {

    @RequestMapping("hello/world")
    public String helloWorld() {
        return "Hello world";
    }

    @RequestMapping("hello/void")
    public void helloVoid() {
        log.info("call void");
    }

    @RequestMapping("echo/text")
    public String echoText(String text) {
        return text;
    }

    @RequestMapping("echo/object")
    public Map<String, Object> echoObject(String text, Long number, int code, RoutingContext ctx) {
        log.info("uri={}", ctx.request().absoluteURI());

        log.info("text={}, number={}, code={}", text, number, code);
        HashMap<String, Object> map = new HashMap<>();
        map.put("text", text);
        map.put("number", number);
        map.put("code", code);
        return map;
    }

    @RequestMapping(value = "hello/array")
    public HashMap<String, Object> helloArray(long[] ids, String[] names, RoutingContext ctx) {
        log.info("ids={}", Arrays.toString(ids));
        log.info("names={}", Arrays.toString(names));

        HashMap<String, Object> map = new HashMap<>();
        map.put("ids", ids);
        map.put("names", names);
        return map;
    }

    @RequestMapping("hello/list")
    public HashMap<String, Object> helloList(List<Long> ids, TreeSet<String> names, LinkedList rawList, RoutingContext ctx) {
        log.info("ids={}", ids);
        log.info("names={}", names);
        log.info("rawList={}", rawList);

        HashMap<String, Object> map = new HashMap<>();
        map.put("ids", ids);
        map.put("names", names);
        map.put("rawList", rawList);
        return map;
    }

    @RequestMapping("query/bean")
    public MockUser queryBean(MockUser req) {
        log.info("req={}", req);
        return req;
    }

    @RequestMapping(value = "post/body", method = "POST")
    public MockUser postRequestBody(@RequestBody MockUser req) {
        log.info("req={}", req);
        return req;
    }

    @RequestMapping("hello/path/variable/:token/:id")
    public String helloPathVariable(String token, Long id, RoutingContext ctx) {
        log.info("token={}", token);
        log.info("id={}", id);
        return token + id;
    }
}
