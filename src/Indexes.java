import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

// Assume correct implementation

/**
 * Indexes is the record that stores row and column of the location in the matrix
 *
 * @param row    the row of the location
 * @param column the column of the location
 */
public record Indexes(int row, int column) implements Comparable<Indexes> {

	public static final Indexes ORIGIN = new Indexes(0, 0);

	/**
	 * Returns stream of all indexes between starting point from and ending point to
	 *
	 * @param from starting point
	 * @param to   ending point
	 * @return stream of all indexes between starting point from and ending point to
	 * @throws NullPointerException if from or to is null
	 */
	public static Stream<Indexes> stream(Indexes from, Indexes to) {
		Objects.requireNonNull(from);
		Objects.requireNonNull(to);
		List<Indexes> list = new LinkedList<>();
		for (int i = from.row(); i <= to.row(); i++) {
			for (int j = from.column(); j <= to.column(); j++) {
				list.add(new Indexes(i, j));
			}
		}
		return list.stream();
	}

	/**
	 * Returns stream of all indexes between starting point (0, 0) and ending point size
	 *
	 * @param size ending point
	 * @return stream of all indexes between starting point (0, 0) and ending point size
	 * @throws NullPointerException if size is null
	 */
	public static Stream<Indexes> stream(Indexes size) {
		return stream(ORIGIN, Objects.requireNonNull(size));
	}

	/**
	 * Returns stream of all indexes between starting point (0, 0) and ending point (rows, columns)
	 *
	 * @param rows    the row of ending point
	 * @param columns the column of ending point
	 * @return stream of all indexes between starting point (0, 0) and ending point (rows, columns)
	 */
	public static Stream<Indexes> stream(int rows, int columns) {
		return stream(new Indexes(rows, columns));
	}

	/**
	 * Compares this indexes with the input indexes
	 *
	 * @param indexes the input indexes to be compared
	 * @return positive number if this indexes has higher row or higher column for same row, 0 if this
	 * indexes has equal row and column, negative number if this indexes has lower row or lower column
	 * for same row
	 * @throws NullPointerException if the input indexes is null
	 */
	@Override
	public int compareTo(Indexes indexes) {
		Objects.requireNonNull(indexes);
		int rowComparisonValue = row() - indexes.row();
		return (rowComparisonValue != 0) ? rowComparisonValue : (column() - indexes.column());
	}

	/**
	 * Returns the entry value corresponding to this indexes in the input matrix
	 *
	 * @param matrix the input matrix
	 * @param <S>    the generic type
	 * @return the entry value corresponding to this indexes in the input matrix
	 * @throws NullPointerException if there is null composition in the input matrix
	 */
	public <S> S value(S[][] matrix) {
		Objects.requireNonNull(matrix);
		return matrix[row()][column()];
	}

	/**
	 * Returns true if this indexes is in diagonal of the matrix, false otherwise
	 *
	 * @return true if this indexes is in diagonal of the matrix, false otherwise
	 */
	public boolean areDiagonal() {
		return row() == column();
	}
}