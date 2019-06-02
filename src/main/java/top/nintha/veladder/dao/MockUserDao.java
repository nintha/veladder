package top.nintha.veladder.dao;

import top.nintha.veladder.annotations.BlockingService;
import top.nintha.veladder.entity.MockUser;

@BlockingService
public class MockUserDao {

    public MockUser findDefaultUser(){
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return MockUser.defaultUser();
    }

    public String blockingActionWithException(){
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        throw new RuntimeException("test throw error");
    }
}
