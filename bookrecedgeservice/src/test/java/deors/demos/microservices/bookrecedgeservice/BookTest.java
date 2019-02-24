package deors.demos.microservices.bookrecedgeservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class BookTest {

    @Test
    public void testDefaultConstructor() {

        Book b = new Book();

        assertNull(b.getId(),
            "on a bean just created with the empty constructor, id should be null");
        assertNull(b.getTitle(),
            "on a bean just created with the empty constructor, title should be null");
        assertNull(b.getAuthor(),
            "on a bean just created with the empty constructor, author should be null");
    }

    @Test
    public void testConstructor() {

        Book b = new Book(Long.valueOf(1000L), "the title", "the author");

        assertEquals(Long.valueOf(1000L), b.getId(),
            "on a bean just created, id should be the same as the value passed to the constructor");
        assertEquals("the title", b.getTitle(),
            "on a bean just created, title should be the same as the value passed to the constructor");
        assertEquals("the author", b.getAuthor(),
            "on a bean just created, author should be the same as the value passed to the constructor");
    }

    @Test
    public void testSetters() {

        Book b = new Book();
        b.setId(Long.valueOf(1000L));
        b.setTitle("the title");
        b.setAuthor("the author");

        assertEquals(Long.valueOf(1000L), b.getId(),
            "on a bean, id should be the same as the value passed to the setter");
        assertEquals("the title", b.getTitle(),
            "on a bean, title should be the same as the value passed to the setter");
        assertEquals("the author", b.getAuthor(),
            "on a bean, author should be the same as the value passed to the setter");
    }

    @Test
    public void testToString() {

        Book b = new Book();
        b.setId(Long.valueOf(1000L));
        b.setTitle("the title");
        b.setAuthor("the author");

        assertEquals("Book [id=1000, title=the title, author=the author]", b.toString(),
            "on a bean, the value returned by toString() should match the expected format");
    }
}
