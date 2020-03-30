package org.adex;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
public class PlayingWithRecordsApplication {

	public static void main(String[] args) {
		SpringApplication.run(PlayingWithRecordsApplication.class, args);
	}

}

@Component
class Runner {

	private final DaoModels<Book> bookDao;

	public Runner(DaoModels<Book> bookDao) {
		super();
		this.bookDao = bookDao;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void run() {

		System.out.println("Rows added : " + this.bookDao.add("Effective Java Book", Reviews.EXCELLENT));
		System.out.println("Rows added : " + this.bookDao.add("React 101", Reviews.GOOD));

		System.out.println(">>> Select All : ");
		this.bookDao.findAll().forEach(System.out::println);

	}

}

interface DaoModels<T> {

	int add(String title, Reviews reviews);

	T findById(int id);

	List<T> findAll();
}

@Repository
class BookDao implements DaoModels<Book> {

	private static int idGenerator = 100;

	private final JdbcTemplate jdbcTemplate;

	@Autowired
	private BookRowMapper bookRowMapper;

	private final static String selectByIdQuery = """
			select * from books
			where id = ?
			""";

	private final static String selectAll = "select * from books";

	private final static String insertQuery = """
			insert into books
			values(?, ?, ?)
			""";

	public BookDao(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public Book findById(int id) {
		return this.jdbcTemplate.queryForObject(selectByIdQuery, new Object[] { id }, this.bookRowMapper);
	}

	@Override
	public int add(String title, Reviews reviews) {
		Book book = this.buildBook(title, reviews);
		return this.jdbcTemplate.update(insertQuery, new Object[] { book.id(), book.title(), book.points() });
	}

	@Override
	public List<Book> findAll() {
		return this.jdbcTemplate.query(selectAll, this.bookRowMapper);
	}

	private Book buildBook(String name, Reviews status) {
		var points = switch (status) {
		case BAD -> 0;
		case GOOD -> 1;
		case EXCELLENT -> 2;
		default -> -1;
		};
		return new Book(idGenerator++, name, points);
	}

}

record Book(int id, String title, int points) {
	public Book {
		if (points < 0 || points > 2)
			throw new IllegalArgumentException("Points must be in interval [0:2]");
		if (title.isEmpty() || title.length() == 0)
			throw new IllegalArgumentException("Title must not be empty");
	}
}

enum Reviews {
	BAD, GOOD, EXCELLENT, NOT_DEFIEND;
}

@Component
class BookRowMapper implements RowMapper<Book> {

	@Override
	public Book mapRow(ResultSet rs, int rowNum) throws SQLException {
		return new Book(rs.getInt("id"), rs.getString("title"), rs.getInt("points"));
	}

}