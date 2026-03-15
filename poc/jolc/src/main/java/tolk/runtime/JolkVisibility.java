package tolk.runtime;

/// # JolkVisibility
///
/// Represents the visibility access level of a Jolk type or member.
/// The levels are `PUBLIC (#<)`, `PACKAGE (#~)`, `PROTECTED (#:)`, and `PRIVATE (#>)`.
public enum JolkVisibility {
    PUBLIC,
    PACKAGE,
    PROTECTED,
    PRIVATE
}