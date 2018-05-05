package deors.demos.microservices.bookrecservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BookController {

    @Autowired
    private BookRepository bookRepository;

    @RequestMapping("/bookrec")
    public Book getBookRecommendation() {
        return bookRepository.getBooksRandomOrder().get(0);
    }
}
