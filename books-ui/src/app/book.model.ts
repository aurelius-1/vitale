export interface Book {
  id?: number;
  isbn: string;
  title: string;
  author: string;
  price: number;
  publisher?: string;
}