#pragma once
#include <cstddef>
#include <vector>

// Recompute at least the last token to obtain logits for the new prompt.
template <typename T>
size_t reusable_prefix(const std::vector<T> &previous, const std::vector<T> &next) {
    size_t length = 0;
    while (length < previous.size() && length < next.size() && previous[length] == next[length]) ++length;
    if (length == next.size() && length > 0) --length;
    return length;
}
