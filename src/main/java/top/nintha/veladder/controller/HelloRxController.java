package top.nintha.veladder.controller;

import io.reactivex.Single;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
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
        return Singles.supplyAsync(mockUserDao::findDefaultUser);
    }

    @RequestMapping(value = "rx/users/exception", method = HttpMethod.GET)
    public Single<String> exceptionAction() {
        return Singles.supplyAsync(mockUserDao::blockingActionWithException);
    }

    @RequestMapping(value = "rx/file/upload", method = HttpMethod.POST)
    public Single<String> uploadFile(FileUpload file, RoutingContext ctx){
        if(file == null){
            log.error("upload failed");
            return Single.just("");
        }

        FileSystem fileSystem = ctx.vertx().fileSystem();
        fileSystem.readFile(file.uploadedFileName(), ar -> {
            if(ar.succeeded()){
                fileSystem.writeFile(file.fileName(), ar.result(), writeAr -> {
                    if(writeAr.succeeded()){
                        log.info("upload ok.");
                    }else {
                        log.error("error", ar.cause());
                    }
                });
            }else{
                log.error("error", ar.cause());
            }
        });
        log.info("{}", file.uploadedFileName());
        return Single.just(file.fileName());
    }

    @RequestMapping(value = "rx/file/download", method = HttpMethod.GET)
    public void downloadFile(RoutingContext ctx){
        ctx.response()
                .putHeader(HttpHeaders.CONTENT_TYPE, "application/octet-stream")
                .putHeader("Content-Disposition", "attachment; filename=\"pic.jpg\"")
                .putHeader(HttpHeaders.TRANSFER_ENCODING, "chunked")
                .sendFile("src/main/resources/assert/pic.jpg");
    }
}
