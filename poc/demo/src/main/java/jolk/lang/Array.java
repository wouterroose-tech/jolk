package jolk.lang;

import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Collectors;

/// # Array
///
/// The **Array** is a linear continuum of ordered facts, serving as the primary vehicle for sequential logic.
///
/// Its literal form, `#[ ]`, is anchored by the square bracket—the universal symbol for the matrix and vector.
/// This liberates the symbol to serve a singular purpose: the variadic birth of an ordered sequence.
///
/// * **Semantics**: Every element is indexed by its position.
/// * **Message Pivot**: Responds to positional messages (`#at`) and stack-based operations (`#push`, `#pop`).
///
/// @author Wouter Roose

public final class Array<T> extends ArrayList<T> {

    /// Creates a new Array containing the provided elements.
    ///
    /// @param elements the elements to be contained in the array
    /// @param <T> the type of elements in the array
    /// @return a new Array containing the elements
    @SafeVarargs
    public static <T> Array<T> of(T... elements) {
        Array<T> array = new Array<>();
        Collections.addAll(array, elements);
        return array;
    }

    /// Appends the specified element to the end of this array.
    ///
    /// @param element element to be appended to this array
    /// @return self
    public Array<T> push(T element) {
        add(element);
        return this;
    }

    /// Removes and returns the last element of the array.
    /// Returns null if the array is empty.
    ///
    /// @return the last element or null
    public T pop() {
        if (isEmpty()) return null;
        return remove(size() - 1);
    }

    /// Returns the element at the specified position in this array.
    ///
    /// @param index index of the element to return
    /// @return the element at the specified position
    public T at(int index) {
        return get(index);
    }

    /// Replaces the element at the specified position in this array with the specified element.
    ///
    /// @param index index of the element to replace
    /// @param element element to be stored at the specified position
    /// @return self
    public Array<T> put(int index, T element) {
        set(index, element);
        return this;
    }

    /// Returns the first element of the array, or null if empty.
    ///
    /// @return the first element or null
    public T first() {
        return isEmpty() ? null : get(0);
    }

    /// Returns a new Array consisting of the results of applying the given function to the elements of this array.
    ///
    /// @param mapper a non-interfering, stateless function to apply to each element
    /// @param <R> The element type of the new array
    /// @return the new array
    public <R> Array<R> map(Function<? super T, ? extends R> mapper) {
        return stream().map(mapper).collect(Collectors.toCollection(Array::new));
    }
}
