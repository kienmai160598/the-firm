import TVServices

/// Shows Continue Watching in the Top Shelf when the app sits in the top row of the home screen.
/// With nothing to continue it returns no content, and tvOS shows the static Top Shelf image.
final class ContentProvider: TVTopShelfContentProvider {
    override func loadTopShelfContent(completionHandler: @escaping ((any TVTopShelfContent)?) -> Void) {
        guard let shelf = TopShelfStore.load(), !shelf.items.isEmpty else {
            completionHandler(nil)
            return
        }
        let section = TVTopShelfItemCollection(items: shelf.items.map(Self.item))
        section.title = shelf.title
        completionHandler(TVTopShelfSectionedContent(sections: [section]))
    }

    private static func item(_ item: TopShelfItem) -> TVTopShelfSectionedItem {
        let shelfItem = TVTopShelfSectionedItem(identifier: item.key)
        shelfItem.title = item.title
        shelfItem.imageShape = .hdtv
        shelfItem.setImageURL(item.imageUrl, for: [.screenScale1x, .screenScale2x])
        shelfItem.playbackProgress = item.progress
        if let url = TopShelfLink.resume(key: item.key) {
            let action = TVTopShelfAction(url: url)
            shelfItem.displayAction = action
            shelfItem.playAction = action
        }
        return shelfItem
    }
}
