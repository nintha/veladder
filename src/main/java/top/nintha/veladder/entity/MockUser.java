package top.nintha.veladder.entity;

import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public class MockUser {
    private Long id;
    private String name;
    private List<String> tags;

    public static MockUser defaultUser(){
        MockUser user = new MockUser();
        user.id = 1L;
        user.name = "default";
        user.tags = Collections.singletonList("TAG");
        return user;
    }
}
