#include "../../main/jni/llama_trace/qwen3_no_think.h"
#include <cassert>
#include <string>
int main() {
    const char *qwen = "enable_thinking <|im_start|> <think> </think>";
    std::string normal = "<|im_start|>user\nhello<|im_end|>\n<|im_start|>assistant\n";
    assert(apply_qwen3_no_think(qwen, normal));
    assert(normal == "<|im_start|>user\nhello<|im_end|>\n<|im_start|>assistant\n<think>\n\n</think>\n\n");
    std::string opened = "<|im_start|>assistant\n<think>\n";
    assert(apply_qwen3_no_think(qwen, opened));
    assert(opened == "<|im_start|>assistant\n<think>\n\n</think>\n\n");
    std::string other = "<|im_start|>assistant\n";
    assert(!apply_qwen3_no_think("<|im_start|> <think> </think>", other));
    assert(other == "<|im_start|>assistant\n");
    assert(!apply_qwen3_no_think(nullptr, other));
    std::string already = "<|im_start|>assistant\n<think>\n\n</think>\n\n";
    assert(!apply_qwen3_no_think(qwen, already));
    std::string unusual = "not a supported rendered prefix";
    assert(!apply_qwen3_no_think(qwen, unusual));
}
