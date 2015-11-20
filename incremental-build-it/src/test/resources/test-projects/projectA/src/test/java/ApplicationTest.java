import org.junit.Test;

public class ApplicationTest {
	
	@Test
	public void doSomething() {
		new Application().doSomething();
		new TestUtil().doNothing();
		if (System.getProperty("projectA.fail") != null) throw new RuntimeException();
	}

}
