import org.junit.Test;
import static org.junit.Assert.*;

public class StringUtilTest {

    @Test
    public void testReverse() {
        assertEquals("dlroW olleH", StringUtil.reverse("Hello World"));
    }

}
