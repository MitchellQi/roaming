import java.util.logging.Logger;
import org.junit.Before;

// Test class for Barricade
public class BarricadeTest {

	private final Logger logger = Logger.getLogger(Barricade.class.getName());
	private final LoggerTestingHandler handler = new LoggerTestingHandler();

	@Before
	public void setup() {
		logger.addHandler(handler);
	}

	// Testing methods starting here

}