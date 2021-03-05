package top.nintha.veladder;

import com.google.common.primitives.Primitives;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import io.vertx.core.*;
import io.vertx.core.dns.AddressResolverOptions;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import javassist.Modifier;
import javassist.*;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import top.nintha.veladder.annotations.RequestBody;
import top.nintha.veladder.annotations.RequestMapping;
import top.nintha.veladder.annotations.RestController;
import top.nintha.veladder.utils.ClassScanUtil;

import javax.sound.sampled.Port;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class AppLauncher extends AbstractVerticle {
    private final static String SCAN_PACKAGE = "top.nintha.veladder.controller";
    private final int port;

    public AppLauncher(int port) {
        this.port = port;
    }

    @Override
    public void start() throws Exception {
        HttpServer server = vertx.createHttpServer();

        Router router = Router.router(vertx);
        Set<Class<?>> classes = ClassScanUtil.scanByAnnotation(SCAN_PACKAGE, RestController.class);
        for (Class<?> cls : classes) {
            Object controller = cls.getConstructor().newInstance();
            routerMapping(controller, router);
        }

        server.requestHandler(router).listen(port, ar -> {
            if (ar.succeeded()) {
                log.info("HTTP Server is listening on {}", port);
            } else {
                log.error("Failed to run HTTP Server", ar.cause());
            }
        });
    }


    /**
     * buildRouteFromAnnotatedClass
     *
     * @param annotatedBean
     * @param router
     * @param <ControllerType>
     * @throws NotFoundException
     */
    private <ControllerType> void routerMapping(ControllerType annotatedBean, Router router) throws NotFoundException {
        Class<ControllerType> clazz = (Class<ControllerType>) annotatedBean.getClass();
        if (!clazz.isAnnotationPresent(RestController.class)) {
            return;
        }

        ClassPool classPool = ClassPool.getDefault();
        classPool.insertClassPath(new ClassClassPath(clazz));
        CtClass cc = classPool.get(clazz.getName());
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (!method.isAnnotationPresent(RequestMapping.class)) {
                continue;
            }

            RequestMapping methodAnno = method.getAnnotation(RequestMapping.class);
            String requestPath = methodAnno.value();

            CtMethod ctMethod = cc.getDeclaredMethod(method.getName());
            MethodInfo methodInfo = ctMethod.getMethodInfo();
            CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
            LocalVariableAttribute attribute = (LocalVariableAttribute) codeAttribute.getAttribute(LocalVariableAttribute.tag);

            Class<?>[] paramTypes = method.getParameterTypes();
            String[] paramNames = new String[ctMethod.getParameterTypes().length];
            if (attribute != null) {
                // 通过javassist获取方法形参，成员方法 0位变量是this
                int pos = Modifier.isStatic(ctMethod.getModifiers()) ? 0 : 1;
                for (int i = 0; i < paramNames.length; i++) {
                    paramNames[i] = attribute.variableName(i + pos);
                }
            }
            String formatPath = requestPath.startsWith("/") ? requestPath : "/" + requestPath;
            log.info("[Router Mapping] {}({}) > {}, {}", method.getName(), formatPath, Arrays.toString(paramNames), Arrays.toString(paramTypes));


            Handler<RoutingContext> requestHandler = ctx -> {
                try {
                    Object[] argValues = new Object[ctMethod.getParameterTypes().length];
                    MultiMap params = ctx.request().params();
                    Annotation[][] parameterAnnotations = method.getParameterAnnotations();
                    Set<FileUpload> uploads = ctx.fileUploads();
                    Map<String, FileUpload> uploadMap = uploads.stream().collect(Collectors.toMap(FileUpload::name, x -> x));
                    for (int i = 0; i < argValues.length; i++) {
                        Class<?> paramType = paramTypes[i];
                        // RequestBody数据解析
                        List<? extends Class<? extends Annotation>> parameterAnnotation = Arrays.stream(parameterAnnotations[i]).map(Annotation::annotationType).collect(Collectors.toList());
                        if (parameterAnnotation.contains(RequestBody.class)) {
                            String bodyAsString = ctx.getBodyAsString();
                            argValues[i] = Json.decodeValue(bodyAsString, paramType);
                        }
                        // special type
                        else if (paramType == RoutingContext.class) {
                            argValues[i] = ctx;
                        } else if (paramType == FileUpload.class) {
                            argValues[i] = uploadMap.get(paramNames[i]);
                        }
                        // Normal Type
                        else if (paramType.isArray() || Collection.class.isAssignableFrom(paramType) || isStringOrPrimitiveType(paramType)) {
                            Type[] genericParameterTypes = method.getGenericParameterTypes();
                            argValues[i] = parseSimpleTypeOrArrayOrCollection(params, paramType, paramNames[i], genericParameterTypes[i]);
                        }
                        // POJO Bean
                        else {
                            argValues[i] = parseBeanType(params, paramType);
                        }
                    }
                    HttpServerResponse response = ctx.response();
                    Object result = MethodHandles.lookup().unreflect(method).bindTo(annotatedBean).invokeWithArguments(argValues);
                    if (!response.headWritten()) {
                        response.putHeader(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8");
                    }

                    // Write to the response and end it
                    Consumer<Object> responseEnd = x -> {
                        if (method.getReturnType() == void.class) {
                            response.end();
                        } else {
                            response.end(x instanceof CharSequence ? x.toString() : Json.encode(x));
                        }
                    };
                    Consumer<Throwable> onError = err -> {
                        log.error("request error, {}::{}", clazz.getName(), method.getName(), err);
                        HashMap<String, Object> map = new HashMap<>();
                        map.put("message", "system error");
                        ctx.response().end(Json.encode(map));
                    };
                    if (result instanceof Single) {
                        ((Single) result).subscribe(responseEnd, onError);
                    } else if (result instanceof Flowable) {
                        throw new UnsupportedOperationException("not support Flowable, maybe use Single instead");
                    } else {
                        responseEnd.accept(result);
                    }

                } catch (Throwable e) {
                    log.error("request error, {}::{} ", clazz.getName(), method.getName(), e);
                    HashMap<String, Object> result = new HashMap<>();
                    result.put("message", "system error");
                    ctx.response().end(Json.encode(result));
                }
            };

            // bind handler to router
            if (methodAnno.method().length == 0) {
                // 默认绑定全部HttpMethod
                router.route(formatPath).handler(BodyHandler.create()).handler(requestHandler);
            } else {
                for (String m : methodAnno.method()) {
                    router.route(HttpMethod.valueOf(m), formatPath).handler(BodyHandler.create()).handler(requestHandler);
                }
            }
        }
    }

    /**
     * 解析简单类型以及对应的集合或数组类型
     *
     * @param allParams             所有请求参数
     * @param paramType             参数类型
     * @param paramName             参数名称
     * @param genericParameterTypes 泛型化参数类型
     * @return
     * @throws Throwable
     */
    private Object parseSimpleTypeOrArrayOrCollection(MultiMap allParams, Class<?> paramType, String paramName, Type genericParameterTypes) throws Throwable {
        // Array type
        if (paramType.isArray()) {
            // 数组元素类型
            Class<?> componentType = paramType.getComponentType();

            List<String> values = allParams.getAll(paramName);
            Object array = Array.newInstance(componentType, values.size());
            for (int j = 0; j < values.size(); j++) {
                Array.set(array, j, parseSimpleType(values.get(j), componentType));
            }
            return array;
        }
        // Collection type
        else if (Collection.class.isAssignableFrom(paramType)) {
            return parseCollectionType(allParams.getAll(paramName), genericParameterTypes);
        }
        // String and primitive type
        else if (isStringOrPrimitiveType(paramType)) {
            return parseSimpleType(allParams.get(paramName), paramType);
        }

        return null;
    }

    /**
     * 判断是否为字符串或基础类型以及对应的包装类型
     */
    private boolean isStringOrPrimitiveType(Class<?> targetClass) {
        return targetClass == String.class || Primitives.allWrapperTypes().contains(Primitives.wrap(targetClass));
    }

    /**
     * 处理字符串，基础类型以及对应的包装类型
     */
    @SuppressWarnings("unchecked")
    private <T> T parseSimpleType(String value, Class<T> targetClass) throws Throwable {
        if (StringUtils.isBlank(value)) {
            return null;
        }

        Class<?> wrapType = Primitives.wrap(targetClass);
        if (Primitives.allWrapperTypes().contains(wrapType)) {
            MethodHandle valueOf = MethodHandles.lookup().unreflect(wrapType.getMethod("valueOf", String.class));
            return (T) valueOf.invoke(value);
        } else if (targetClass == String.class) {
            return (T) value;
        }

        return null;
    }

    /**
     * 解析集合类型
     *
     * @param values               请求参数值
     * @param genericParameterType from Method::getGenericParameterTypes
     */
    private Collection parseCollectionType(List<String> values, Type genericParameterType) throws Throwable {
        Class<?> actualTypeArgument = String.class; // 无泛型参数默认用String类型
        Class<?> rawType;
        // 参数带泛型
        if (genericParameterType instanceof ParameterizedType) {
            ParameterizedType parameterType = (ParameterizedType) genericParameterType;
            actualTypeArgument = (Class<?>) parameterType.getActualTypeArguments()[0];
            rawType = (Class<?>) parameterType.getRawType();
        } else {
            rawType = (Class<?>) genericParameterType;
        }

        Collection coll;
        if (rawType == List.class) {
            coll = new ArrayList<>();
        } else if (rawType == Set.class) {
            coll = new HashSet<>();
        } else {
            coll = (Collection) rawType.newInstance();
        }

        for (String value : values) {
            coll.add(parseSimpleType(value, actualTypeArgument));
        }
        return coll;
    }

    /**
     * 解析实体对象
     *
     * @param allParams 所有参数
     * @param paramType 实体参数类型
     * @return 已经注入字段的实体对象
     */
    private Object parseBeanType(MultiMap allParams, Class<?> paramType) throws Throwable {
        Object bean = paramType.newInstance();
        Field[] fields = paramType.getDeclaredFields();
        for (Field field : fields) {
            Object value = parseSimpleTypeOrArrayOrCollection(allParams, field.getType(), field.getName(), field.getGenericType());

            field.setAccessible(true);
            field.set(bean, value);
        }
        return bean;
    }

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new AppLauncher(8080));
        log.info("Deploy Verticle....");
    }
}
