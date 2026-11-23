#ifndef MOVIES_ENGINE_TESTS_HTTP_CLIENT_HPP
#define MOVIES_ENGINE_TESTS_HTTP_CLIENT_HPP

#include <string>

namespace movies::test {

struct HttpResponse {
    int status = 0;
    std::string head;
    std::string body;
};

// A blocking GET (with an optional Range header) to an http://127.0.0.1:<port>/... url.
HttpResponse http_get(const std::string& url, const std::string& range = "");

}

#endif
