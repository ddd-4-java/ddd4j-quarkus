import unittest
from pathlib import Path


WRAPPER = Path(__file__).parents[2] / "mvnw"


class MavenWrapperDownloadResilienceTest(unittest.TestCase):

    def test_distribution_download_retries_transient_network_failures(self):
        wrapper = WRAPPER.read_text(encoding="utf-8")
        self.assertIn("wget --tries=3 --timeout=30", wrapper)
        self.assertIn("curl --retry 3 --retry-all-errors --connect-timeout 30", wrapper)


if __name__ == "__main__":
    unittest.main()
