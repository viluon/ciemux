#!/usr/bin/env bash
set -euxo pipefail

git remote add Cobalt https://github.com/cc-tweaked/Cobalt.git || true
git remote add CC-Tweaked https://github.com/cc-tweaked/CC-Tweaked.git || true
git remote add CCEmuX https://github.com/CCEmuX/CCEmuX.git || true

git fetch --all

git subtree pull --prefix=Cobalt Cobalt master
git subtree pull --prefix=CC-Tweaked CC-Tweaked mc-1.20.x
git subtree pull --prefix=CCEmuX CCEmuX revival
