package top.nintha.veladder;

import com.google.common.primitives.Primitives;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import javassist.*;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;
import lombok.extern.slf4j.Slf4j;
import top.nintha.veladder.annotations.RequestMapping;
import top.nintha.veladder.annotations.RestController;
import top.nintha.veladder.controller.HelloController;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

@Slf4j
public class AppLauncher extends AbstractVerticle {
    private final static int port = 8080;

    @Override
    public void start() throws Exception {
        HttpServer server = vertx.createHttpServer();

        Router router = Router.router(vertx);
        buildRouteFromAnnotatedClass(new HelloController(), router);

        server.requestHandler(router).listen(port, ar -> {
            if (ar.succeeded()) {
                log.info("HTTP Server is listening on {}", port);
            } else {
                log.error("Failed to run HTTP Server", ar.cause());
            }
        });
    }


    private <T> void buildRouteFromAnnotatedClass(T AnnotatedBean, Router router) throws NotFoundException {
        Class<T> clazz = (Class<T>) AnnotatedBean.getClass();
        boolean clzHasAnno = clazz.isAnnotationPresent(RestController.class);
        if (clzHasAnno) {
            RestController annotation = clazz.getAnnotation(RestController.class);
            String name = annotation.name();
            log.info("RestController, annotationValue: name={}", name);
        }

        ClassPool classPool = ClassPool.getDefault();
        classPool.insertClassPath(new ClassClassPath(clazz));
        CtClass cc = classPool.get(clazz.getName());
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            boolean methodHasAnno = method.isAnnotationPresent(RequestMapping.class);
            if (!methodHasAnno) continue;
            RequestMapping methodAnno = method.getAnnotation(RequestMapping.class);
            String annotationValue = methodAnno.value();
            log.info("method {}, annotationValue: {}", method.getName(), annotationValue);

            CtMethod ctMethod = cc.getDeclaredMethod(method.getName());
            MethodInfo methodInfo = ctMethod.getMethodInfo();
            CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
            LocalVariableAttribute attribute = (LocalVariableAttribute) codeAttribute.getAttribute(LocalVariableAttribute.tag);
            if (attribute == null) {
                continue;
            }

            Class<?>[] paramTypes = method.getParameterTypes();
            String[] paramNames = new String[ctMethod.getParameterTypes().length];
            // 成员方法 0位变量是this
            int pos = Modifier.isStatic(ctMethod.getModifiers()) ? 0 : 1;
            for (int i = 0; i < paramNames.length; i++) {
                paramNames[i] = attribute.variableName(i + pos);
                log.info("param #{} > {}", i, paramNames[i]);
            }

            String path = annotationValue.startsWith("/") ? annotationValue : "/" + annotationValue;
            router.route(methodAnno.method(), path).handler(ctx -> {
                try {
                    HttpServerResponse response = ctx.response();
                    response.putHeader(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8");
                    Object[] argValues = new Object[ctMethod.getParameterTypes().length];
                    for (int i = 0; i < argValues.length; i++) {
                        Class<?> paramType = paramTypes[i];
                        // TODO 数组类型 或 集合类型解析
                        // TODO RequestBody数据解析

                        // special type
                        if (paramType == RoutingContext.class) {
                            argValues[i] = ctx;
                        }
                        // simple type: String and primitive type
                        else {
                            String paramStringValue = ctx.request().getParam(paramNames[i]);
                            Class<?> wrapType = Primitives.wrap(paramType);
                            if (Primitives.allWrapperTypes().contains(wrapType)) {
                                MethodHandle valueOf = MethodHandles.lookup().unreflect(wrapType.getMethod("valueOf", String.class));
                                argValues[i] = valueOf.invoke(paramStringValue);
                            } else if (paramType == String.class) {
                                argValues[i] = paramStringValue;
                            }

                        }
                    }
                    Object result = MethodHandles.lookup().unreflect(method).bindTo(AnnotatedBean).invoke(argValues);
                    // Write to the response and end it
                    response.end(result instanceof CharSequence ? result.toString() : Json.encode(result));
                } catch (Throwable e) {
                    log.error("request error, {}::{} ", clazz.getName(), method.getName(), e);
                }
            });

        }

    }

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new AppLauncher());
        log.info("Deploy Verticle....");
    }
}
