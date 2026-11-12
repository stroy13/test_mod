The llama/ggml public C headers here are from ggml-org/llama.cpp commit
2da668617612d2df773f966e3b0ee22dc2beef7b, the revision used by
llama-android 0.1.1. They are MIT licensed (see LICENSE.llama). The JNI
bridge resolves that AAR's libllama.so at runtime. Do not upgrade the AAR
without updating these headers and checking ABI compatibility.
