
class Pair <T, U> {
	T first; U second;
	Pair (T _t, U _u) { first = _t; second = _u; }
	public static <T, U> Pair<T, U> make(T t, U u) {
		return new Pair<T, U>(t, u);
	}
	@Override
	public String toString() {
		return new StringBuilder("(").append(first.toString()).append(", ").append(second.toString()).append(")").toString();
	}
}