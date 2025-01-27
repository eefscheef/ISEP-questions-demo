import unittest
from palindrome import is_palindrome

class TestPalindromeSecret(unittest.TestCase):

    def test_empty_string(self):
        self.assertTrue(is_palindrome(""))

    def test_mixed_case_palindrome(self):
        self.assertTrue(is_palindrome("RaceCar"))

    def test_palindrome_with_spaces(self):
        self.assertTrue(is_palindrome("A man a plan a canal Panama".replace(" ", "").lower()))

if __name__ == '__main__':
    unittest.main()
