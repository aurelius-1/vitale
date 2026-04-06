import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { provideHttpClient } from '@angular/common/http';
import { BookService } from './book.service';
import { Book } from './book.model';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  books: Book[] = [];
  error = '';
  showForm = false;
  editingIsbn: string | null = null;

  form: Book = { isbn: '', title: '', author: '', price: 0, publisher: '' };

  constructor(private bookService: BookService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.bookService.getAll().subscribe({
      next: (data) => (this.books = data),
      error: () => (this.error = 'Could not load books.')
    });
  }

  openAdd(): void {
    this.editingIsbn = null;
    this.form = { isbn: '', title: '', author: '', price: 0, publisher: '' };
    this.showForm = true;
  }

  openEdit(book: Book): void {
    this.editingIsbn = book.isbn;
    this.form = { ...book };
    this.showForm = true;
  }

  save(): void {
    if (this.editingIsbn) {
      this.bookService.update(this.editingIsbn, this.form).subscribe({
        next: () => { this.showForm = false; this.load(); },
        error: () => (this.error = 'Could not update book.')
      });
    } else {
      this.bookService.create(this.form).subscribe({
        next: () => { this.showForm = false; this.load(); },
        error: () => (this.error = 'Could not create book.')
      });
    }
  }

  delete(isbn: string): void {
    this.bookService.delete(isbn).subscribe({
      next: () => this.load(),
      error: () => (this.error = 'Could not delete book.')
    });
  }

  cancel(): void {
    this.showForm = false;
    this.error = '';
  }
}