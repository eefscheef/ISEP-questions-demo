import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SecretTest {

    @Test
    public void testReverseEmptyString() {
        assertEquals("", Code.reverse(""));
    }

    @Test
    public void testReverseSingleCharacter() {
        assertEquals("a", Code.reverse("a"));
    }

}
