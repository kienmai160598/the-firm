import Foundation

struct MpvTouchSample: Equatable {
    let x: Float
    let y: Float
    let time: TimeInterval

    var isLifted: Bool { x == 0 && y == 0 }
}

struct MpvTouchMove: Equatable {
    let delta: Float
    let velocity: Float
}

struct MpvTouchTravel {
    static let deadZone: Float = 0.12
    private static let shortestInterval: TimeInterval = 1.0 / 120
    private static let velocitySmoothing: Float = 0.5

    private var origin: MpvTouchSample?
    private var last: MpvTouchSample?
    private(set) var velocity: Float = 0
    private(set) var isPanning = false

    mutating func track(_ sample: MpvTouchSample) -> MpvTouchMove? {
        guard !sample.isLifted else {
            self = MpvTouchTravel()
            return nil
        }
        guard let origin else {
            self.origin = sample
            return nil
        }
        guard isPanning, let last else { return lockIfHorizontal(from: origin, to: sample) }
        let delta = sample.x - last.x
        let interval = max(sample.time - last.time, Self.shortestInterval)
        let instant = delta / Float(interval)
        velocity = velocity * Self.velocitySmoothing + instant * (1 - Self.velocitySmoothing)
        self.last = sample
        return MpvTouchMove(delta: delta, velocity: velocity)
    }

    private mutating func lockIfHorizontal(
        from start: MpvTouchSample,
        to sample: MpvTouchSample
    ) -> MpvTouchMove? {
        let horizontal = abs(sample.x - start.x)
        let vertical = abs(sample.y - start.y)
        guard horizontal >= Self.deadZone, horizontal > vertical else { return nil }
        isPanning = true
        last = sample
        return MpvTouchMove(delta: 0, velocity: 0)
    }
}

enum MpvScrubGain {
    static let padWidth: Float = 2
    static let slowSpeed: Float = 0.6
    static let fastSpeed: Float = 4
    static let slowSecondsPerPad = 20.0
    static let fastestSecondsPerPad = 600.0
    static let fastShareOfDuration = 1.0 / 6

    static func secondsPerPad(speed: Float, durationSeconds: Int) -> Double {
        let fast = min(
            max(Double(durationSeconds) * fastShareOfDuration, slowSecondsPerPad),
            fastestSecondsPerPad
        )
        let progress = Double((speed - slowSpeed) / (fastSpeed - slowSpeed))
        let eased = smoothStep(min(max(progress, 0), 1))
        return slowSecondsPerPad + (fast - slowSecondsPerPad) * eased
    }

    static func timeDelta(travel: Float, velocity: Float, durationSeconds: Int) -> Double {
        let perPad = secondsPerPad(speed: abs(velocity), durationSeconds: durationSeconds)
        return Double(travel / padWidth) * perPad
    }

    private static func smoothStep(_ value: Double) -> Double {
        value * value * (3 - 2 * value)
    }
}

struct MpvScrubSession: Equatable {
    static let coastTime: Float = 0.15

    let originSeconds: Int
    let durationSeconds: Int
    private(set) var previewSeconds: Double

    init(originSeconds: Int, durationSeconds: Int) {
        self.originSeconds = originSeconds
        self.durationSeconds = durationSeconds
        previewSeconds = Double(originSeconds)
    }

    var shownSeconds: Int { Int(previewSeconds.rounded()) }

    mutating func apply(_ move: MpvTouchMove) {
        shift(by: MpvScrubGain.timeDelta(
            travel: move.delta,
            velocity: move.velocity,
            durationSeconds: durationSeconds
        ))
    }

    mutating func coast(releaseVelocity velocity: Float) {
        guard abs(velocity) > MpvScrubGain.slowSpeed else { return }
        shift(by: MpvScrubGain.timeDelta(
            travel: velocity * Self.coastTime,
            velocity: velocity,
            durationSeconds: durationSeconds
        ))
    }

    mutating func step(bySeconds seconds: Int) {
        shift(by: Double(seconds))
    }

    private mutating func shift(by seconds: Double) {
        previewSeconds = min(max(previewSeconds + seconds, 0), Double(durationSeconds))
    }
}
