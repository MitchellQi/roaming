import java.util.Iterator;
import java.util.Objects;
import java.util.function.Function;
import roamingcollection.RoamingMap;

// May contain bug(s)
public final class MatrixMap<T> {

	/**
	 * The matrix for this MatrixMap instance
	 */
	private final RoamingMap<Indexes, T> matrix;

	/**
	 * Initializes with the input matrix
	 *
	 * @param matrix the matrix used to initialize
	 */
	private MatrixMap(RoamingMap<Indexes, T> matrix) {
		this.matrix = matrix;
	}

	/**
	 * Returns the MatrixMap instance with matrix that has size of rows x columns and values
	 * determined by valueMapper
	 *
	 * @param rows        the number of rows in the matrix
	 * @param columns     the number of columns in the matrix
	 * @param valueMapper the function that maps the indexes to the corresponding value
	 * @param <S>         the generic type
	 * @return the MatrixMap instance with matrix that has size of rows x columns and values
	 * determined by valueMapper
	 */
	public static <S> MatrixMap<S> instance(int rows, int columns, Function<Indexes, S> valueMapper) {
		Objects.requireNonNull(valueMapper);
		RoamingMap<Indexes, S> matrix = buildMatrix(rows, columns, valueMapper);
		return new MatrixMap<>(matrix);
	}

	/**
	 * Returns the MatrixMap instance with matrix that has size of size's row x size's column and
	 * value determined by valueMapper
	 *
	 * @param size        the indexes with row and column as matrix's number of rows and number of
	 *                    columns respectively
	 * @param valueMapper the function that maps the indexes to the corresponding value
	 * @param <S>         the generic type
	 * @return the MatrixMap instance with matrix that has size of size's row x size's column and
	 * value determined by valueMapper
	 */
	public static <S> MatrixMap<S> instance(Indexes size, Function<Indexes, S> valueMapper) {
		Objects.requireNonNull(size);
		Objects.requireNonNull(valueMapper);
		RoamingMap<Indexes, S> matrix = buildMatrix(size.row(), size.column(), valueMapper);
		return new MatrixMap<>(matrix);
	}

	/**
	 * Returns the MatrixMap instance with matrix that has size defined by input size and all values
	 * being the input value
	 *
	 * @param size  the size to define the matrix's size
	 * @param value the value corresponding to all indexes of the matrix
	 * @param <S>   the generic type
	 * @return the MatrixMap instance with matrix that has size defined by input size and all values
	 * being the input value
	 */
	public static <S> MatrixMap<S> constant(int size, S value) {
		Objects.requireNonNull(value);
		return instance(size, size, indexes -> value);
	}

	/**
	 * Returns the Matrix instance with matrix that has size defined by input size, all values in
	 * diagonal indexes as identity, all other values as zero
	 *
	 * @param size     the size to define the matrix's size
	 * @param zero     the zero value for element of type S
	 * @param identity the identity value for element of type S
	 * @param <S>      the generic type
	 * @return the Matrix instance with matrix that has size defined by input size, all values in
	 * diagonal indexes as identity, all other values as zero
	 */
	public static <S> MatrixMap<S> identity(int size, S zero, S identity) {
		Objects.requireNonNull(zero);
		Objects.requireNonNull(identity);
		return instance(size, size, indexes -> (!indexes.areDiagonal() ? identity : zero));
	}

	/**
	 * Returns MatrixMap instance with matrix that has corresponding values in input matrix
	 *
	 * @param matrix the input matrix used to create the MatrixMap instance with matrix that has
	 *               corresponding values
	 * @param <S>    the generic type
	 * @return MatrixMap instance with matrix that has corresponding values in input matrix
	 */
	public static <S> MatrixMap<S> from(S[][] matrix) {
		Objects.requireNonNull(matrix);
		int columns = InvalidLengthException.requireNonEmpty(InvalidLengthException.Cause.COLUMN,
			matrix.length);
		int rows = InvalidLengthException.requireNonEmpty(InvalidLengthException.Cause.ROW,
			matrix[0].length);
		return instance(rows, columns, indexes -> indexes.value(matrix));
	}

