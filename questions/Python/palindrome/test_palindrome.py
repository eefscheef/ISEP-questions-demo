import unittest
from palindrome import is_palindrome

class TestPalindrome(unittest.TestCase):

    def test_simple_palindrome(self):
        self.assertTrue(is_palindrome("racecar"))

    def test_not_palindrome(self):
        self.assertFalse(is_palindrome("hello"))

if __name__ == '__main__':
    unittest.main()
