package vn.edu.bookstore.catalog.api;

import java.net.URI;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import vn.edu.bookstore.catalog.api.generated.BooksApi;
import vn.edu.bookstore.catalog.api.model.Book;
import vn.edu.bookstore.catalog.api.model.BookCreateRequest;
import vn.edu.bookstore.catalog.api.model.BookPage;
import vn.edu.bookstore.catalog.api.model.BookUpdateRequest;
import vn.edu.bookstore.catalog.domain.BookEntity;
import vn.edu.bookstore.catalog.service.BookService;
import vn.edu.bookstore.catalog.service.PreconditionFailedException;
import vn.edu.bookstore.catalog.service.PreconditionRequiredException;

@RestController
public class BookController implements BooksApi {

    private final BookService service;

    public BookController(BookService service) {
        this.service = service;
    }

    public static Sort toSort(String sort) {
        String sortParam = (sort != null && !sort.isBlank()) ? sort : "title,asc";
        String[] parts = sortParam.split(",");
        String property = parts[0].trim();

        if ("price".equals(property)) {
            property = "priceAmount";
        }

        Sort.Direction direction = (parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim()))
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        return Sort.by(direction, property);
    }

    @Override
    public ResponseEntity<BookPage> listBooks(String q, Integer page, Integer size, String sort) {
        int pageNum = (page != null) ? page : 0;
        int pageSize = (size != null) ? size : 20;

        Pageable pageable = PageRequest.of(pageNum, pageSize, toSort(sort));
        var pageResult = service.list(q, pageable);

        return ResponseEntity.ok(BookMapper.toPage(pageResult));
    }

    @Override
    public ResponseEntity<Book> getBook(String isbn, String ifNoneMatch) {
        BookEntity e = service.get(isbn);
        String etag = BookMapper.etag(e);

        if (ifNoneMatch != null && matches(ifNoneMatch, etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).build();
        }

        return ResponseEntity.ok().eTag(etag).body(BookMapper.toApi(e));
    }

    @Override
    public ResponseEntity<Book> createBook(BookCreateRequest request) {
        BookEntity e = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{isbn}").buildAndExpand(e.getIsbn()).toUri();
        return ResponseEntity.created(location).eTag(BookMapper.etag(e)).body(BookMapper.toApi(e));
    }

    @Override
    public ResponseEntity<Book> updateBook(String isbn, BookUpdateRequest request, String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank()) {
            throw new PreconditionRequiredException("Cần header If-Match chứa ETag đã đọc trước khi cập nhật");
        }

        BookEntity e = service.update(isbn, request, parseVersion(ifMatch));
        return ResponseEntity.ok().eTag(BookMapper.etag(e)).body(BookMapper.toApi(e));
    }

    @Override
    public ResponseEntity<Void> deleteBook(String isbn) {
        service.delete(isbn);
        return ResponseEntity.noContent().build();
    }

    private static boolean matches(String header, String etag) {
        for (String candidate : header.split(",")) {
            String c = candidate.trim();
            if (c.equals("*") || c.replaceFirst("^W/", "").equals(etag)) {
                return true;
            }
        }
        return false;
    }

    private static long parseVersion(String ifMatch) {
        String v = ifMatch.trim().replaceFirst("^W/", "").replace("\"", "");
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException ex) {
            throw new PreconditionFailedException("If-Match không hợp lệ: " + ifMatch);
        }
    }
}
