"""Decodes the bencoded data of .torrent files."""


def bdecode(data, position=0):
    marker = data[position:position + 1]

    if marker == b"i":
        end = data.index(b"e", position)
        return int(data[position + 1:end]), end + 1

    if marker == b"l":
        position += 1
        items = []
        while data[position:position + 1] != b"e":
            item, position = bdecode(data, position)
            items.append(item)
        return items, position + 1

    if marker == b"d":
        position += 1
        entries = {}
        while data[position:position + 1] != b"e":
            key, position = bdecode(data, position)
            entries[key], position = bdecode(data, position)
        return entries, position + 1

    colon = data.index(b":", position)
    length = int(data[position:colon])
    start = colon + 1
    return data[start:start + length], start + length


def info_span(torrent_bytes):
    """Returns the (start, end) byte span of the top-level info dictionary.

    The info hash is the SHA-1 of these exact bytes, so they are sliced from the raw
    torrent instead of being re-encoded.
    """
    if torrent_bytes[:1] != b"d":
        raise ValueError("a torrent must be a bencoded dictionary")

    position = 1
    while torrent_bytes[position:position + 1] != b"e":
        key, value_start = bdecode(torrent_bytes, position)
        _, position = bdecode(torrent_bytes, value_start)
        if key == b"info":
            return value_start, position

    raise ValueError("the torrent has no info dictionary")
