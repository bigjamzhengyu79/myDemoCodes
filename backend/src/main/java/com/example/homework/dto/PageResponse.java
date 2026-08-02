package com.example.homework.dto;

import org.springframework.data.domain.Page;
import java.util.List;

/**
 * 分页结果的显式包装。
 * 不直接序列化 Spring Data 的 Page/PageImpl —— 其 JSON 结构在 Spring Data 各版本间不稳定，
 * 且会附带前端用不到的 pageable/sort 字段。这里只暴露前端需要的字段。
 */
public class PageResponse<T> {
    private List<T> content;
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;

    public static <T> PageResponse<T> of(List<T> content, Page<?> page) {
        PageResponse<T> r = new PageResponse<>();
        r.content = content;
        r.totalElements = page.getTotalElements();
        r.totalPages = page.getTotalPages();
        r.page = page.getNumber();
        r.size = page.getSize();
        return r;
    }

    public List<T> getContent() { return content; }
    public long getTotalElements() { return totalElements; }
    public int getTotalPages() { return totalPages; }
    public int getPage() { return page; }
    public int getSize() { return size; }
}
