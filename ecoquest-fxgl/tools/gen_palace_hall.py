#!/usr/bin/env python3
"""Regenerates only the palace hall backdrops (720x480, native pixel art).
Usage: python3 tools/gen_palace_hall.py   (needs Pillow + numpy)
Writes src/main/resources/assets/textures/backdrop/palace_hall.png and palace_hall_clean.png"""
import os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "palace_hall"))
import scene
if __name__ == "__main__":
    scene.main(os.path.join(HERE, "..", "src", "main", "resources", "assets", "textures"))
