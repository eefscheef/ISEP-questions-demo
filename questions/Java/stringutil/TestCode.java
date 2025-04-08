import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestCode {

    @Test
    public void testReverse() {
        assertEquals("dlroW olleH", Code.reverse("Hello World"));
    }

}
