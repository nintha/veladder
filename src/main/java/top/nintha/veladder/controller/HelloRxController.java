package top.nintha.veladder.controller;

import io.reactivex.Single;
import io.vertx.core.http.HttpMethod;
import lombok.extern.slf4j.Slf4j;
import top.nintha.veladder.annotations.RequestMapping;
import top.nintha.veladder.annotations.RestController;
import top.nintha.veladder.dao.MockUserDao;
import top.nintha.veladder.entity.MockUser;
import top.nintha.veladder.utils.Singles;

import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
public class HelloRxController {
    private static final MockUserDao mockUserDao = new MockUserDao();

    @RequestMapping("rx/hello/world")
    public Single<String> helloWorld() {
        return Single.just("Hello world");
    }


    @RequestMapping(value = "rx/users/default", method = HttpMethod.GET)
    public Single<MockUser> findDefaultUser() {
        CompletableFuture<MockUser> userFuture = CompletableFuture.supplyAsync(mockUserDao::findDefaultUser);
        return Singles.fromCompletableFuture(userFuture);
    }

    @RequestMapping(value = "rx/users/exception", method = HttpMethod.GET)
    public Single<String> exceptionAction() {
        CompletableFuture<String> userFuture = CompletableFuture.supplyAsync(mockUserDao::blockingActionWithException);
        return Singles.fromCompletableFuture(userFuture);
    }
}
