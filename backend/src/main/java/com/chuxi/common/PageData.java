package com.chuxi.common;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PageData<T> {
    private List<T> records;
    private long total;
    private int pageNo;
    private int pageSize;
}
