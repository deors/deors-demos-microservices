package deors.demos.microservices.bookrecedgeservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class BookController {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${defaultBookId}")
    private long defaultBookId;

    @Value("${defaultBookTitle}")
    private String defaultBookTitle;

    @Value("${defaultBookAuthor}")
    private String defaultBookAuthor;

    @Autowired
    private CircuitBreakerFactory circuitBreakerFactory;

    @RequestMapping("/bookrecedge")
    public Book getBookRecommendation() {
        CircuitBreaker circuitBreaker = circuitBreakerFactory.create("bookrec");
        return circuitBreaker.run(
            () -> restTemplate.getForObject("http://bookrecservice/bookrec", Book.class),
            throwable -> getDefaultBook());
    }

    public Book getDefaultBook() {
        return new Book(defaultBookId, defaultBookTitle, defaultBookAuthor);
    }
}