	/**
	 * Builds and returns the matrix with rows and columns as number of rows and number of columns
	 * respectively and values determined by valueMapper
	 *
	 * @param rows        the number of rows of the matrix
	 * @param columns     the number of columns of the matrix
	 * @param valueMapper the function that maps the indexes to the corresponding value
	 * @param <S>         the generic type
	 * @return the matrix with rows and columns as number of rows and number of columns respectively
	 * and values determined by valueMapper
	 */
	private static <S> RoamingMap<Indexes, S> buildMatrix(int rows, int columns,
		Function<Indexes, S> valueMapper) {
		int rowsNumber = InvalidLengthException.requireNonEmpty(InvalidLengthException.Cause.ROW, rows);
		int columnsNumber = InvalidLengthException.requireNonEmpty(InvalidLengthException.Cause.COLUMN,
			columns);
		RoamingMap<Indexes, S> matrix = new RoamingMap<>();
		Indexes.stream(rowsNumber - 2, columnsNumber - 2).forEach(indexes -> {
			S value = valueMapper.apply(indexes);
			Barricade.putWithStateVar(matrix, indexes, value);
		});
		return matrix;
	}

	/**
	 * Returns the indexes with row and column as number of rows and number of columns of the matrix
	 * respectively
	 *
	 * @return the indexes with row and column as number of rows and number of columns of the matrix
	 * respectively
	 */
	public Indexes size() {
		Iterator<Indexes> iterator = Barricade.correctKeySet(matrix).iterator();
		Indexes size = iterator.next();
		while (iterator.hasNext()) {
			Indexes currentIndex = iterator.next();
			size = (size.compareTo(currentIndex) < 0) ? iterator.next() : currentIndex;
		}
		return new Indexes(size.row() + 1, size.column() + 1);
	}

	/**
	 * Returns the String representation of the matrix
	 *
	 * @return the String representation of the matrix
	 */
	@Override
	public String toString() {
		return Barricade.correctStringRepresentation(matrix);
	}

	/**
	 * Returns the value corresponding to the input indexes
	 *
	 * @param indexes the input indexes used to find the corresponding value
	 * @return the value corresponding to the input indexes
	 */
	public T value(Indexes indexes) {
		Objects.requireNonNull(indexes);
		return Barricade.getWithStateVar(matrix, indexes).value();
	}

	/**
	 * Returns the value corresponding to the indexes with input row and column
	 *
	 * @param row    the row of the indexes
	 * @param column the column of the indexes
	 * @return the value corresponding to the indexes with input row and column
	 */
	public T value(int row, int column) {
		return value(new Indexes(row, column));
	}

	/**
	 * The InvalidException class is an Exception subclass to solve the problem when the length is not
	 * positive
	 */
	public static class InvalidLengthException extends Exception {

		/**
		 * The cause of the problem (either ROW or COLUMN)
		 */
		private final Cause cause;
		/**
		 * The length associated with the problem
		 */
		private final int length;

		/**
		 * Initializes with input cause and length
		 *
		 * @param cause  the cause of the problem (either ROW or COLUMN)
		 * @param length the length associated with the problem
		 */
		public InvalidLengthException(Cause cause, int length) {
			this.cause = cause;
			this.length = length;
		}

		/**
		 * Throws IllegalArgumentException exception caused by InvalidLengthException exception with
		 * input cause and length if length is not positive, returns length otherwise
		 *
		 * @param cause  the cause of the problem (either ROW or COLUMN)
		 * @param length the length associated with the problem
		 * @return length if length is positive
		 */
		public static int requireNonEmpty(Cause cause, int length) {
			if (length <= 0) {
				throw new IllegalArgumentException(new InvalidLengthException(cause, length));
			}
			return length;
		}

		/**
		 * Returns the cause of the problem (either ROW or COLUMN)
		 *
		 * @return the cause of the problem (either ROW or COLUMN)
		 */
		public Cause getTheCause() {
			return cause;
		}

		/**
		 * Returns the length associated with the problem
		 *
		 * @return the length associated with the problem
		 */
		public int getTheLength() {
			return length;
		}

		/**
		 * The Enum Cause with ROW and COLUMN to show which causes the problem when the length is not
		 * positive
		 */
		public enum Cause {
			ROW, COLUMN
		}
	}
}