import org.junit.Test;

public class ComponentTest {
	
	@Test
	public void doSomething() {
		if (System.getProperty("projectB.fail") != null) throw new RuntimeException();
	}

}
