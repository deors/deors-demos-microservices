package deors.demos.microservices.bookrecservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@DataJpaTest
public class BookRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BookRepository bookRepository;

    @Test
    public void testGetBooksRandomOrder() {

        List<Book> foundBooks = bookRepository.getBooksRandomOrder();

        assertEquals(10, foundBooks.size(),
            "the number of found books should be ten");

        List<String> bookTitles = foundBooks.stream().map(b -> b.getTitle()).collect(Collectors.toList());
        List<String> bookAuthors = foundBooks.stream().map(b -> b.getAuthor()).collect(Collectors.toList());

        assertTrue(bookTitles.contains("the player of games"),
            "the list of found books should contain 'the player of games'");
        assertTrue(bookTitles.contains("lady of the lake"),
            "the list of found books should contain 'lady of the lake'");
        assertFalse(bookTitles.contains("the tower of swallows"),
            "the list of found books should not contain 'the tower of swallows'");
        assertTrue(bookAuthors.contains("isaac asimov"),
            "the list of found books should contain 'isaac asimov'");
        assertTrue(bookAuthors.contains("j.r.r. tolkien"),
            "the list of found books should contain 'j.r.r. tolkien'");
        assertFalse(bookAuthors.contains("ursula k. leguin"),
            "the list of found books should not contain 'ursula k. leguin'");
    }

    @Test
    public void testGetBooksRandomOrderAddTwoRecords() {

        Book sample1Book = new Book();
        sample1Book.setTitle("the first book title");
        sample1Book.setAuthor("the first book author");

        Book sample2Book = new Book();
        sample2Book.setTitle("the second book title");
        sample2Book.setAuthor("the second book author");

        entityManager.persist(sample1Book);
        entityManager.persist(sample2Book);
        entityManager.flush();

        List<Book> foundBooks = bookRepository.getBooksRandomOrder();

        assertEquals(12, foundBooks.size(),
            "the number of found books should be twelve");

        List<String> bookTitles = foundBooks.stream().map(b -> b.getTitle()).collect(Collectors.toList());
        List<String> bookAuthors = foundBooks.stream().map(b -> b.getAuthor()).collect(Collectors.toList());

        assertTrue(bookTitles.contains("the first book title"),
            "the list of found books should contain the first book title");
        assertTrue(bookTitles.contains("the second book title"),
            "the list of found books should contain the second book title");
        assertTrue(bookAuthors.contains("the first book author"),
            "the list of found books should contain the first book author");
        assertTrue(bookAuthors.contains("the second book author"),
            "the list of found books should contain the second book author");
    }
}
