package co.com.powerup.ags.loan.request.model.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> {
    
    private List<T> content;
    private PaginationInfo pagination;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaginationInfo {
        private int currentPage;
        private int pageSize;
        private long totalElements;
        private int totalPages;
        private boolean hasNext;
        private boolean hasPrevious;
        private boolean first;
        private boolean last;
        private int numberOfElements;
    }
    
    public static <T> PagedResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        
        PaginationInfo pagination = PaginationInfo.builder()
                .currentPage(page)
                .pageSize(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .numberOfElements(content.size())
                .hasNext(page < totalPages - 1)
                .hasPrevious(page > 0)
                .first(page == 0)
                .last(page == totalPages - 1 || totalPages == 0)
                .build();
        
        return PagedResponse.<T>builder()
                .content(content)
                .pagination(pagination)
                .build();
    }
}