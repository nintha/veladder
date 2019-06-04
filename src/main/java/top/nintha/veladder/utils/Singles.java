package top.nintha.veladder.utils;

import io.reactivex.Single;
import io.vertx.core.Future;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class Singles {
    /**
     * async converter, call by CompletableFuture::whenComplete
     */
    public static <T> Single<T> fromCompletableFuture(CompletableFuture<T> future) {
        return Single.create(emitter -> future.whenComplete((value, error) -> {
            if (error == null) {
                emitter.onSuccess(value);
            } else {
                emitter.onError(error);
            }
        }));
    }

    public static <T> Single<T> fromVertxFuture(Future<T> future){
        return Single.create(emitter -> future.setHandler(ar -> {
            if(ar.succeeded()){
                emitter.onSuccess(ar.result());
            }else {
                emitter.onError(ar.cause());
            }
        }));
    }

    public static <T> Single<T> supplyAsync(Supplier<T> supplier){
        return fromCompletableFuture(CompletableFuture.supplyAsync(supplier));
    }

}
