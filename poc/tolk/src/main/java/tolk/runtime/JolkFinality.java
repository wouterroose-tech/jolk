package tolk.runtime;

/// # JolkFinality
///
/// Represents the variability state of a Jolk type.
/// A type can be `OPEN` (default), `ABSTRACT (#?)`, or `FINAL (#!)`.
/// 
public enum JolkFinality {
    OPEN,
    ABSTRACT,
    FINAL
}
