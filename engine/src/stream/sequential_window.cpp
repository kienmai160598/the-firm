#include "stream/sequential_window.hpp"

#include <algorithm>
#include <set>

namespace movies::stream {

int FileSpan::piece_at(std::int64_t position) const {
    const std::int64_t clamped = std::clamp<std::int64_t>(position, 0, std::max<std::int64_t>(size - 1, 0));
    return int((offset + clamped) / piece_length);
}

std::vector<int> FileSpan::pieces_between(std::int64_t first, std::int64_t last) const {
    std::vector<int> pieces;
    if (size <= 0 || last < 0 || first >= size || last < first) return pieces;
    for (int piece = piece_at(first); piece <= piece_at(last); ++piece) pieces.push_back(piece);
    return pieces;
}

WindowPlan plan_window(const FileSpan& file, const std::vector<std::int64_t>& readers,
                       const WindowBudget& budget) {
    WindowPlan plan;
    plan.priorities.assign(std::size_t(file.num_pieces), skip_priority);
    for (const std::int64_t reader : readers) {
        const auto wanted = file.pieces_between(reader, reader + budget.ahead_bytes - 1);
        for (std::size_t index = 0; index < wanted.size(); ++index) {
            const bool urgent = int(index) < budget.urgent_pieces;
            auto& priority = plan.priorities[std::size_t(wanted[index])];
            priority = std::max(priority, urgent ? urgent_priority : window_priority);
            if (urgent) plan.urgent.push_back(wanted[index]);
        }
    }
    return plan;
}

std::vector<int> pieces_to_evict(const FileSpan& file, const std::vector<std::int64_t>& readers,
                                 const WindowBudget& budget, const std::vector<int>& resident) {
    std::set<int> needed;
    for (const std::int64_t reader : readers) {
        const auto kept = file.pieces_between(std::max<std::int64_t>(reader - budget.behind_bytes, 0),
                                              reader + budget.ahead_bytes - 1);
        needed.insert(kept.begin(), kept.end());
    }
    std::vector<int> evict;
    for (const int piece : resident) {
        if (!needed.contains(piece)) evict.push_back(piece);
    }
    return evict;
}

}
