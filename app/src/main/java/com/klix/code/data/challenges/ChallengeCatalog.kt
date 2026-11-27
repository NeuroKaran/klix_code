package com.klix.code.data.challenges

object ChallengeCatalog {

    val dailyChallenges: List<CodingChallenge> = listOf(
        CodingChallenge(
            id = "two_sum_on",
            title = "Two-Sum",
            shortTitle = "Two-Sum",
            description = "Return indices of two numbers that add up to target.",
            difficulty = ChallengeDifficulty.EASY,
            estimatedMinutes = 2,
            language = ChallengeLanguage.PYTHON,
            fileName = "two_sum.py",
            previewSnippet = """
                nums = [2, 7, 11, 15],
                target = 9
                # -> [0, 1]
            """.trimIndent(),
            entrySymbol = "two_sum",
            starterCode = """
                def two_sum(nums, target):
                    # Return indices [i, j] of two numbers adding up to target.
                    # TODO: find two indices whose values sum to target
                    return []


                if __name__ == "__main__":
                    nums = [2, 7, 11, 15]
                    target = 9
                    print(two_sum(nums, target))
            """.trimIndent(),
            testCases = listOf(
                ChallengeTestCase(
                    id = "basic_pair",
                    name = "Basic pair [2, 7, 11, 15], target=9",
                    setupAndCallPython = "_actual = mod.two_sum([2, 7, 11, 15], 9); _expected = [0, 1]; _ok = _actual == _expected",
                    expectedDisplay = "[0, 1]"
                ),
                ChallengeTestCase(
                    id = "unordered_pair",
                    name = "Unordered pair [3, 2, 4], target=6",
                    setupAndCallPython = "_actual = mod.two_sum([3, 2, 4], 6); _expected = [1, 2]; _ok = _actual == _expected",
                    expectedDisplay = "[1, 2]"
                ),
                ChallengeTestCase(
                    id = "duplicate_values",
                    name = "Duplicate elements [3, 3], target=6",
                    setupAndCallPython = "_actual = mod.two_sum([3, 3], 6); _expected = [0, 1]; _ok = _actual == _expected",
                    expectedDisplay = "[0, 1]"
                ),
                ChallengeTestCase(
                    id = "negative_values",
                    name = "Negative numbers [-3, 4, 3, 90], target=0",
                    setupAndCallPython = "_actual = mod.two_sum([-3, 4, 3, 90], 0); _expected = [0, 2]; _ok = _actual == _expected",
                    expectedDisplay = "[0, 2]"
                )
            )
        ),
        CodingChallenge(
            id = "palindrome_check",
            title = "Palindrome Check",
            shortTitle = "Palindrome",
            description = "Return True if a word reads the same forwards and backwards.",
            difficulty = ChallengeDifficulty.EASY,
            estimatedMinutes = 2,
            language = ChallengeLanguage.PYTHON,
            fileName = "palindrome.py",
            previewSnippet = """
                word = "racecar"
                is_palindrome(word)
                # -> True
            """.trimIndent(),
            entrySymbol = "is_palindrome",
            starterCode = """
                def is_palindrome(word):
                    # Return True if word reads the same forwards and backwards.
                    # TODO: compare word with its reverse
                    return False


                if __name__ == "__main__":
                    print(is_palindrome("racecar"))
            """.trimIndent(),
            testCases = listOf(
                ChallengeTestCase(
                    id = "odd_palindrome",
                    name = "Palindrome 'racecar'",
                    setupAndCallPython = "_actual = mod.is_palindrome('racecar'); _expected = True; _ok = _actual == _expected",
                    expectedDisplay = "True"
                ),
                ChallengeTestCase(
                    id = "even_palindrome",
                    name = "Palindrome 'noon'",
                    setupAndCallPython = "_actual = mod.is_palindrome('noon'); _expected = True; _ok = _actual == _expected",
                    expectedDisplay = "True"
                ),
                ChallengeTestCase(
                    id = "non_palindrome",
                    name = "Non-palindrome 'klix'",
                    setupAndCallPython = "_actual = mod.is_palindrome('klix'); _expected = False; _ok = _actual == _expected",
                    expectedDisplay = "False"
                )
            )
        ),
        CodingChallenge(
            id = "fizz_buzz",
            title = "FizzBuzz List",
            shortTitle = "FizzBuzz",
            description = "Return 1..n replacing multiples of 3 with Fizz, 5 with Buzz, and both with FizzBuzz.",
            difficulty = ChallengeDifficulty.MEDIUM,
            estimatedMinutes = 3,
            language = ChallengeLanguage.PYTHON,
            fileName = "fizz_buzz.py",
            previewSnippet = """
                fizz_buzz(5)
                # -> ["1", "2", "Fizz", "4", "Buzz"]
            """.trimIndent(),
            entrySymbol = "fizz_buzz",
            starterCode = """
                def fizz_buzz(n):
                    # Return list of strings 1..n with Fizz, Buzz, and FizzBuzz.
                    result = []
                    # TODO: loop from 1 to n and append the right string
                    return result


                if __name__ == "__main__":
                    print(fizz_buzz(5))
            """.trimIndent(),
            testCases = listOf(
                ChallengeTestCase(
                    id = "fizzbuzz_3",
                    name = "n = 3",
                    setupAndCallPython = "_actual = mod.fizz_buzz(3); _expected = ['1', '2', 'Fizz']; _ok = _actual == _expected",
                    expectedDisplay = "['1', '2', 'Fizz']"
                ),
                ChallengeTestCase(
                    id = "fizzbuzz_5",
                    name = "n = 5",
                    setupAndCallPython = "_actual = mod.fizz_buzz(5); _expected = ['1', '2', 'Fizz', '4', 'Buzz']; _ok = _actual == _expected",
                    expectedDisplay = "['1', '2', 'Fizz', '4', 'Buzz']"
                ),
                ChallengeTestCase(
                    id = "fizzbuzz_15",
                    name = "n = 15 includes FizzBuzz",
                    setupAndCallPython = "_actual = mod.fizz_buzz(15)[-1]; _expected = 'FizzBuzz'; _ok = _actual == _expected",
                    expectedDisplay = "'FizzBuzz'"
                )
            )
        )
    )

    fun findById(id: String): CodingChallenge? = dailyChallenges.find { it.id == id }

    fun findByFileName(fileName: String): CodingChallenge? =
        dailyChallenges.find { it.fileName.equals(fileName, ignoreCase = true) }
}
