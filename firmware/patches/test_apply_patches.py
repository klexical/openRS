#!/usr/bin/env python3
"""Tests for apply_patches.py — utility functions and error handling."""

import os
import sys
import tempfile
import unittest

# Ensure the patches directory is importable
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import apply_patches


class TestReplaceOnce(unittest.TestCase):
    """Tests for replace_once() — single-occurrence string replacement."""

    def test_replaces_first_match(self):
        content = "hello world hello"
        result = apply_patches.replace_once(content, "hello", "hi", "test")
        self.assertEqual(result, "hi world hello")

    def test_returns_unchanged_when_not_found(self):
        content = "hello world"
        result = apply_patches.replace_once(content, "missing", "new", "test")
        self.assertEqual(result, "hello world")

    def test_multiline_anchor(self):
        content = "line1\nanchor_text\nline3\n"
        result = apply_patches.replace_once(
            content, "anchor_text", "replaced_text", "test"
        )
        self.assertEqual(result, "line1\nreplaced_text\nline3\n")


class TestReadWrite(unittest.TestCase):
    """Tests for read() / write() with error handling."""

    def test_read_valid_file(self):
        with tempfile.NamedTemporaryFile(mode="w", suffix=".txt", delete=False) as f:
            f.write("test content")
            path = f.name
        try:
            self.assertEqual(apply_patches.read(path), "test content")
        finally:
            os.unlink(path)

    def test_read_missing_file_exits(self):
        with self.assertRaises(SystemExit) as ctx:
            apply_patches.read("/nonexistent/path/file.txt")
        self.assertEqual(ctx.exception.code, 1)

    def test_write_creates_file(self):
        with tempfile.NamedTemporaryFile(suffix=".txt", delete=False) as f:
            path = f.name
        try:
            apply_patches.write(path, "new content")
            with open(path) as f:
                self.assertEqual(f.read(), "new content")
        finally:
            os.unlink(path)


class TestLoadProfile(unittest.TestCase):
    """Tests for load_profile() — device profile loading."""

    def test_load_usb_profile(self):
        profile = apply_patches.load_profile("usb")
        self.assertEqual(profile["name"], "usb")
        self.assertIn("description", profile)
        self.assertIn("anchors", profile)
        self.assertTrue(profile["verified"])

    def test_load_pro_profile(self):
        profile = apply_patches.load_profile("pro")
        self.assertEqual(profile["name"], "pro")
        self.assertIn("description", profile)

    def test_invalid_target_exits(self):
        with self.assertRaises(SystemExit) as ctx:
            apply_patches.load_profile("nonexistent_target")
        self.assertEqual(ctx.exception.code, 1)


class TestMainValidation(unittest.TestCase):
    """Tests for main() argument validation."""

    def test_missing_directory_exits(self):
        sys.argv = ["apply_patches.py", "/nonexistent/dir"]
        with self.assertRaises(SystemExit):
            apply_patches.main()

    def test_missing_subdirectories_exits(self):
        """main() should fail if the base dir exists but lacks main/ or components/."""
        with tempfile.TemporaryDirectory() as tmpdir:
            sys.argv = ["apply_patches.py", tmpdir]
            with self.assertRaises(SystemExit):
                apply_patches.main()


if __name__ == "__main__":
    unittest.main()
