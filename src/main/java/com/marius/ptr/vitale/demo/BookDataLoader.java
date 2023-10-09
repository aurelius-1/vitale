package com.marius.ptr.vitale.demo;

import com.marius.ptr.vitale.domain.Book;
import com.marius.ptr.vitale.domain.BookRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("testdata")
public class BookDataLoader {

    final BookRepository bookRepository;

    public BookDataLoader(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void loadData() {
        bookRepository.deleteAll();

        var book1 = Book.of("1234567890", "Title", "Author", 10.0);
        var book2 = Book.of("", "Title2", "Author2", 11.0);

        bookRepository.saveAll(List.of(book1, book2));
    }
}
