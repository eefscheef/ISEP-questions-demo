import org.junit.Test;
import static org.junit.Assert.*;

public class StringUtilSecretTest {

    @Test
    public void testReverseEmptyString() {
        assertEquals("", StringUtil.reverse(""));
    }

    @Test
    public void testReverseSingleCharacter() {
        assertEquals("a", StringUtil.reverse("a"));
    }

}
