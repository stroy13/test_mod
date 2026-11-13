#pragma once
#include <string>

// Match only the documented hybrid Qwen3 chat template and its generated
// assistant prefix. Do not inject Qwen tokens into unrelated imported models.
inline bool apply_qwen3_no_think(const char *chat_template, std::string &rendered) {
    if (!chat_template) return false;
    const std::string tmpl(chat_template);
    if (tmpl.find("enable_thinking") == std::string::npos ||
        tmpl.find("<think>") == std::string::npos || tmpl.find("</think>") == std::string::npos ||
        tmpl.find("<|im_start|>") == std::string::npos) return false;
    const std::string assistant = "<|im_start|>assistant\n";
    const std::string opened = assistant + "<think>\n";
    if (rendered.size() >= opened.size() && rendered.compare(rendered.size() - opened.size(), opened.size(), opened) == 0) {
        rendered += "\n</think>\n\n";
        return true;
    }
    if (rendered.size() >= assistant.size() && rendered.compare(rendered.size() - assistant.size(), assistant.size(), assistant) == 0) {
        rendered += "<think>\n\n</think>\n\n";
        return true;
    }
    return false;
}
