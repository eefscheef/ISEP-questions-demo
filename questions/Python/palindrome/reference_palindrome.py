def is_palindrome(s: str) -> bool:
    """
    Check if the given string is a palindrome. A palindrome reads the same forward and backward.
    """
    cleaned = ''.join(c.lower() for c in s if c.isalnum())  # Ignore case and non-alphanumeric characters
    return cleaned == cleaned[::-1]  # Compare string with its reverse
