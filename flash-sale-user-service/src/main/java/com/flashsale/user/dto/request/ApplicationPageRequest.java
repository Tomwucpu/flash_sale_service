package com.flashsale.user.dto.request;

public record ApplicationPageRequest(
        String status,
        Integer page,
        Integer size
) {

    public int getPage() {
        return page != null && page > 0 ? page : 1;
    }

    public int getSize() {
        return size != null && size > 0 ? size : 10;
    }
}
