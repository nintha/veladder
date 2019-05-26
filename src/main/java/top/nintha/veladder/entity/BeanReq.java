package top.nintha.veladder.entity;

import lombok.Data;

import java.util.List;

@Data
public class BeanReq {
    private Long id;
    private String name;
    private List<String> list;
}
