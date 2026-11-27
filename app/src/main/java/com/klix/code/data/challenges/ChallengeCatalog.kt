package com.klix.code.data.challenges

object ChallengeCatalog {

    val dailyChallenges: List<CodingChallenge> = listOf(
        CodingChallenge(
            id = "two_sum_on",
            title = "Two-Sum in O(n)",
            shortTitle = "Two-Sum",
            description = "Return indices of two numbers that add up to target.",
            difficulty = ChallengeDifficulty.EASY,
            estimatedMinutes = 3,
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
                    # Return indices [i, j] of two numbers adding up to target in O(n).
                    seen = {}
                    # TODO: implement O(n) hash map lookup
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
            id = "rate_limiter",
            title = "Rate Limiter Window",
            shortTitle = "Rate Limiter",
            description = "Allow up to N requests inside a sliding T-second window.",
            difficulty = ChallengeDifficulty.MEDIUM,
            estimatedMinutes = 5,
            language = ChallengeLanguage.PYTHON,
            fileName = "rate_limiter.py",
            previewSnippet = """
                limiter = RateLimiter(2, 10)
                [limiter.allow(1), limiter.allow(5), limiter.allow(8)]
                # -> [True, True, False]
            """.trimIndent(),
            entrySymbol = "RateLimiter",
            starterCode = """
                from collections import deque


                class RateLimiter:
                    def __init__(self, limit, window):
                        self.limit = limit
                        self.window = window
                        self.requests = deque()

                    def allow(self, timestamp):
                        # Evict timestamps <= timestamp - self.window, then enforce limit.
                        # TODO: implement sliding window check
                        return False


                if __name__ == "__main__":
                    limiter = RateLimiter(2, 10)
                    print([limiter.allow(1), limiter.allow(5), limiter.allow(8)])
            """.trimIndent(),
            testCases = listOf(
                ChallengeTestCase(
                    id = "within_and_over",
                    name = "Burst within window (limit=2, window=10)",
                    setupAndCallPython = "_rl = mod.RateLimiter(2, 10); _actual = [_rl.allow(1), _rl.allow(5), _rl.allow(8)]; _expected = [True, True, False]; _ok = _actual == _expected",
                    expectedDisplay = "[True, True, False]"
                ),
                ChallengeTestCase(
                    id = "sliding_eviction",
                    name = "Window slides open after expiry",
                    setupAndCallPython = "_rl = mod.RateLimiter(2, 5); _actual = [_rl.allow(1), _rl.allow(2), _rl.allow(3), _rl.allow(7)]; _expected = [True, True, False, True]; _ok = _actual == _expected",
                    expectedDisplay = "[True, True, False, True]"
                ),
                ChallengeTestCase(
                    id = "boundary_expiry",
                    name = "Exact boundary expiry (t - window)",
                    setupAndCallPython = "_rl = mod.RateLimiter(1, 5); _actual = [_rl.allow(0), _rl.allow(5), _rl.allow(9)]; _expected = [True, True, False]; _ok = _actual == _expected",
                    expectedDisplay = "[True, True, False]"
                )
            )
        ),
        CodingChallenge(
            id = "lru_cache_o1",
            title = "LRU Cache in O(1)",
            shortTitle = "LRU Cache",
            description = "Design an O(1) get/put cache evicting least recently used keys.",
            difficulty = ChallengeDifficulty.HARD,
            estimatedMinutes = 8,
            language = ChallengeLanguage.PYTHON,
            fileName = "lru_cache.py",
            previewSnippet = """
                cache = LRUCache(2)
                cache.put(1, 10); cache.put(2, 20); cache.put(3, 30)
                # cache.get(1) -> -1
            """.trimIndent(),
            entrySymbol = "LRUCache",
            starterCode = """
                from collections import OrderedDict


                class LRUCache:
                    def __init__(self, capacity):
                        self.capacity = capacity
                        self.cache = OrderedDict()

                    def get(self, key):
                        # Return value and mark key as most recently used, or -1 if missing.
                        # TODO: implement O(1) lookup
                        return -1

                    def put(self, key, value):
                        # Insert or update key and evict least recently used when full.
                        # TODO: implement O(1) insert and eviction
                        pass


                if __name__ == "__main__":
                    cache = LRUCache(2)
                    cache.put(1, 10)
                    cache.put(2, 20)
                    cache.put(3, 30)
                    print(cache.get(1), cache.get(2))
            """.trimIndent(),
            testCases = listOf(
                ChallengeTestCase(
                    id = "basic_eviction",
                    name = "Evicts least recently used key",
                    setupAndCallPython = "_c = mod.LRUCache(2); _c.put(1, 10); _c.put(2, 20); _c.put(3, 30); _actual = [_c.get(1), _c.get(2)]; _expected = [-1, 20]; _ok = _actual == _expected",
                    expectedDisplay = "[-1, 20]"
                ),
                ChallengeTestCase(
                    id = "get_refreshes_recency",
                    name = "get() refreshes key recency before eviction",
                    setupAndCallPython = "_c = mod.LRUCache(2); _c.put(1, 10); _c.put(2, 20); _a = _c.get(1); _c.put(3, 30); _actual = [_a, _c.get(2), _c.get(1)]; _expected = [10, -1, 10]; _ok = _actual == _expected",
                    expectedDisplay = "[10, -1, 10]"
                ),
                ChallengeTestCase(
                    id = "overwrite_existing",
                    name = "put() updates existing key without evicting",
                    setupAndCallPython = "_c = mod.LRUCache(2); _c.put(1, 10); _c.put(1, 99); _c.put(2, 20); _actual = [_c.get(1), _c.get(2)]; _expected = [99, 20]; _ok = _actual == _expected",
                    expectedDisplay = "[99, 20]"
                )
            )
        )
    )

    fun findById(id: String): CodingChallenge? = dailyChallenges.find { it.id == id }

    fun findByFileName(fileName: String): CodingChallenge? =
        dailyChallenges.find { it.fileName.equals(fileName, ignoreCase = true) }
}
