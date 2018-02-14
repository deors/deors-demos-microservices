package deors.demos.microservices.bookrecservice;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

/**
 * Created by jorge.hidalgo on 09/10/2016.
 */
@RepositoryRestResource
public interface BookRepository extends CrudRepository<Book, Long> {
    @Query("select b from Book b order by RAND()")
    List<Book> getBooksRandomOrder();
}
