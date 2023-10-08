package com.marius.ptr.vitale.demo;

import com.marius.ptr.vitale.domain.Book;
import com.marius.ptr.vitale.domain.BookRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Profile("testdata")
public class BookDataLoader {

    final BookRepository bookRepository;

    public BookDataLoader(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void loadData() {
        var book1 = new Book("1234567890", "Title", "Author", 10.0);
        bookRepository.save(book1);

        var book2 = new Book("1234567460", "Title2", "Author2", 11.0);
        bookRepository.save(book2);

    }
}
