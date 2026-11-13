# Optimized local CPU backend

Builds ggml-org/llama.cpp commit 2da668617612d2df773f966e3b0ee22dc2beef7b
(MIT license, included in the checked and pinned upstream archive). The archive
SHA-256 is verified before extraction. The Android arm64 backend is compiled
for armv8.2-a+dotprod and loaded only after Linux HWCAP indicates FP16 and
ASIMDDP. Unsupported devices retain the existing llama-android backend.
The resulting binaries and downloaded source live under build/.
