package deors.demos.microservices.bookrecedgeservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class BookController {

    @Autowired
    RestTemplate restTemplate;

    @Value("${defaultBookId}") private long defaultBookId;
    @Value("${defaultBookTitle}") private String defaultBookTitle;
    @Value("${defaultBookAuthor}") private String defaultBookAuthor;

    @RequestMapping("/bookrecedge")
    @com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand(fallbackMethod = "getDefaultBook")
    public Book getBookRecommendation() {
        return restTemplate.getForObject("http://bookrecservice/bookrec", Book.class);
    }

    public Book getDefaultBook() {
        return new Book(defaultBookId, defaultBookTitle, defaultBookAuthor);
    }
}
