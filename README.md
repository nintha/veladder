# veladder
A ladder for helping springer use vertx web

spring风格的vertx web框架



## Get start

veladder可以使用类似spring mvc的注解来构建web服务，大部分功能是通过注解扫描进行实现的。

``` java
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
```





